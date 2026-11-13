package com.example.sagittariuswallpaper;

import android.app.Activity;
import android.app.WallpaperManager;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {
    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(48, 64, 48, 48);
        layout.setBackgroundColor(0xFF071D20);
        TextView title = new TextView(this);
        title.setText("Стрелец — живые обои");
        title.setTextColor(0xFFFFFFFF);
        title.setTextSize(24);
        TextView description = new TextView(this);
        description.setText("Исходный дизайн сохранён. Внутри синего медальона движутся мягкие световые блики и отражения.");
        description.setTextColor(0xFFD4E8EA);
        description.setTextSize(16);
        description.setPadding(0, 24, 0, 32);
        Button apply = new Button(this);
        apply.setText("Предпросмотр и установка обоев");
        apply.setOnClickListener(v -> {
            Intent intent = new Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER);
            intent.putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                    new ComponentName(this, SagittariusWallpaperService.class));
            startActivity(intent);
        });
        layout.addView(title);
        layout.addView(description);
        layout.addView(apply);
        setContentView(layout);
    }
}
