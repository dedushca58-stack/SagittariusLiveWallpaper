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
        private SurfaceHolder currentHolder;

        @Override
        public void onCreate(SurfaceHolder surfaceHolder) {
            super.onCreate(surfaceHolder);
            setTouchEventsEnabled(false);
        }

        @Override
        public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            super.onSurfaceChanged(holder, format, width, height);
            currentHolder = holder;
            if (player == null) {
                createPlayer(holder);
            } else if (prepared) {
                player.setSurface(holder.getSurface());
                updatePlayback();
            }
        }

        private void createPlayer(SurfaceHolder holder) {
            try {
                player = new MediaPlayer();
                player.setLooping(true);
                player.setVolume(0f, 0f);
                player.setVideoScalingMode(MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING);
                player.setSurface(holder.getSurface());

                try (android.content.res.AssetFileDescriptor afd =
                             getAssets().openFd("aqua_motion.mp4")) {
                    player.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
                }

                player.setOnPreparedListener(mp -> {
                    prepared = true;
                    updatePlayback();
                });
                player.setOnErrorListener((mp, what, extra) -> {
                    releasePlayer();
                    return true;
                });
                player.prepareAsync();
            } catch (IOException | IllegalArgumentException | IllegalStateException e) {
                releasePlayer();
            }
        }

        @Override
        public void onVisibilityChanged(boolean isVisible) {
            visible = isVisible;
            updatePlayback();
        }

        private void updatePlayback() {
            if (player == null || !prepared) return;
            try {
                if (visible && currentHolder != null && currentHolder.getSurface().isValid()) {
                    if (!player.isPlaying()) player.start();
                } else if (player.isPlaying()) {
                    player.pause();
                }
            } catch (IllegalStateException ignored) {
                // The system can destroy the wallpaper surface during transitions.
            }
        }

        @Override
        public void onSurfaceDestroyed(SurfaceHolder holder) {
            currentHolder = null;
            if (player != null) {
                try {
                    player.setSurface(null);
                    if (prepared && player.isPlaying()) player.pause();
                } catch (IllegalStateException ignored) { }
            }
            super.onSurfaceDestroyed(holder);
        }

        private void releasePlayer() {
            prepared = false;
            if (player != null) {
                try { player.setSurface(null); } catch (Exception ignored) { }
                try { player.reset(); } catch (Exception ignored) { }
                try { player.release(); } catch (Exception ignored) { }
                player = null;
            }
        }

        @Override
        public void onDestroy() {
            visible = false;
            releasePlayer();
            super.onDestroy();
        }
    }
}
