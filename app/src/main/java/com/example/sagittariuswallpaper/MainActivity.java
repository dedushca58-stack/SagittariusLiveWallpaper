package com.example.sagittariuswallpaper;

import android.app.Activity;
import android.app.WallpaperManager;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.view.Gravity;
import android.graphics.Color;
import android.graphics.Typeface;

public class MainActivity extends Activity {
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);
        layout.setPadding(32, 32, 32, 32);
        layout.setBackgroundColor(Color.rgb(5, 48, 53));

        TextView title = new TextView(this);
        title.setText("СТРЕЛЕЦ");
        title.setTextColor(Color.WHITE);
        title.setTextSize(28);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        layout.addView(title, new LinearLayout.LayoutParams(-1, -2));

        TextView hint = new TextView(this);
        hint.setText(getString(R.string.hint));
        hint.setTextColor(Color.LTGRAY);
        hint.setTextSize(16);
        hint.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(-1, -2);
        hp.setMargins(0, 20, 0, 28);
        layout.addView(hint, hp);

        Button button = new Button(this);
        button.setText(getString(R.string.choose_wallpaper));
        button.setOnClickListener(v -> {
            Intent intent = new Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER);
            intent.putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                    new ComponentName(this, SagittariusWallpaperService.class));
            startActivity(intent);
        });
        layout.addView(button, new LinearLayout.LayoutParams(-1, -2));
        setContentView(layout);
    }
}
