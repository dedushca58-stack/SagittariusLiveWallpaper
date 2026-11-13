package com.example.sagittariuswallpaper;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.Handler;
import android.os.Looper;
import android.service.wallpaper.WallpaperService;
import android.view.SurfaceHolder;

import java.io.InputStream;

/** Original wallpaper plus a translucent, slowly drifting portrait reflection in the medallion. */
public class SagittariusWallpaperService extends WallpaperService {
    @Override public Engine onCreateEngine() { return new WallpaperEngine(); }

    private class WallpaperEngine extends Engine {
        private static final long FRAME_DELAY_MS = 40L;
        // Geometry measured against the original 813 x 1933 wallpaper.
        private static final float MEDALLION_X = 405f;
        private static final float MEDALLION_Y = 1190f;
        private static final float MEDALLION_RADIUS = 190f;

        private final Handler handler = new Handler(Looper.getMainLooper());
        private final Paint bitmapPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        private final Paint portraitPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        private final Paint lightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Matrix imageMatrix = new Matrix();

        private Bitmap wallpaper;
        private Bitmap portraitReflection;
        private boolean visible;
        private boolean surfaceReady;
        private int surfaceWidth, surfaceHeight;
        private float imageScale = 1f, imageOffsetX, imageOffsetY;
        private float phase;

        private final Runnable drawTask = new Runnable() {
            @Override public void run() {
                if (!visible || !surfaceReady) return;
                drawFrame();
                if (visible) handler.postDelayed(this, FRAME_DELAY_MS);
            }
        };

        WallpaperEngine() {
            wallpaper = BitmapFactory.decodeResource(getResources(), R.drawable.wallpaper);
            try (InputStream in = getAssets().open("portrait_reflection.png")) {
                portraitReflection = BitmapFactory.decodeStream(in);
            } catch (Exception ignored) {
                // The original wallpaper still works if the optional portrait asset is not installed yet.
            }
            setTouchEventsEnabled(false);
        }

        @Override public void onVisibilityChanged(boolean isVisible) {
            visible = isVisible;
            handler.removeCallbacks(drawTask);
            if (visible && surfaceReady) drawTask.run();
        }

        @Override public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            super.onSurfaceChanged(holder, format, width, height);
            surfaceWidth = width;
            surfaceHeight = height;
            surfaceReady = width > 0 && height > 0;
            updateImageTransform();
            drawFrame();
            if (visible) {
                handler.removeCallbacks(drawTask);
                handler.postDelayed(drawTask, FRAME_DELAY_MS);
            }
        }

        @Override public void onSurfaceDestroyed(SurfaceHolder holder) {
            surfaceReady = false;
            visible = false;
            handler.removeCallbacks(drawTask);
            super.onSurfaceDestroyed(holder);
        }

        @Override public void onDestroy() {
            visible = false;
            surfaceReady = false;
            handler.removeCallbacks(drawTask);
            if (wallpaper != null && !wallpaper.isRecycled()) wallpaper.recycle();
            if (portraitReflection != null && !portraitReflection.isRecycled()) portraitReflection.recycle();
            wallpaper = null;
            portraitReflection = null;
            super.onDestroy();
        }

        private void updateImageTransform() {
            if (wallpaper == null || surfaceWidth <= 0 || surfaceHeight <= 0) return;
            imageScale = Math.max(surfaceWidth / (float) wallpaper.getWidth(),
                                  surfaceHeight / (float) wallpaper.getHeight());
            imageOffsetX = (surfaceWidth - wallpaper.getWidth() * imageScale) * 0.5f;
            imageOffsetY = (surfaceHeight - wallpaper.getHeight() * imageScale) * 0.5f;
            imageMatrix.setScale(imageScale, imageScale);
            imageMatrix.postTranslate(imageOffsetX, imageOffsetY);
        }

        private void drawFrame() {
            if (wallpaper == null || !surfaceReady) return;
            SurfaceHolder holder = getSurfaceHolder();
            Canvas canvas = null;
            try {
                canvas = holder.lockCanvas();
                if (canvas == null) return;
                canvas.drawColor(Color.BLACK);
                // The user's original artwork is always the untouched base layer.
                canvas.drawBitmap(wallpaper, imageMatrix, bitmapPaint);
                drawPortraitReflection(canvas);
                drawGlassHighlights(canvas);
            } catch (RuntimeException ignored) {
                // Android may release the wallpaper surface during a screen transition.
            } finally {
                if (canvas != null) {
                    try { holder.unlockCanvasAndPost(canvas); }
                    catch (RuntimeException ignored) { }
                }
            }
        }

        private void drawPortraitReflection(Canvas canvas) {
            if (portraitReflection == null) return;

            float cx = imageOffsetX + MEDALLION_X * imageScale;
            float cy = imageOffsetY + MEDALLION_Y * imageScale;
            float radius = MEDALLION_RADIUS * imageScale;

            // Very slow, gentle drift. The image itself is translucent (34% alpha),
            // so the original gold Sagittarius emblem remains visible through it.
            double wave = phase * Math.PI * 2.0;
            float driftX = (float) Math.sin(wave) * radius * 0.15f;
            float driftY = (float) Math.sin(wave * 0.5) * radius * 0.025f;
            float breathing = 1f + (float) Math.sin(wave) * 0.012f;
            float half = radius * 0.91f * breathing;

            canvas.save();
            Path circle = new Path();
            circle.addCircle(cx, cy, radius * 0.94f, Path.Direction.CW);
            canvas.clipPath(circle);

            RectF target = new RectF(cx - half + driftX, cy - half + driftY,
                                     cx + half + driftX, cy + half + driftY);
            portraitPaint.setAlpha(255);
            portraitPaint.setShader(null);
            canvas.drawBitmap(portraitReflection, null, target, portraitPaint);
            canvas.restore();
        }

        private void drawGlassHighlights(Canvas canvas) {
            float cx = imageOffsetX + MEDALLION_X * imageScale;
            float cy = imageOffsetY + MEDALLION_Y * imageScale;
            float radius = MEDALLION_RADIUS * imageScale;

            canvas.save();
            Path circle = new Path();
            circle.addCircle(cx, cy, radius * 0.94f, Path.Direction.CW);
            canvas.clipPath(circle);

            float travel = (float) Math.sin(phase * Math.PI * 2.0) * radius * 0.20f;
            float beamX = cx + travel;
            lightPaint.setShader(new RadialGradient(
                    beamX, cy - radius * 0.12f, radius * 0.78f,
                    new int[] { 0x25ffffff, 0x12d8f6ff, 0x00ffffff },
                    new float[] { 0f, 0.5f, 1f }, Shader.TileMode.CLAMP));
            canvas.drawCircle(beamX, cy - radius * 0.12f, radius * 0.78f, lightPaint);
            lightPaint.setShader(null);

            float angle = (phase * 360f) - 90f;
            double radians = Math.toRadians(angle);
            float gx = cx + (float) Math.cos(radians) * radius * 0.80f;
            float gy = cy + (float) Math.sin(radians) * radius * 0.80f;
            lightPaint.setShader(new RadialGradient(
                    gx, gy, radius * 0.18f,
                    new int[] { 0x55ffffff, 0x18ffffff, 0x00ffffff },
                    null, Shader.TileMode.CLAMP));
            canvas.drawCircle(gx, gy, radius * 0.18f, lightPaint);
            lightPaint.setShader(null);
            canvas.restore();

            phase += 0.00055f;
            if (phase >= 1f) phase -= 1f;
        }
    }
}
