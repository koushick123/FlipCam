package com.flipcam.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Ordered recording configurations, not independent codec maxima. No Android dependency. */
public final class AudioRecordingPolicy {
    private AudioRecordingPolicy() { }

    public static final class Config {
        public final int encoder, sampleRate, bitRate, channels;
        public final String origin;
        public Config(int encoder, int sampleRate, int bitRate, int channels, String origin) {
            this.encoder = encoder; this.sampleRate = sampleRate;
            this.bitRate = bitRate; this.channels = channels; this.origin = origin;
        }
        public boolean valid() {
            return encoder >= 3 && encoder <= 5 && sampleRate > 0 && bitRate > 0
                    && channels >= 1 && channels <= 2;
        }
        @Override public String toString() {
            return origin + ": encoder=" + encoder + ", rate=" + sampleRate
                    + ", bitrate=" + bitRate + ", channels=" + channels;
        }
    }

    public static List<Config> candidates(int encoder, int rate, int bitrate, int channels) {
        List<Config> result = new ArrayList<>();
        add(result, new Config(encoder, rate, bitrate, channels, "device-profile"));
        // Preserve a device's mono preference. Stereo is not a quality upgrade by itself.
        if (channels == 2) add(result, new Config(3, 48000, 192000, 2, "fallback"));
        add(result, new Config(3, 48000, 96000, 1, "fallback"));
        if (channels == 2) add(result, new Config(3, 44100, 128000, 2, "fallback"));
        add(result, new Config(3, 44100, 96000, 1, "fallback"));
        add(result, new Config(3, 16000, 64000, 1, "fallback"));
        return Collections.unmodifiableList(result);
    }

    private static void add(List<Config> result, Config config) {
        if (!config.valid()) return;
        for (Config existing : result) {
            if (existing.encoder == config.encoder && existing.sampleRate == config.sampleRate
                    && existing.bitRate == config.bitRate && existing.channels == config.channels) return;
        }
        result.add(config);
    }

    public static final class CleanupFailure extends IllegalStateException {
        public CleanupFailure(Throwable cause) { super("Cannot release rejected recorder", cause); }
    }

    public interface Attempt<T> {
        boolean supports(Config config);
        /** Must release any partially created recorder before throwing. */
        T prepare(Config config) throws Exception;
    }

    public static final class Prepared<T> {
        public final Config config;
        public final T recorder;
        Prepared(Config config, T recorder) { this.config = config; this.recorder = recorder; }
    }

    public static <T> Prepared<T> prepare(List<Config> candidates, Attempt<T> attempt) {
        IllegalStateException failure = new IllegalStateException("No supported audio recording configuration could be prepared");
        for (Config config : candidates) {
            try {
                if (!config.valid() || !attempt.supports(config)) continue;
                T recorder = attempt.prepare(config);
                if (recorder == null) throw new IllegalStateException("Recorder preparation returned null");
                return new Prepared<>(config, recorder);
            } catch (CleanupFailure terminal) {
                throw terminal;
            } catch (Exception rejected) {
                failure.addSuppressed(rejected);
            }
        }
        throw failure;
    }
}
