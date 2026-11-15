package com.example.aquamotion;

import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.service.wallpaper.WallpaperService;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;

import java.io.IOException;

public class AquaMotionWallpaperService extends WallpaperService {
    private static final String TAG = "AquaMotionWallpaper";

    @Override
    public Engine onCreateEngine() {
        return new VideoWallpaperEngine();
    }

    private class VideoWallpaperEngine extends Engine {
        private MediaPlayer player;
        private SurfaceHolder holder;
        private boolean visible;
        private boolean prepared;
        private boolean destroyed;

        @Override
        public void onCreate(SurfaceHolder surfaceHolder) {
            super.onCreate(surfaceHolder);
            setTouchEventsEnabled(false);
            holder = surfaceHolder;
            Log.i(TAG, "Engine created");
        }

        @Override
        public void onSurfaceCreated(SurfaceHolder surfaceHolder) {
            super.onSurfaceCreated(surfaceHolder);
            holder = surfaceHolder;
            Log.i(TAG, "Surface created; valid=" + surfaceHolder.getSurface().isValid());
            startPlayerForSurface();
        }

        @Override
        public void onSurfaceChanged(SurfaceHolder surfaceHolder, int format, int width, int height) {
            super.onSurfaceChanged(surfaceHolder, format, width, height);
            holder = surfaceHolder;
            Log.i(TAG, "Surface changed " + width + "x" + height);
            if (player == null) startPlayerForSurface();
        }

        private void startPlayerForSurface() {
            if (destroyed || holder == null) return;
            Surface surface = holder.getSurface();
            if (surface == null || !surface.isValid()) {
                Log.w(TAG, "Surface not valid yet");
                return;
            }

            releasePlayer();
            MediaPlayer mp = new MediaPlayer();
            player = mp;
            prepared = false;

            try {
                mp.setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MOVIE)
                        .build());
                mp.setVolume(0f, 0f);
                mp.setLooping(true);

                // Bind directly to the live wallpaper Surface. setSurface is more
                // reliable here than relying on the legacy display wrapper.
                mp.setSurface(surface);

                mp.setOnPreparedListener(preparedPlayer -> {
                    if (player != preparedPlayer || destroyed) return;
                    Log.i(TAG, "Video prepared: "
                            + preparedPlayer.getVideoWidth() + "x"
                            + preparedPlayer.getVideoHeight());
                    prepared = true;
                    updatePlayback();
                });
                mp.setOnVideoSizeChangedListener((videoPlayer, width, height) ->
                        Log.i(TAG, "Video size changed " + width + "x" + height));
                mp.setOnCompletionListener(completed -> {
                    if (player == completed) {
                        try {
                            completed.seekTo(0);
                            if (visible) completed.start();
                        } catch (IllegalStateException e) {
                            Log.e(TAG, "Restart after completion failed", e);
                        }
                    }
                });
                mp.setOnErrorListener((errorPlayer, what, extra) -> {
                    Log.e(TAG, "MediaPlayer error what=" + what + " extra=" + extra);
                    if (player == errorPlayer) releasePlayer();
                    return true;
                });
                mp.setOnInfoListener((infoPlayer, what, extra) -> {
                    Log.i(TAG, "MediaPlayer info what=" + what + " extra=" + extra);
                    return false;
                });

                try (android.content.res.AssetFileDescriptor afd =
                             getAssets().openFd("aqua_motion.mp4")) {
                    Log.i(TAG, "Asset length=" + afd.getLength());
                    mp.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
                }

                mp.prepareAsync();
                Log.i(TAG, "prepareAsync requested; visible=" + visible);
            } catch (IOException | RuntimeException e) {
                Log.e(TAG, "Could not initialize video player", e);
                if (player == mp) releasePlayer();
            }
        }

        @Override
        public void onVisibilityChanged(boolean isVisible) {
            visible = isVisible;
            Log.i(TAG, "Visibility changed: " + isVisible);
            if (visible && player == null && holder != null) {
                startPlayerForSurface();
            } else {
                updatePlayback();
            }
        }

        private void updatePlayback() {
            MediaPlayer current = player;
            if (current == null || !prepared) return;
            try {
                if (visible && holder != null && holder.getSurface().isValid()) {
                    if (!current.isPlaying()) current.start();
                    Log.i(TAG, "Playback started");
                } else if (current.isPlaying()) {
                    current.pause();
                    Log.i(TAG, "Playback paused");
                }
            } catch (IllegalStateException e) {
                Log.e(TAG, "Playback state error", e);
                releasePlayer();
            }
        }

        @Override
        public void onSurfaceDestroyed(SurfaceHolder surfaceHolder) {
            Log.i(TAG, "Surface destroyed");
            holder = null;
            releasePlayer();
            super.onSurfaceDestroyed(surfaceHolder);
        }

        private void releasePlayer() {
            prepared = false;
            MediaPlayer old = player;
            player = null;
            if (old != null) {
                try { old.setOnPreparedListener(null); } catch (RuntimeException ignored) { }
                try { old.setOnCompletionListener(null); } catch (RuntimeException ignored) { }
                try { old.setOnErrorListener(null); } catch (RuntimeException ignored) { }
                try { old.setOnInfoListener(null); } catch (RuntimeException ignored) { }
                try { old.setOnVideoSizeChangedListener(null); } catch (RuntimeException ignored) { }
                try { old.setSurface(null); } catch (RuntimeException ignored) { }
                try { old.reset(); } catch (RuntimeException ignored) { }
                try { old.release(); } catch (RuntimeException ignored) { }
            }
        }

        @Override
        public void onDestroy() {
            Log.i(TAG, "Engine destroyed");
            destroyed = true;
            visible = false;
            holder = null;
            releasePlayer();
            super.onDestroy();
        }
    }
}
