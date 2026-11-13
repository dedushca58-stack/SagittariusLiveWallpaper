package com.example.sagittariuswallpaper;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.service.wallpaper.WallpaperService;
import android.view.SurfaceHolder;
import android.os.Handler;
import android.os.Looper;
import java.util.Random;

public class SagittariusWallpaperService extends WallpaperService {
    @Override public Engine onCreateEngine() { return new WallpaperEngine(); }

    private class WallpaperEngine extends Engine {
        private final Handler handler = new Handler(Looper.getMainLooper());
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Random random = new Random(417);
        private boolean visible;
        private float phase = 0f;
        private final Runnable drawTask = new Runnable() {
            @Override public void run() {
                drawFrame();
                if (visible) handler.postDelayed(this, 33);
            }
        };

        @Override public void onVisibilityChanged(boolean v) {
            visible = v;
            handler.removeCallbacks(drawTask);
            if (visible) drawTask.run();
        }
        @Override public void onSurfaceChanged(SurfaceHolder h, int f, int w, int ht) {
            super.onSurfaceChanged(h, f, w, ht);
            drawFrame();
        }
        @Override public void onSurfaceDestroyed(SurfaceHolder h) {
            visible = false;
            handler.removeCallbacks(drawTask);
            super.onSurfaceDestroyed(h);
        }
        @Override public void onDestroy() {
            visible = false;
            handler.removeCallbacks(drawTask);
            super.onDestroy();
        }

        private void drawFrame() {
            SurfaceHolder holder = getSurfaceHolder();
            Canvas c = null;
            try {
                c = holder.lockCanvas();
                if (c == null) return;
                drawScene(c, c.getWidth(), c.getHeight());
            } finally {
                if (c != null) holder.unlockCanvasAndPost(c);
            }
        }

        private void drawScene(Canvas c, int w, int h) {
            float sx = w / 1080f, sy = h / 2400f;
            c.save();
            c.scale(sx, sy);
            p.setShader(new LinearGradient(0, 0, 1080, 2400,
                    new int[]{0xff073e43, 0xff062f35, 0xff041f27},
                    null, Shader.TileMode.CLAMP));
            c.drawColor(Color.BLACK);
            c.drawRect(0, 0, 1080, 2400, p);
            p.setShader(null);

            // Fine leather grain, fixed in place.
            random.setSeed(417);
            p.setColor(0x1affd9c0);
            p.setStrokeWidth(1.2f);
            for (int i = 0; i < 1700; i++) {
                float x = random.nextFloat() * 1080f;
                float y = random.nextFloat() * 2400f;
                float r = 0.6f + random.nextFloat() * 1.5f;
                c.drawPoint(x, y, p);
            }

            // Upper glass panel.
            RectF panel = new RectF(92, 250, 988, 1040);
            p.setShader(new LinearGradient(0, 250, 0, 1040,
                    new int[]{0x55ffffff, 0x1827dce8, 0x3310a8ba, 0x44102029},
                    null, Shader.TileMode.CLAMP));
            c.drawRoundRect(panel, 78, 78, p);
            p.setShader(null);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(4);
            p.setColor(0x66d8ffff);
            c.drawRoundRect(panel, 78, 78, p);
            p.setStyle(Paint.Style.FILL);

            // Medallion and its blue glass depth.
            float cx = 540, cy = 635, radius = 258;
            p.setShader(new LinearGradient(cx-radius, cy-radius, cx+radius, cy+radius,
                    new int[]{0xffa7f4ff, 0xff176a9c, 0xff062c57, 0xff54bfe8},
                    new float[]{0f, .35f, .72f, 1f}, Shader.TileMode.CLAMP));
            c.drawCircle(cx, cy, radius, p);
            p.setShader(null);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(13);
            p.setColor(0x99d9f8ff);
            c.drawCircle(cx, cy, radius-7, p);
            p.setStrokeWidth(3);
            p.setColor(0x77ffffff);
            c.drawCircle(cx, cy, radius-25, p);
            p.setStyle(Paint.Style.FILL);

            // Moving light is clipped strictly to the medallion.
            c.save();
            Path clip = new Path();
            clip.addCircle(cx, cy, radius-18, Path.Direction.CW);
            c.clipPath(clip);
            c.rotate((float)(Math.sin(phase) * 16f), cx, cy);
            float shift = (float)((phase % 1.0f) * 760f) - 380f;
            p.setShader(new LinearGradient(cx-440+shift, cy-350, cx+80+shift, cy+350,
                    new int[]{0x00ffffff, 0x22ffffff, 0xaaffffff, 0x18ffffff, 0x00ffffff},
                    new float[]{0f, .35f, .5f, .65f, 1f}, Shader.TileMode.CLAMP));
            c.drawRect(cx-radius, cy-radius, cx+radius, cy+radius, p);
            p.setShader(null);
            p.setColor(0x35a9efff);
            c.drawOval(new RectF(cx-210, cy-180, cx+210, cy+170), p);
            c.restore();

            // Sagittarius glyph.
            p.setColor(0xffffd987);
            p.setTextAlign(Paint.Align.CENTER);
            p.setTypeface(Typeface.create("serif", Typeface.BOLD));
            p.setTextSize(260);
            p.setShadowLayer(20, 0, 5, 0xffffc45d);
            c.drawText("♐", cx, cy+88, p);
            p.clearShadowLayer();

            // Lower matching glass strip.
            RectF strip = new RectF(92, 2110, 988, 2250);
            p.setShader(new LinearGradient(0, 2110, 0, 2250,
                    new int[]{0x221ffffff, 0x4435d9e8, 0x33102029},
                    null, Shader.TileMode.CLAMP));
            c.drawRoundRect(strip, 48, 48, p);
            p.setShader(null);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(3);
            p.setColor(0x55d8ffff);
            c.drawRoundRect(strip, 48, 48, p);
            p.setStyle(Paint.Style.FILL);
            c.restore();
            phase += 0.006f;
        }
    }
}
