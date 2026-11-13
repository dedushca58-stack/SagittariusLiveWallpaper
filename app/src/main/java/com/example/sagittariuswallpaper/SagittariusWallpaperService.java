package com.example.sagittariuswallpaper;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.Handler;
import android.os.Looper;
import android.service.wallpaper.WallpaperService;
import android.view.SurfaceHolder;

public class SagittariusWallpaperService extends WallpaperService {
    @Override public Engine onCreateEngine() { return new LiveEngine(); }
    private class LiveEngine extends Engine {
        private final Handler handler = new Handler(Looper.getMainLooper());
        private final Paint imagePaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        private final Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint streakPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private Bitmap bitmap;
        private boolean visible;
        private int screenW = 1, screenH = 1;
        private long startTime;
        private final Runnable drawTask = new Runnable() {
            @Override public void run() { drawFrame(); if (visible) handler.postDelayed(this, 40); }
        };
        LiveEngine() {
            bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.wallpaper_base);
            startTime = System.currentTimeMillis();
            streakPaint.setStyle(Paint.Style.STROKE);
            streakPaint.setStrokeCap(Paint.Cap.ROUND);
        }
        @Override public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            super.onSurfaceChanged(holder, format, width, height); screenW = width; screenH = height; drawFrame();
        }
        @Override public void onVisibilityChanged(boolean isVisible) {
            visible = isVisible; handler.removeCallbacks(drawTask);
            if (visible) { startTime = System.currentTimeMillis(); handler.post(drawTask); }
        }
        @Override public void onDestroy() {
            visible = false; handler.removeCallbacks(drawTask);
            if (bitmap != null && !bitmap.isRecycled()) bitmap.recycle(); super.onDestroy();
        }
        private void drawFrame() {
            SurfaceHolder holder = getSurfaceHolder(); Canvas c = null;
            try {
                c = holder.lockCanvas(); if (c == null) return;
                if (bitmap == null) { c.drawColor(Color.BLACK); return; }
                float scale = Math.max((float) screenW / bitmap.getWidth(), (float) screenH / bitmap.getHeight());
                float dw = bitmap.getWidth() * scale, dh = bitmap.getHeight() * scale;
                float left = (screenW - dw) / 2f, top = (screenH - dh) / 2f;
                c.drawBitmap(bitmap, null, new RectF(left, top, left + dw, top + dh), imagePaint);
                float cx = left + 161f * scale, cy = top + 481f * scale, radius = 85f * scale;
                float t = (System.currentTimeMillis() - startTime) / 1000f;
                c.save(); Path clip = new Path(); clip.addCircle(cx, cy, radius * 0.96f, Path.Direction.CW); c.clipPath(clip);
                float x1 = cx + (float)Math.sin(t * 0.62f) * radius * 0.48f;
                float y1 = cy + (float)Math.cos(t * 0.43f) * radius * 0.42f;
                glowPaint.setShader(new RadialGradient(x1, y1, radius * 0.82f,
                        new int[]{0x665FEAFF, 0x2254CFFF, 0x0040BFFF}, null, Shader.TileMode.CLAMP));
                c.drawCircle(cx, cy, radius, glowPaint); glowPaint.setShader(null);
                float angle = (t * 24f) % 360f - 90f, rad = (float)Math.toRadians(angle);
                float sx = cx + (float)Math.cos(rad) * radius * 0.72f, sy = cy + (float)Math.sin(rad) * radius * 0.72f;
                streakPaint.setStrokeWidth(Math.max(2f, scale * 2.2f)); streakPaint.setColor(0x99B9F7FF);
                streakPaint.setShadowLayer(9f * scale, 0, 0, 0xAA36BFFF);
                c.drawLine(sx - radius * 0.23f, sy - radius * 0.10f, sx + radius * 0.23f, sy + radius * 0.10f, streakPaint);
                streakPaint.clearShadowLayer(); streakPaint.setStrokeWidth(Math.max(1.5f, scale * 1.2f));
                streakPaint.setColor(0x668BEAFF);
                RectF oval = new RectF(cx - radius * 0.88f, cy - radius * 0.88f, cx + radius * 0.88f, cy + radius * 0.88f);
                c.drawArc(oval, angle - 28f, 48f, false, streakPaint); c.restore();
            } catch (Exception ignored) { } finally { if (c != null) holder.unlockCanvasAndPost(c); }
        }
    }
}
