package com.example.aquamotion;

import android.media.MediaPlayer;
import android.service.wallpaper.WallpaperService;
import android.view.SurfaceHolder;

import java.io.IOException;

public class AquaMotionWallpaperService extends WallpaperService {
    @Override
    public Engine onCreateEngine() {
        return new VideoWallpaperEngine();
    }

    private class VideoWallpaperEngine extends Engine {
        private MediaPlayer player;
        private boolean visible;
        private boolean prepared;
        private SurfaceHolder holder;

        @Override
        public void onCreate(SurfaceHolder surfaceHolder) {
            super.onCreate(surfaceHolder);
            setTouchEventsEnabled(false);
        }

        @Override
        public void onSurfaceChanged(SurfaceHolder surfaceHolder, int format, int width, int height) {
            super.onSurfaceChanged(surfaceHolder, format, width, height);
            holder = surfaceHolder;
            // Recreate the player for the current live-wallpaper surface. This avoids
            // retaining a stale Surface across preview/apply transitions on Android.
            releasePlayer();
            createPlayer();
        }

        private void createPlayer() {
            if (holder == null || !holder.getSurface().isValid()) return;
            MediaPlayer newPlayer = new MediaPlayer();
            player = newPlayer;
            prepared = false;
            try {
                newPlayer.setLooping(true);
                newPlayer.setVolume(0f, 0f);
                newPlayer.setVideoScalingMode(
                        MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING);

                try (android.content.res.AssetFileDescriptor afd =
                             getAssets().openFd("aqua_motion.mp4")) {
                    newPlayer.setDataSource(
                            afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
                }

                newPlayer.setDisplay(holder);
                newPlayer.setOnPreparedListener(mp -> {
                    if (player != mp) return;
                    prepared = true;
                    updatePlayback();
                });
                newPlayer.setOnErrorListener((mp, what, extra) -> {
                    if (player == mp) releasePlayer();
                    return true;
                });
                newPlayer.prepareAsync();
            } catch (IOException | RuntimeException e) {
                releasePlayer();
            }
        }

        @Override
        public void onVisibilityChanged(boolean isVisible) {
            visible = isVisible;
            updatePlayback();
        }

        private void updatePlayback() {
            MediaPlayer current = player;
            if (current == null || !prepared) return;
            try {
                if (visible && holder != null && holder.getSurface().isValid()) {
                    if (!current.isPlaying()) current.start();
                } else if (current.isPlaying()) {
                    current.pause();
                }
            } catch (IllegalStateException e) {
                releasePlayer();
            }
        }

        @Override
        public void onSurfaceDestroyed(SurfaceHolder surfaceHolder) {
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
                try { old.setOnErrorListener(null); } catch (RuntimeException ignored) { }
                try { old.reset(); } catch (RuntimeException ignored) { }
                try { old.release(); } catch (RuntimeException ignored) { }
            }
        }

        @Override
        public void onDestroy() {
            visible = false;
            holder = null;
            releasePlayer();
            super.onDestroy();
        }
    }
}
