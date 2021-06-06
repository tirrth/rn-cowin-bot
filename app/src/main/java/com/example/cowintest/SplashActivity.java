package com.example.cowintest;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.airbnb.lottie.LottieAnimationView;

public class SplashActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        LottieAnimationView lottieAnimationView = findViewById(R.id.splash_animation);
        Integer[] animations = {R.raw.covid_anim_1, R.raw.covid_anim_2, R.raw.covid_anim_3, R.raw.covid_anim_4, R.raw.covid_anim_5};
        final int random_anim_id =  animations[generateRandomInteger(0, animations.length)];
        Log.d("Random Anim Id", "" + random_anim_id);
        lottieAnimationView.setAnimation(random_anim_id);
        getSupportActionBar().hide();
        startActivity(new Intent(getBaseContext(), MainActivity.class));
//        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                startActivity(new Intent(getBaseContext(), MainActivity.class));
//                finish();
//            }
//        }, 3000);

    }

    private int generateRandomInteger(int min, int max){
        return (int)Math.floor(Math.random()*(max - min + 1) + min);
    }
}