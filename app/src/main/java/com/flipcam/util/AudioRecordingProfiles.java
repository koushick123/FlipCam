package com.flipcam.util;

import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.util.Log;

/** Check the complete AAC configuration against encoders, never decoder maxima. */
public final class AudioRecordingProfiles {
    private AudioRecordingProfiles() { }

    public static boolean supports(AudioRecordingPolicy.Config config) {
        if (!config.valid()) return false;
        MediaFormat format = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC,
                config.sampleRate, config.channels);
        format.setInteger(MediaFormat.KEY_BIT_RATE, config.bitRate);
        int aacProfile = config.encoder == 4 ? MediaCodecInfo.CodecProfileLevel.AACObjectHE
                : config.encoder == 5 ? MediaCodecInfo.CodecProfileLevel.AACObjectELD
                : MediaCodecInfo.CodecProfileLevel.AACObjectLC;
        format.setInteger(MediaFormat.KEY_AAC_PROFILE, aacProfile);
        try {
            for (MediaCodecInfo info : new MediaCodecList(MediaCodecList.REGULAR_CODECS).getCodecInfos()) {
                if (!info.isEncoder()) continue;
                for (String type : info.getSupportedTypes()) {
                    if (!MediaFormat.MIMETYPE_AUDIO_AAC.equalsIgnoreCase(type)) continue;
                    try {
                        MediaCodecInfo.CodecCapabilities caps = info.getCapabilitiesForType(type);
                        MediaCodecInfo.AudioCapabilities audio = caps.getAudioCapabilities();
                        if (audio != null && audio.isSampleRateSupported(config.sampleRate)
                                && audio.getBitrateRange().contains(config.bitRate)
                                && config.channels <= audio.getMaxInputChannelCount()
                                && caps.isFormatSupported(format)) return true;
                    } catch (RuntimeException unavailable) {
                        Log.w("AudioRecordingProfiles", "Cannot inspect encoder " + info.getName(), unavailable);
                    }
                }
            }
        } catch (RuntimeException unavailable) {
            Log.w("AudioRecordingProfiles", "Audio encoder enumeration failed", unavailable);
        }
        return false;
    }
}
