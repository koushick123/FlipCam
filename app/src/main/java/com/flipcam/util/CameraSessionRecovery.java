package com.flipcam.util;

/** Bounded camera opening/rollback policy; independent of Android for failure-path tests. */
public final class CameraSessionRecovery {
    private CameraSessionRecovery() { }

    public interface Session {
        void close();
        void open(boolean backFacing);
        boolean isReady();
    }

    public static final class Result {
        public final boolean available;
        public final boolean backFacing;
        public final boolean restored;
        public final RuntimeException failure;

        private Result(boolean available, boolean backFacing, boolean restored, RuntimeException failure) {
            this.available = available;
            this.backFacing = backFacing;
            this.restored = restored;
            this.failure = failure;
        }
    }

    public static Result open(boolean backFacing, Session session) {
        return attempt(backFacing, backFacing, false, session);
    }

    public static Result switchFacing(boolean previousBackFacing, Session session) {
        return attempt(!previousBackFacing, previousBackFacing, true, session);
    }

    private static Result attempt(boolean requested, boolean previous, boolean rollback, Session session) {
        close(session);
        RuntimeException failure;
        try {
            openReady(session, requested);
            return new Result(true, requested, false, null);
        } catch (RuntimeException error) {
            failure = error;
        }
        close(session);
        if (rollback) {
            try {
                openReady(session, previous);
                return new Result(true, previous, true, failure);
            } catch (RuntimeException restoreError) {
                if (restoreError != failure) restoreError.addSuppressed(failure);
                failure = restoreError;
            }
            close(session);
        }
        return new Result(false, previous, false, failure);
    }

    private static void openReady(Session session, boolean backFacing) {
        session.open(backFacing);
        if (!session.isReady()) throw new IllegalStateException("Requested camera is unavailable");
    }

    private static void close(Session session) {
        // Cleanup errors must not skip an opening/rollback attempt.
        try { session.close(); } catch (RuntimeException ignored) { }
    }
}
