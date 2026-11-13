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

/**
 * Displays the original wallpaper and a subtle, slowly moving portrait reflection
 * inside the blue medallion. The original Sagittarius symbol remains underneath.
 */
public class SagittariusWallpaperService extends WallpaperService {
    @Override
    public Engine onCreateEngine() {
        return new WallpaperEngine();
    }

    private class WallpaperEngine extends Engine {
        private static final long FRAME_DELAY_MS = 40L;
        // Coordinates in the original 813 x 1933 wallpaper.
        private static final float MEDALLION_X = 405f;
        private static final float MEDALLION_Y = 1190f;
        private static final float MEDALLION_RADIUS = 190f;
        private static final float PORTRAIT_SIZE = 338f;

        private final Handler handler = new Handler(Looper.getMainLooper());
        private final Paint bitmapPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        private final Paint portraitPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        private final Paint lightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Matrix imageMatrix = new Matrix();
        private Bitmap wallpaper;
        private Bitmap portraitReflection;
        private boolean visible;
        private boolean surfaceReady;
        private int surfaceWidth;
        private int surfaceHeight;
        private float imageScale = 1f;
        private float imageOffsetX;
        private float imageOffsetY;
        private float phase;
        private float portraitPhase;

        private final Runnable drawTask = new Runnable() {
            @Override public void run() {
                if (!visible || !surfaceReady) return;
                drawFrame();
                if (visible) handler.postDelayed(this, FRAME_DELAY_MS);
            }
        };

        WallpaperEngine() {
            wallpaper = BitmapFactory.decodeResource(getResources(), R.drawable.wallpaper);
            try (InputStream input = getAssets().open("portrait_reflection.png")) {
                portraitReflection = BitmapFactory.decodeStream(input);
            } catch (Exception ignored) {
                portraitReflection = null;
            }
            // The PNG itself carries reduced alpha, so the gold symbol shows through it.
            portraitPaint.setAlpha(255);
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
            imageScale = Math.max(
                    surfaceWidth / (float) wallpaper.getWidth(),
                    surfaceHeight / (float) wallpaper.getHeight());
            float drawnWidth = wallpaper.getWidth() * imageScale;
            float drawnHeight = wallpaper.getHeight() * imageScale;
            imageOffsetX = (surfaceWidth - drawnWidth) * 0.5f;
            imageOffsetY = (surfaceHeight - drawnHeight) * 0.5f;
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
                canvas.drawBitmap(wallpaper, imageMatrix, bitmapPaint);
                drawPortraitReflection(canvas);
                drawMedallionGlints(canvas);
            } catch (RuntimeException ignored) {
                // Android may release the surface during screen transitions.
            } finally {
                if (canvas != null) {
                    try {
                        holder.unlockCanvasAndPost(canvas);
                    } catch (RuntimeException ignored) {
                        // Surface was already released.
                    }
                }
            }
        }

        private void drawPortraitReflection(Canvas canvas) {
            if (portraitReflection == null) return;

            float cx = imageOffsetX + MEDALLION_X * imageScale;
            float cy = imageOffsetY + MEDALLION_Y * imageScale;
            float radius = MEDALLION_RADIUS * imageScale;
            float size = PORTRAIT_SIZE * imageScale;

            // A very slow drift, like a reflection moving across curved glass.
            float drift = (float) Math.sin(portraitPhase * Math.PI * 2.0) * 17f * imageScale;
            float bob = (float) Math.sin(portraitPhase * Math.PI * 4.0) * 4f * imageScale;
            float scale = 1f + 0.012f * (float) Math.sin(portraitPhase * Math.PI * 2.0);

            canvas.save();
            canvas.clipPath(new Path() {{
                addCircle(cx, cy, radius * 0.94f, Path.Direction.CW);
            }});

            float half = size * scale * 0.5f;
            RectF dst = new RectF(cx - half + drift, cy - half + bob,
                    cx + half + drift, cy + half + bob);
            canvas.drawBitmap(portraitReflection, null, dst, portraitPaint);
            canvas.restore();

            // Approximately one full slow pass every 75 seconds.
            portraitPhase += 0.00053f;
            if (portraitPhase >= 1f) portraitPhase -= 1f;
        }

        private void drawMedallionGlints(Canvas canvas) {
            float cx = imageOffsetX + MEDALLION_X * imageScale;
            float cy = imageOffsetY + MEDALLION_Y * imageScale;
            float radius = MEDALLION_RADIUS * imageScale;

            canvas.save();
            canvas.clipPath(new Path() {{
                addCircle(cx, cy, radius * 0.97f, Path.Direction.CW);
            }});

            float travel = (phase - 0.5f) * radius * 2.2f;
            float beamX = cx + travel;
            lightPaint.setStyle(Paint.Style.FILL);
            lightPaint.setShader(new RadialGradient(
                    beamX, cy - radius * 0.12f, radius * 0.82f,
                    new int[] { 0x20ffffff, 0x10d8f6ff, 0x00ffffff },
                    new float[] { 0f, 0.48f, 1f }, Shader.TileMode.CLAMP));
            canvas.drawCircle(beamX, cy - radius * 0.12f, radius * 0.82f, lightPaint);
            lightPaint.setShader(null);

            float angle = phase * 360f - 90f;
            double radians = Math.toRadians(angle);
            float gx = cx + (float) Math.cos(radians) * radius * 0.82f;
            float gy = cy + (float) Math.sin(radians) * radius * 0.82f;
            lightPaint.setShader(new RadialGradient(
                    gx, gy, radius * 0.16f,
                    new int[] { 0x55ffffff, 0x18ffffff, 0x00ffffff },
                    null, Shader.TileMode.CLAMP));
            canvas.drawCircle(gx, gy, radius * 0.16f, lightPaint);
            lightPaint.setShader(null);
            canvas.restore();

            phase += 0.0018f;
            if (phase >= 1f) phase -= 1f;
        }
    }
}
