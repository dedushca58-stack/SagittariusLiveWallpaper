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

/**
 * Renders the user's original wallpaper bitmap without redrawing or altering it.
 * The only animated pixels are translucent reflections clipped to the blue medallion.
 */
public class SagittariusWallpaperService extends WallpaperService {
    @Override
    public Engine onCreateEngine() {
        return new WallpaperEngine();
    }

    private class WallpaperEngine extends Engine {
        private static final long FRAME_DELAY_MS = 40L;
        // Medallion geometry in the original 813 x 1933 wallpaper image.
        private static final float MEDALLION_X = 405f;
        private static final float MEDALLION_Y = 1190f;
        private static final float MEDALLION_RADIUS = 190f;
        // Keep the brightest reflection away from the gold Sagittarius emblem.
        private static final float EMBLEM_CLEAR_RADIUS = 48f;

        private final Handler handler = new Handler(Looper.getMainLooper());
        private final Paint bitmapPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        private final Paint lightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Matrix imageMatrix = new Matrix();
        private Bitmap wallpaper;
        private boolean visible;
        private boolean surfaceReady;
        private int surfaceWidth;
        private int surfaceHeight;
        private float imageScale = 1f;
        private float imageOffsetX;
        private float imageOffsetY;
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
            if (wallpaper != null && !wallpaper.isRecycled()) {
                wallpaper.recycle();
                wallpaper = null;
            }
            super.onDestroy();
        }

        private void updateImageTransform() {
            if (wallpaper == null || surfaceWidth <= 0 || surfaceHeight <= 0) return;

            // Center-crop uniformly to fill the display, preserving the original artwork's proportions.
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

                // First draw the exact uploaded bitmap. No generated replacement artwork.
                canvas.drawColor(Color.BLACK);
                canvas.drawBitmap(wallpaper, imageMatrix, bitmapPaint);

                drawMedallionReflections(canvas);
            } catch (RuntimeException ignored) {
                // The wallpaper surface can disappear while Android is changing screens.
            } finally {
                if (canvas != null) {
                    try {
                        holder.unlockCanvasAndPost(canvas);
                    } catch (RuntimeException ignored) {
                        // Surface was already released by the system.
                    }
                }
            }
        }

        private void drawMedallionReflections(Canvas canvas) {
            float cx = imageOffsetX + MEDALLION_X * imageScale;
            float cy = imageOffsetY + MEDALLION_Y * imageScale;
            float radius = MEDALLION_RADIUS * imageScale;
            float emblemClear = EMBLEM_CLEAR_RADIUS * imageScale;

            canvas.save();

            // Even-odd path makes a transparent central opening, so the gold symbol
            // remains exactly as it appears in the original bitmap.
            Path reflectionArea = new Path();
            reflectionArea.setFillType(Path.FillType.EVEN_ODD);
            reflectionArea.addCircle(cx, cy, radius, Path.Direction.CW);
            reflectionArea.addCircle(cx, cy, emblemClear, Path.Direction.CCW);
            canvas.clipPath(reflectionArea);

            float travel = (phase - 0.5f) * radius * 3.0f;
            float beamX = cx + travel;
            float beamY = cy - radius * 0.10f;

            // Broad, soft reflection that glides slowly across the blue glass.
            lightPaint.setStyle(Paint.Style.FILL);
            lightPaint.setShader(new RadialGradient(
                    beamX, beamY, radius * 0.88f,
                    new int[] { 0x38ffffff, 0x19d8f6ff, 0x00ffffff },
                    new float[] { 0f, 0.48f, 1f },
                    Shader.TileMode.CLAMP));
            canvas.drawCircle(beamX, beamY, radius * 0.88f, lightPaint);
            lightPaint.setShader(null);

            // Narrow diagonal specular highlight; low opacity to preserve the image.
            canvas.save();
            canvas.rotate(-27f, cx, cy);
            float streakX = cx + travel * 1.18f;
            RectF streak = new RectF(
                    streakX - radius * 1.25f,
                    cy - radius * 0.10f,
                    streakX + radius * 1.25f,
                    cy + radius * 0.10f);
            lightPaint.setShader(new android.graphics.LinearGradient(
                    streak.left, streak.top, streak.right, streak.bottom,
                    new int[] { 0x00ffffff, 0x12ffffff, 0x42ffffff, 0x12ffffff, 0x00ffffff },
                    new float[] { 0f, 0.34f, 0.5f, 0.66f, 1f },
                    Shader.TileMode.CLAMP));
            canvas.drawRoundRect(streak, radius * 0.1f, radius * 0.1f, lightPaint);
            lightPaint.setShader(null);
            canvas.restore();

            // A subtle moving glint near the edge of the medallion.
            float angle = (phase * 360f) - 90f;
            double radians = Math.toRadians(angle);
            float glintX = cx + (float) Math.cos(radians) * radius * 0.78f;
            float glintY = cy + (float) Math.sin(radians) * radius * 0.78f;
            lightPaint.setShader(new RadialGradient(
                    glintX, glintY, radius * 0.22f,
                    new int[] { 0x66ffffff, 0x20ffffff, 0x00ffffff },
                    null, Shader.TileMode.CLAMP));
            canvas.drawCircle(glintX, glintY, radius * 0.22f, lightPaint);
            lightPaint.setShader(null);

            canvas.restore();

            phase += 0.0035f;
            if (phase >= 1f) phase -= 1f;
        }
    }
}
