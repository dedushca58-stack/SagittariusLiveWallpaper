package com.example.sagittariuswallpaper;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
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
                if (c != null) drawScene(c, c.getWidth(), c.getHeight());
            } finally {
                if (c != null) holder.unlockCanvasAndPost(c);
            }
        }

        private void fill(Canvas c, int color) {
            p.setShader(null);
            p.setStyle(Paint.Style.FILL);
            p.setColor(color);
            p.clearShadowLayer();
            c.drawRect(0, 0, 1080, 2400, p);
        }

        private void drawScene(Canvas c, int w, int h) {
            float sx = w / 1080f, sy = h / 2400f;
            c.save();
            c.scale(sx, sy);

            // Deep petrol-teal leather, with a soft central sheen.
            p.setShader(new LinearGradient(0, 0, 1080, 2400,
                    new int[]{0xff06434a, 0xff07383e, 0xff052b32, 0xff031f28},
                    new float[]{0f, .34f, .72f, 1f}, Shader.TileMode.CLAMP));
            c.drawRect(0, 0, 1080, 2400, p);
            p.setShader(new RadialGradient(430, 620, 1250,
                    new int[]{0x2449b5b4, 0x10218c91, 0x00000000},
                    null, Shader.TileMode.CLAMP));
            c.drawRect(0, 0, 1080, 2400, p);
            p.setShader(null);

            // Fine, irregular leather grain and subtle embossed pores.
            random.setSeed(417);
            p.setStrokeWidth(1.1f);
            for (int i = 0; i < 5200; i++) {
                float x = random.nextFloat() * 1080f;
                float y = random.nextFloat() * 2400f;
                int a = 8 + random.nextInt(17);
                p.setColor((i % 3 == 0) ? (a << 24) | 0x8fe2d5 : (a << 24) | 0x001015);
                float r = .7f + random.nextFloat() * 1.4f;
                c.drawCircle(x, y, r, p);
            }
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(1.4f);
            p.setColor(0x0d9fe1d5);
            for (int i = 0; i < 34; i++) {
                float y = 40 + i * 73;
                c.drawArc(new RectF(-180, y, 1250, y + 150), 190, 155, false, p);
            }
            p.setStyle(Paint.Style.FILL);

            // Floating glass plate with depth, tint and polished edge.
            RectF panel = new RectF(88, 250, 992, 1040);
            p.setShader(new LinearGradient(120, 250, 950, 1040,
                    new int[]{0x3bf4ffff, 0x1e9ce0e5, 0x1745aeb8, 0x26304450},
                    new float[]{0f, .28f, .7f, 1f}, Shader.TileMode.CLAMP));
            p.setShadowLayer(34, 0, 22, 0x88000812);
            c.drawRoundRect(panel, 76, 76, p);
            p.clearShadowLayer();
            p.setShader(null);

            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(5);
            p.setShader(new LinearGradient(90, 250, 990, 1040,
                    new int[]{0xbfffffff, 0x668be7eb, 0x5593dce1, 0x99e9ffff},
                    null, Shader.TileMode.CLAMP));
            c.drawRoundRect(panel, 76, 76, p);
            p.setShader(null);
            p.setStrokeWidth(2);
            p.setColor(0x33ffffff);
            c.drawRoundRect(new RectF(101, 263, 979, 1027), 62, 62, p);
            p.setStyle(Paint.Style.FILL);

            float cx = 540f, cy = 645f;

            // Outer lens: dark bevel, bright glass rim and transparent body.
            p.setShader(new RadialGradient(cx - 72, cy - 105, 330,
                    new int[]{0xffd9ffff, 0xff77c9df, 0xff174c70, 0xff061d37},
                    new float[]{0f, .48f, .82f, 1f}, Shader.TileMode.CLAMP));
            p.setShadowLayer(30, 0, 17, 0xaa00121e);
            c.drawCircle(cx, cy, 264, p);
            p.clearShadowLayer();
            p.setShader(null);

            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(13);
            p.setShader(new LinearGradient(cx - 250, cy - 250, cx + 250, cy + 250,
                    new int[]{0xfff0ffff, 0xff78d4ec, 0xff0a3454, 0xffbcefff},
                    null, Shader.TileMode.CLAMP));
            c.drawCircle(cx, cy, 252, p);
            p.setShader(null);
            p.setStrokeWidth(4);
            p.setColor(0xaaffffff);
            c.drawCircle(cx, cy, 235, p);
            p.setStrokeWidth(2);
            p.setColor(0x5594f1ff);
            c.drawCircle(cx, cy, 222, p);
            p.setStyle(Paint.Style.FILL);

            // Blue convex cabochon, visibly domed rather than a flat disc.
            p.setShader(new RadialGradient(cx - 66, cy - 84, 220,
                    new int[]{0xff68d9ff, 0xff197dc4, 0xff07518e, 0xff032b58},
                    new float[]{0f, .36f, .73f, 1f}, Shader.TileMode.CLAMP));
            c.drawCircle(cx, cy, 184, p);
            p.setShader(null);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(5);
            p.setColor(0x887ceaff);
            c.drawCircle(cx, cy, 180, p);
            p.setStyle(Paint.Style.FILL);

            // All animated light is clipped to the blue cabochon.
            c.save();
            Path lensClip = new Path();
            lensClip.addCircle(cx, cy, 178, Path.Direction.CW);
            c.clipPath(lensClip);
            float sweep = ((phase % 1f) * 620f) - 310f;
            p.setShader(new LinearGradient(cx - 310 + sweep, cy - 220,
                    cx + 80 + sweep, cy + 220,
                    new int[]{0x00ffffff, 0x12ffffff, 0x99ffffff, 0x24ffffff, 0x00ffffff},
                    new float[]{0f, .38f, .5f, .62f, 1f}, Shader.TileMode.CLAMP));
            c.drawRect(cx - 190, cy - 190, cx + 190, cy + 190, p);
            p.setShader(null);
            p.setShader(new RadialGradient(cx - 82, cy - 105, 225,
                    new int[]{0x55ffffff, 0x12b6f5ff, 0x00000000},
                    null, Shader.TileMode.CLAMP));
            c.drawCircle(cx, cy, 180, p);
            p.setShader(null);
            // Narrow moving specular streak.
            c.rotate(-24f, cx, cy);
            p.setShader(new LinearGradient(cx - 230 + sweep, cy - 24,
                    cx + 230 + sweep, cy + 24,
                    new int[]{0x00ffffff, 0x00ffffff, 0x55ffffff, 0x00ffffff},
                    new float[]{0f, .38f, .5f, 1f}, Shader.TileMode.CLAMP));
            c.drawRoundRect(new RectF(cx - 220, cy - 18, cx + 220, cy + 18), 18, 18, p);
            p.setShader(null);
            c.restore();

            // Sagittarius arrow drawn as vector paths so it remains gold on every device.
            c.save();
            c.translate(cx, cy);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeCap(Paint.Cap.ROUND);
            p.setStrokeJoin(Paint.Join.ROUND);
            p.setStrokeWidth(23);
            p.setShader(new LinearGradient(-90, 95, 95, -95,
                    new int[]{0xff8a4a0d, 0xffffe7a0, 0xffd18a25, 0xfffff0b4},
                    null, Shader.TileMode.CLAMP));
            Path arrow = new Path();
            arrow.moveTo(-83, 82);
            arrow.lineTo(77, -78);
            c.drawPath(arrow, p);
            Path head = new Path();
            head.moveTo(4, -78);
            head.lineTo(77, -78);
            head.lineTo(77, -5);
            c.drawPath(head, p);
            Path cross = new Path();
            cross.moveTo(-61, -12);
            cross.lineTo(13, 62);
            c.drawPath(cross, p);
            p.setShader(null);
            p.setStrokeWidth(5);
            p.setColor(0xfffff0c2);
            c.drawPath(arrow, p);
            c.drawPath(head, p);
            c.drawPath(cross, p);
            p.setStyle(Paint.Style.FILL);
            c.restore();

            // Small stationary glass glints on the upper-left rim.
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(5);
            p.setStrokeCap(Paint.Cap.ROUND);
            p.setColor(0x99ffffff);
            c.drawArc(new RectF(cx - 244, cy - 244, cx + 244, cy + 244), 205, 43, false, p);
            p.setStrokeWidth(2);
            p.setColor(0x66b8f6ff);
            c.drawArc(new RectF(cx - 228, cy - 228, cx + 228, cy + 228), 25, 42, false, p);
            p.setStyle(Paint.Style.FILL);

            // Bottom glass strip.
            RectF strip = new RectF(88, 2110, 992, 2260);
            p.setShader(new LinearGradient(0, 2110, 0, 2260,
                    new int[]{0x3effffff, 0x2845b8c5, 0x38304a56, 0x48202d39},
                    null, Shader.TileMode.CLAMP));
            p.setShadowLayer(24, 0, 12, 0x88000000);
            c.drawRoundRect(strip, 54, 54, p);
            p.clearShadowLayer();
            p.setShader(null);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(4);
            p.setColor(0x779ceaf0);
            c.drawRoundRect(strip, 54, 54, p);
            p.setStrokeWidth(2);
            p.setColor(0x44ffffff);
            c.drawLine(145, 2135, 935, 2135, p);
            p.setStyle(Paint.Style.FILL);

            c.restore();
            phase += 0.0045f;
        }
    }
}