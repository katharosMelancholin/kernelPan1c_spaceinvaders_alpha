package com.kernelpan1c.spaceinvander_thegame;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Build;
import android.view.MotionEvent;
import android.view.SurfaceView;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GameView extends SurfaceView implements Runnable {

    private     Thread      thread;
    private     boolean     isPlaying, isGameOver= false;
    private     Background  background1, background2;
    private     int         screenX, screenY, score = 0;
    private     int         sound;
    private     Paint       paint;
    public      static      float       screenRatioX, screenRatioY;
    private     Flight      flight;
    private     Random      random;

    private List<Bullet> bullets;

    private Alien[]     aliens;

    private SharedPreferences sharedPreferences;

    private game_activity activity;

    private SoundPool soundPool;

    public GameView(game_activity activity, int screenX, int screenY) {
        super(activity);

        this.activity = activity;

        sharedPreferences = activity.getSharedPreferences( "game", Context.MODE_PRIVATE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setContentType( AudioAttributes.CONTENT_TYPE_MUSIC )
                    .setUsage( AudioAttributes.USAGE_GAME )
                    .build();

            soundPool = new SoundPool.Builder()
                    .setAudioAttributes( audioAttributes )
                    .build();

        } else
            soundPool = new SoundPool( 1, AudioManager.STREAM_MUSIC, 0);

        sound = soundPool.load( activity, R.raw.blaster, 1);

        this.screenX = screenX;
        this.screenY = screenY;

        screenRatioX = 2340f / screenX;
        screenRatioY = 1080f / screenY;

        paint = new Paint();
        paint.setTextSize(128);
        paint.setColor(Color.parseColor("#f7d56f"));

        flight = new Flight( this, screenY, getResources() );

        bullets = new ArrayList<>();

        aliens = new Alien[4];

        for ( int i = 0; i < 4; i++ ) {

            Alien alien = new Alien( getResources() );
            aliens[i]   = alien;

        }

        background1 = new Background (screenX, screenY, getResources());
        background2 = new Background (screenX, screenY, getResources());

        background2.x = screenX;

        random = new Random();

    }

    @Override
    public void run() {

        while (isPlaying) {

            update  ();
            draw    ();
            sleep   ();

        }

    }
    
    public void resume() {

        isPlaying = true;
        thread = new Thread(this);
        thread.start();

    }

    public void pause() {

        isPlaying = false;
        try {
            thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void update() {

        background1.x -= 20 * screenRatioX;
        background2.x -= 20 * screenRatioX;

        if (background1.x + background1.background.getWidth() < 0) {
            background1.x = screenX;
        }

        if (background2.x + background2.background.getWidth() < 0) {
            background2.x = screenX;
        }

        if ( flight.isGoingUp )
            flight.y -= 30 * screenRatioY;
        else
            flight.y += 30 * screenRatioY;

        if ( flight.y < 0 )
            flight.y = 0;

        if ( flight.y >= screenX - flight.height )
            flight.y = screenY - flight.height;


        List<Bullet> trash = new ArrayList<>();

        for ( Bullet bullet : bullets ) {

            if ( bullet.x > screenX )
                trash.add( bullet );
            bullet.x += 30 * screenRatioX;

            for ( Alien alien : aliens ) {

                if ( Rect.intersects( alien.getCollision(), bullet.getCollision() ) ) {

                    score += 10;
                    alien.x         = -500;
                    bullet.x        = screenX + 500;
                    alien.wasShot   = true;

                }

            }
        }

        for ( Bullet bullet : trash ) {

            bullets.remove( bullet );

        }

        for ( Alien alien : aliens ) {

            alien.x -= alien.speed;

            if ( alien.x + alien.width < 0 ) {

                if ( !alien.wasShot ) {
                    isGameOver = true;
                    return;
                }

                int bound   = (int) (30 * screenRatioX);
                alien.speed = random.nextInt( bound );

                if ( alien.speed < 5 * screenRatioX ) alien.speed = (int) (5 * screenRatioX);

                alien.x = screenX;
                alien.y = random.nextInt( screenY - alien.height );

                alien.wasShot = false;
            }

            if ( Rect.intersects( alien.getCollision(), flight.getCollision() )) {
                isGameOver = true;
            }

        }

    }

    private void draw() {

        if ( getHolder().getSurface().isValid() ) {

            Canvas canvas = getHolder().lockCanvas();
            canvas.drawBitmap( background1.background, background1.x, background1.y, paint );
            canvas.drawBitmap( background2.background, background2.x, background2.y, paint );

            canvas.drawText( score + "", screenX / 2f, 164, paint );


            for ( Alien alien : aliens )
                canvas.drawBitmap(alien.getAlien(), alien.x, alien.y, paint);

            if ( isGameOver ) {
                isPlaying = false;
                canvas.drawBitmap( flight.getDead(), flight.x, flight.y, paint );
                getHolder().unlockCanvasAndPost( canvas );
                safeIfHighScore();
                waitBeforeExit();
                return;
            }

            canvas.drawBitmap( flight.getFlight(), flight.x, flight.y, paint );

            for ( Bullet bullet : bullets )
                canvas.drawBitmap( bullet.bullet, bullet.x, bullet.y, paint );

            getHolder().unlockCanvasAndPost( canvas );
        }

    }

    private void waitBeforeExit() {

        try {
            Thread.sleep(3000);
            activity.startActivity( new Intent(activity, MainActivity.class) );
            activity.finish();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    private void safeIfHighScore() {

        if ( sharedPreferences.getInt( "highscore", 0) < score ) {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putInt( "highscore", score);
            editor.apply();
        }

    }

    private void sleep() {

        try {
            Thread.sleep(17);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        switch ( event.getAction() ) {

            case MotionEvent.ACTION_DOWN:
                if ( event.getX() < screenX / 2 ) {
                    flight.isGoingUp = true;
                }
                break;
            case MotionEvent.ACTION_UP:
                flight.isGoingUp = false;
                if ( event.getX() > screenX / 2 )
                    flight.toShoot++;
                break;

        }
        return true;
    }

    public void create_new_bullet() {

        if (sharedPreferences.getBoolean( "isMute", false )) {
            soundPool.play( sound, 1, 1, 0, 0, 1);
        }

        Bullet bullet = new Bullet( getResources() );
        bullet.x = flight.x + flight.width;
        bullet.y = flight.y + ( flight.height / 2 );

        bullets.add( bullet );
    }
}
