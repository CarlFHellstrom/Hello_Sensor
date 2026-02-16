package com.example.hello_sensor;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.widget.ImageView;
import android.widget.TextView;
import java.util.Random;

public class GameActivity extends BasicActivity implements SensorEventListener {

    private SensorManager sensorManager;
    private Sensor accelerometer;
    private ImageView imageViewCoin;
    private TextView textViewResult;
    private TextView textViewScore;

    private boolean isFlipping;
    private int headsCount = 0;
    private int tailsCount = 0;
    private Random random;

    private static final float TILT_LOWER_THRESHOLD = 7.0f;
    private static final float TILT_UPPER_THRESHOLD = 10.0f;
    private static final float TILT_UP_THRESHOLD = 6.0f;
    private boolean wasInRange = false;

    private Handler handler;

    private MediaPlayer flipSound;
    private MediaPlayer landSound;
    private Vibrator vibrator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);
        enableBackButton();

        imageViewCoin = findViewById(R.id.imageViewCoin);
        textViewResult = findViewById(R.id.textViewResult);
        textViewScore = findViewById(R.id.textViewScore);

        random = new Random();
        handler = new Handler();

        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }

        imageViewCoin.setImageResource(R.drawable.coin_heads);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        }

        try {
            flipSound = MediaPlayer.create(this, R.raw.coin_flip);
            landSound = MediaPlayer.create(this, R.raw.coin_land);

            if (flipSound != null) {
                flipSound.setVolume(1.0f, 1.0f);
            }
            if (landSound != null) {
                landSound.setVolume(1.0f, 1.0f);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);

        if (flipSound != null) {
            flipSound.release();
            flipSound = null;
        }
        if (landSound != null) {
            landSound.release();
            landSound = null;
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER && !isFlipping) {
            float y = event.values[1];

            boolean inRange = (y >= TILT_LOWER_THRESHOLD && y <= TILT_UPPER_THRESHOLD);

            if (wasInRange && !inRange && y < TILT_UP_THRESHOLD) {
                flipCoin();
            }

            wasInRange = inRange;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    private void flipCoin() {
        isFlipping = true;
        textViewResult.setText("Flipping...");

        if (flipSound != null) {
            try {
                flipSound.start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        animateCoinFlip();

        handler.postDelayed(() -> {
            boolean isHeads = random.nextBoolean();
            showResult(isHeads);
        }, 1500);
    }

    private void animateCoinFlip() {
        final int[] drawables = {R.drawable.coin_heads, R.drawable.coin_tails};
        final int[] index = {0};

        Runnable flipAnimation = new Runnable() {
            int count = 0;
            @Override
            public void run() {
                if (count < 15) {
                    imageViewCoin.setImageResource(drawables[index[0] % 2]);
                    index[0]++;
                    count++;
                    handler.postDelayed(this, 100);
                }
            }
        };
        handler.post(flipAnimation);
    }

    private void showResult(boolean isHeads) {

        if (isHeads) {
            headsCount++;
            textViewResult.setText("Heads!");
            imageViewCoin.setImageResource(R.drawable.coin_heads);
        } else {
            tailsCount++;
            textViewResult.setText("Tails!");
            imageViewCoin.setImageResource(R.drawable.coin_tails);
        }

        textViewScore.setText("Heads: " + headsCount + " | Tails: " + tailsCount);

        if (landSound != null) {
            try {
                landSound.start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (vibrator != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                vibrator.vibrate(500);
            }
        }

        handler.postDelayed(() -> {
            isFlipping = false;
            textViewResult.setText("Tilt to flip again!");
        }, 1000);
    }
}