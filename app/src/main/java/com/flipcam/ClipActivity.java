package com.flipcam;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.transformer.Composition;
import androidx.media3.transformer.EditedMediaItem;
import androidx.media3.transformer.ExportException;
import androidx.media3.transformer.ExportResult;
import androidx.media3.transformer.ProgressHolder;
import androidx.media3.transformer.Transformer;
import androidx.media3.ui.PlayerView;
import com.flipcam.clip.ClipProgressView;
import com.flipcam.clip.ClipRangeView;
import com.flipcam.clip.LatestRecordingStore;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Edits only the persisted latest original. No arbitrary file input or delete action. */
@androidx.annotation.OptIn(markerClass = androidx.media3.common.util.UnstableApi.class)
public final class ClipActivity extends AppCompatActivity {
    private static final String TAG = "FlipCamClip";
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ExecutorService thumbnails = Executors.newSingleThreadExecutor();
    private ExoPlayer player;
    private Transformer transformer;
    private File source, savedClip, pending, output;
    private long duration, start, end;
    private int exportGeneration;
    private boolean destroyed, savedScreen;
    private ClipRangeView range;
    private ClipProgressView progress;
    private TextView title, playbackTime;
    private final Runnable rangePreview = () -> preparePreview(false);
    private final Runnable playbackTick = new Runnable() {
        @Override public void run() {
            if (destroyed) return;
            if (player != null) playbackTime.setText(time(player.getCurrentPosition()) + " / " + time(end-start));
            handler.postDelayed(this, 150);
        }
    };
    private final Runnable exportTick = new Runnable() {
        @Override public void run() {
            if (transformer == null || destroyed) return;
            ProgressHolder holder = new ProgressHolder();
            int state = transformer.getProgress(holder);
            progress.setProgress(state == Transformer.PROGRESS_STATE_AVAILABLE ? holder.progress : -1);
            handler.postDelayed(this, 100);
        }
    };

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_clip);
        source = LatestRecordingStore.latest(this);
        if (source == null) { unavailable(); return; }
        // Only remove our own unfinished export after a process interruption.
        String abandoned = getSharedPreferences("FlipCamClipExport", MODE_PRIVATE).getString("pending", "");
        if (!abandoned.isEmpty()) {
            File file = new File(abandoned);
            if (file.getName().startsWith(".clip_") && file.getName().endsWith(".mp4")) file.delete();
            getSharedPreferences("FlipCamClipExport", MODE_PRIVATE).edit().remove("pending").apply();
        }
        try {
            duration = duration(source);
            if (duration <= 0) { unavailable(); return; }
        } catch (IOException | RuntimeException error) { unavailable(); return; }
        start = state == null ? 0 : Math.min(state.getLong("start"), Math.max(0, duration-1));
        end = state == null ? duration : Math.max(start+1, Math.min(duration, state.getLong("end", duration)));
        title = findViewById(R.id.clipTitle);
        playbackTime = findViewById(R.id.clipPlaybackTime);
        range = findViewById(R.id.clipRange);
        progress = findViewById(R.id.clipProgress);
        ((TextView)findViewById(R.id.clipSourceInfo)).setText(getString(R.string.clipSourceInfo, time(duration)));
        player = new ExoPlayer.Builder(this).build();
        ((PlayerView)findViewById(R.id.clipPlayer)).setPlayer(player);
        player.addListener(new Player.Listener() {
            @Override public void onIsPlayingChanged(boolean playing) {
                ((ImageButton)findViewById(R.id.clipPreviewToggle)).setImageResource(
                        playing ? R.drawable.ic_pause : R.drawable.ic_play_arrow);
                findViewById(R.id.clipPreviewToggle).setContentDescription(
                        playing ? getString(R.string.pauseRecording) : getString(R.string.clipPreview));
            }
        });
        findViewById(R.id.clipBack).setOnClickListener(v -> onBackPressed());
        findViewById(R.id.clipPreview).setOnClickListener(v -> preparePreview(true));
        findViewById(R.id.clipPreviewToggle).setOnClickListener(v -> {
            if (player.isPlaying()) player.pause(); else preparePreview(true);
        });
        findViewById(R.id.clipStart).setOnClickListener(v -> editTime(true));
        findViewById(R.id.clipEnd).setOnClickListener(v -> editTime(false));
        findViewById(R.id.clipSave).setOnClickListener(v -> export());
        findViewById(R.id.clipCancel).setOnClickListener(v -> cancelExport());
        findViewById(R.id.clipBackToCamera).setOnClickListener(v -> finish());
        findViewById(R.id.clipPlayOriginal).setOnClickListener(v -> playFile(source));
        findViewById(R.id.clipPlaySaved).setOnClickListener(v -> playFile(savedClip));
        findViewById(R.id.clipPlaySavedButton).setOnClickListener(v -> playFile(savedClip));
        range.setListener((from, to) -> {
            start = from; end = to; updateRange(); player.pause();
            handler.removeCallbacks(rangePreview); handler.postDelayed(rangePreview, 120);
        });
        updateRange(); preparePreview(false);
        if (state != null && state.getBoolean("saved")) {
            savedClip = LatestRecordingStore.clip(this);
            if (savedClip != null) showSaved();
        }
        loadImages(); handler.post(playbackTick);
    }

    private static long duration(File file) throws IOException {
        MediaMetadataRetriever reader = new MediaMetadataRetriever();
        try {
            reader.setDataSource(file.getAbsolutePath());
            String value = reader.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            return value == null ? 0 : Long.parseLong(value);
        } finally { reader.release(); }
    }

    private void unavailable() {
        Toast.makeText(this, R.string.clipUnavailable, Toast.LENGTH_LONG).show(); finish();
    }
    private void updateRange() {
        range.setRange(duration, start, end);
        ((TextView)findViewById(R.id.clipStart)).setText(getString(R.string.clipStartField, preciseTime(start)));
        ((TextView)findViewById(R.id.clipEnd)).setText(getString(R.string.clipEndField, preciseTime(end)));
        ((TextView)findViewById(R.id.clipLength)).setText(getString(R.string.clipLength, preciseTime(end-start)));
    }
    private MediaItem selection() {
        return new MediaItem.Builder().setUri(Uri.fromFile(source))
                .setClippingConfiguration(new MediaItem.ClippingConfiguration.Builder()
                        .setStartPositionMs(start).setEndPositionMs(end).build()).build();
    }
    private void preparePreview(boolean play) {
        if (player == null || transformer != null || destroyed) return;
        handler.removeCallbacks(rangePreview);
        player.setMediaItem(selection()); player.prepare(); player.setPlayWhenReady(play);
    }
    private void editTime(boolean first) {
        player.pause();
        EditText input = new EditText(this);
        input.setSingleLine(); input.setText(preciseTime(first ? start : end)); input.selectAll();
        input.setHint(R.string.clipTimeHint);
        input.setInputType(android.text.InputType.TYPE_CLASS_TEXT);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(first ? R.string.clipStartTime : R.string.clipEndTime).setView(input)
                .setNegativeButton(android.R.string.cancel, null).setPositiveButton(android.R.string.ok, null).create();
        dialog.setOnShowListener(unused -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            try {
                long value = parseTime(input.getText().toString());
                long from = first ? value : start, to = first ? end : value;
                if (from < 0 || to <= from || to > duration) throw new IllegalArgumentException();
                start = from; end = to; updateRange(); preparePreview(false); dialog.dismiss();
            } catch (RuntimeException error) { input.setError(getString(R.string.clipInvalidRange)); }
        }));
        dialog.show();
    }
    static long parseTime(String value) {
        String[] parts = value.trim().split(":");
        if (parts.length != 2) throw new IllegalArgumentException();
        long minutes = Long.parseLong(parts[0]); double seconds = Double.parseDouble(parts[1]);
        if (minutes < 0 || minutes > Long.MAX_VALUE/60000 || seconds < 0 || seconds >= 60
                || Double.isNaN(seconds) || Double.isInfinite(seconds)) throw new IllegalArgumentException();
        long base = minutes*60000, millis = Math.round(seconds*1000);
        if (Long.MAX_VALUE-base < millis) throw new IllegalArgumentException();
        return base+millis;
    }
    private static String time(long ms) {
        return String.format(Locale.US, "%02d:%02d", ms/60000, (ms/1000)%60);
    }
    private static String preciseTime(long ms) {
        return ms%1000 == 0 ? time(ms) : time(ms)+String.format(Locale.US, ".%03d", ms%1000);
    }
    private static String seconds(long ms) {
        return ms%1000 == 0 ? String.valueOf(ms/1000) : String.format(Locale.US, "%.3f", ms/1000d);
    }

    private void panel(int visible) {
        for (int id : new int[]{R.id.clipEditor, R.id.clipExport, R.id.clipSaved})
            findViewById(id).setVisibility(id == visible ? View.VISIBLE : View.GONE);
    }
    private void export() {
        if (transformer != null) return;
        File current = LatestRecordingStore.latest(this);
        if (!source.equals(current) || !source.isFile()) { unavailable(); return; }
        if (start < 0 || end > duration || end <= start) return;
        player.pause(); handler.removeCallbacks(rangePreview);
        int generation = ++exportGeneration;
        try {
            pending = File.createTempFile(".clip_", ".mp4", source.getParentFile());
            String stamp = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US).format(new Date());
            output = new File(source.getParentFile(), "FC_CLIP_" + stamp + "_" + UUID.randomUUID().toString().substring(0,8) + ".mp4");
            getSharedPreferences("FlipCamClipExport", MODE_PRIVATE).edit().putString("pending", pending.getAbsolutePath()).apply();
            transformer = new Transformer.Builder(this).setVideoMimeType(MimeTypes.VIDEO_H264)
                    .setAudioMimeType(MimeTypes.AUDIO_AAC).addListener(new Transformer.Listener() {
                        @Override public void onCompleted(Composition composition, ExportResult result) {
                            if (generation != exportGeneration || destroyed) return;
                            transformer = null; handler.removeCallbacks(exportTick);
                            try {
                                if (pending.length() == 0 || duration(pending) <= 0 || !pending.renameTo(output))
                                    throw new IOException("Invalid exported file");
                                pending = null;
                                if (!LatestRecordingStore.saveClip(ClipActivity.this, source, output, start, end)) {
                                    output.delete(); throw new IOException("Latest original changed");
                                }
                                clearPendingPreference(); savedClip = output;
                                MediaScannerConnection.scanFile(ClipActivity.this, new String[]{savedClip.getAbsolutePath()},
                                        new String[]{"video/mp4"}, null);
                                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                                showSaved(); loadSavedImage();
                            } catch (IOException | RuntimeException error) { exportError(error); }
                        }
                        @Override public void onError(Composition composition, ExportResult result, ExportException error) {
                            if (generation == exportGeneration && !destroyed) exportError(error);
                        }
                    }).build();
            savedScreen = false; title.setText(R.string.clipExportTitle); panel(R.id.clipExport);
            progress.setProgress(-1);
            ((TextView)findViewById(R.id.clipExportCaption)).setText(getString(R.string.clipExportCaption, seconds(end-start)));
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            transformer.start(new EditedMediaItem.Builder(selection()).build(), pending.getAbsolutePath());
            handler.post(exportTick);
        } catch (IOException | RuntimeException error) { exportError(error); }
    }
    private void clearPendingPreference() {
        getSharedPreferences("FlipCamClipExport", MODE_PRIVATE).edit().remove("pending").apply();
    }
    private void cleanExport() {
        ++exportGeneration; handler.removeCallbacks(exportTick);
        if (transformer != null) { transformer.cancel(); transformer = null; }
        if (pending != null) { pending.delete(); pending = null; }
        clearPendingPreference(); getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }
    private void cancelExport() {
        cleanExport(); showEditor(); Toast.makeText(this, R.string.clipCanceled, Toast.LENGTH_SHORT).show();
    }
    private void exportError(Exception error) {
        Log.e(TAG, "Clip export failed; original retained", error);
        cleanExport(); showEditor(); Toast.makeText(this, R.string.clipFailed, Toast.LENGTH_LONG).show();
    }
    private void showEditor() {
        savedScreen = false; title.setText(R.string.clipEditorTitle); panel(R.id.clipEditor); preparePreview(false);
    }
    private void showSaved() {
        savedScreen = true; player.pause(); title.setText(R.string.clipLatestTitle); panel(R.id.clipSaved);
        ((TextView)findViewById(R.id.clipSavedOriginalInfo)).setText(getString(R.string.clipOriginalInfo, time(duration)));
        long from = LatestRecordingStore.clipStart(this), to = LatestRecordingStore.clipEnd(this);
        ((TextView)findViewById(R.id.clipSavedRange)).setText(getString(R.string.clipRangeInfo,
                preciseTime(from), preciseTime(to), seconds(to-from)));
    }

    private Bitmap frame(MediaMetadataRetriever reader, long ms) {
        if (Build.VERSION.SDK_INT >= 27)
            return reader.getScaledFrameAtTime(ms*1000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC, 640, 640);
        Bitmap full = reader.getFrameAtTime(ms*1000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
        if (full == null) return null;
        float scale = Math.min(1f, 640f / Math.max(full.getWidth(), full.getHeight()));
        Bitmap scaled = Bitmap.createScaledBitmap(full, Math.max(1,(int)(full.getWidth()*scale)), Math.max(1,(int)(full.getHeight()*scale)), true);
        if (scaled != full) full.recycle(); return scaled;
    }
    private void loadImages() {
        thumbnails.execute(() -> {
            MediaMetadataRetriever reader = new MediaMetadataRetriever();
            try {
                reader.setDataSource(source.getAbsolutePath());
                Bitmap image = frame(reader, 0);
                handler.post(() -> {
                    if (destroyed) { if (image != null) image.recycle(); return; }
                    ((ImageView)findViewById(R.id.clipExportImage)).setImageBitmap(image);
                    ((ImageView)findViewById(R.id.clipOriginalImage)).setImageBitmap(image);
                });
                for (int i=0; i<6 && !Thread.currentThread().isInterrupted(); i++) {
                    Bitmap tile = frame(reader, duration*i/6);
                    handler.post(() -> {
                        if (destroyed) { if (tile != null) tile.recycle(); }
                        else if (tile != null) range.addFrame(tile);
                    });
                }
            } catch (RuntimeException error) { Log.w(TAG, "Filmstrip unavailable", error); }
            finally { try { reader.release(); } catch (IOException ignored) { } }
        });
        if (savedScreen) loadSavedImage();
    }
    private void loadSavedImage() {
        File file = savedClip;
        if (file == null || thumbnails.isShutdown()) return;
        thumbnails.execute(() -> {
            MediaMetadataRetriever reader = new MediaMetadataRetriever();
            try {
                reader.setDataSource(file.getAbsolutePath()); Bitmap image = frame(reader, 0);
                handler.post(() -> { if (!destroyed) ((ImageView)findViewById(R.id.clipSavedImage)).setImageBitmap(image);
                    else if (image != null) image.recycle(); });
            } catch (RuntimeException error) { Log.w(TAG, "Export thumbnail unavailable", error); }
            finally { try { reader.release(); } catch (IOException ignored) { } }
        });
    }
    private void playFile(File file) {
        if (file == null || !file.isFile()) {
            Toast.makeText(this, R.string.clipUnavailable, Toast.LENGTH_SHORT).show(); return;
        }
        try {
            Uri uri = FileProvider.getUriForFile(this, BuildConfig.APPLICATION_ID+".provider", file);
            Intent play = new Intent(Intent.ACTION_VIEW).setDataAndType(uri,"video/mp4")
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(play,null));
        } catch (ActivityNotFoundException | IllegalArgumentException | SecurityException error) {
            Toast.makeText(this, "Unable to open this video", Toast.LENGTH_LONG).show();
        }
    }
    @Override public void onBackPressed() {
        if (transformer != null) { cancelExport(); return; }
        super.onBackPressed();
    }
    @Override protected void onSaveInstanceState(Bundle state) {
        state.putLong("start", start); state.putLong("end", end); state.putBoolean("saved", savedScreen);
        super.onSaveInstanceState(state);
    }
    @Override protected void onStop() {
        if (player != null) player.pause(); super.onStop();
    }
    @Override protected void onDestroy() {
        destroyed = true; cleanExport(); handler.removeCallbacksAndMessages(null);
        if (player != null) player.release();
        thumbnails.shutdownNow(); if (range != null) range.releaseFrames();
        super.onDestroy();
    }
}
