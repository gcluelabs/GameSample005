package com.example.gamesample;
 
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.Window;
 
public class GameSample extends Activity {
 
  @Override
	public void onCreate( Bundle savedInstanceState ) {
		requestWindowFeature(Window.FEATURE_NO_TITLE);

		super.onCreate( savedInstanceState );
		// 描画クラスのインスタンスを生成
		MySurfaceView mSurfaceView = new MySurfaceView(this);
		setContentView(mSurfaceView);
	}
}

class MySurfaceView extends SurfaceView implements SurfaceHolder.Callback, SensorEventListener {
	/** 画面サイズ */
	private int w;
	private int h;
	/**　ゲーム画面を描画するためのスレッド */
	private MyTimerTask task;
	/** 自機であるねこに関する情報を保持するクラス */
	private Cat cat;
	/** 敵であるねずみに関する情報を保持するクラス */
	private Rat[] rats = new Rat[5];
	/** スコア */
	private int score = 0;
	/**
	 * SensorManager
	 */
	private SensorManager mSensorManager;

	
	public MySurfaceView(Context context) {
		super(context);
		// イベント取得できるようにFocusを有効にする
		setFocusable( true );
		// SensorManager
		mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);

		// Sensorの取得とリスナーへの登録
		//非推奨だが、実装方法が簡単なのでこちらを使用する
		List< Sensor > sensors = mSensorManager.getSensorList(Sensor.TYPE_ORIENTATION);
		if (sensors.size() > 0) {
			Sensor sensor = sensors.get(0);
			mSensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_FASTEST);
		}

		// Callbackを登録する
		getHolder().addCallback(this);

	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
		//画面サイズの保持
		this.w = width;
		this.h = height;
		// Resourceインスタンスの生成
		Resources res = this.getContext().getResources();
		//　ねこの初期化
		Bitmap catImage = BitmapFactory.decodeResource(res, R.drawable.cat);
		cat = new Cat(catImage, w, h);
		// ねずみの初期化
		Bitmap ratImage = BitmapFactory.decodeResource(res, R.drawable.rat);
		for (int i = 0; i < rats.length; i++) {
			rats[i] = new Rat(ratImage, w, h);
		}
		//画像のメモリ解放
		if (catImage.isRecycled()) {
			catImage.recycle();
		}
		if (ratImage.isRecycled()) {
			ratImage.recycle();
		}
		//描画の開始
		Timer timer = new Timer();
		task = new MyTimerTask();
		timer.schedule(task, 100, 100);
	}


	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		//バックグラウンドの処理が動いている場合は、終了させなければいけない
		if (task != null) {
			task.cancel();
		}
		if (mSensorManager != null) {
			mSensorManager.unregisterListener(this);
		}
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
	}


	/**
	 * タッチイベント
	 */
	public boolean onTouchEvent(MotionEvent event) {
		return true;
	}
	
	/**
	 * ゲーム状態を描画するメソッド
	 */
	private void drawGame() {
		// Canvasを取得する
		Canvas canvas = getHolder().lockCanvas();

		// 背景色を設定する
		canvas.drawColor( Color.BLUE );
 
		// Bitmapイメージの描画
		Paint mPaint = new Paint();
		mPaint.setColor(Color.BLACK);
		//穴の表示
		canvas.drawCircle(w/2, h/2, 30, mPaint);
		//ねずみを動かす
		for (int i = 0; i < rats.length; i++) {
			rats[i].drawMove(canvas);
			if (rats[i].checkHunted(cat)) {
				score++;
			}
		}
		//ねこを動かす
		cat.drawMove(canvas);
		
		//スコアの表示
		mPaint.setTextSize(50);
		mPaint.setColor(Color.WHITE);
		canvas.drawText("スコア:" + score, 100, 50, mPaint);
		// 画面に描画をする
		getHolder().unlockCanvasAndPost(canvas);		
	}
	
	// 定期処理を行うクラス
	private class MyTimerTask extends TimerTask{
	     @Override
	     public void run() {
	    	 drawGame();
	     }
	 }

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
	}

	@Override
	public void onSensorChanged(SensorEvent sensorEvent) {
		if (sensorEvent.sensor.getType() == Sensor.TYPE_ORIENTATION) {
//			Log.i("SURFACE", "yaw:" + sensorEvent.values[0]);
//			Log.i("SURFACE", "picth:" + sensorEvent.values[1]);
//			Log.i("SURFACE", "roll:" + sensorEvent.values[2]);
			
			int myX = (int) (cat.p.x - sensorEvent.values[2]/10);
			int myY = (int) (cat.p.y - sensorEvent.values[1]/10);
			//ねこの移動させる位置を設定
			cat.move(myX, myY);
		}
	}
}

class Cat {
	/** ねこの画像を保持する */
	private Bitmap catImage;
	/** ねこが表示されているx,y座標を保持する */
	public Point p;
	/** 画面サイズを超えることを判定するために保持 */
	private Point disp;
	
	/** コンストラクタ */
	public Cat(Bitmap catImage, int w, int h) {
		this.catImage = catImage;
		p = new Point();
		disp = new Point();
		disp.x = w;
		disp.y = h;
		init(w, h);
	}
	/** ねこの動きを初期化する */
	private void init(int w, int h) {
		//ねこの初期値
		p.x = w / 2;
		p.y = h - catImage.getHeight();
	}
	/** ねこを移動させる位置を表示 */
	public void move(int x, int y) {
		p.x = x;
		p.y = y;
	}
	/** ねこの動きを描画するクラス */
	public void drawMove(Canvas c) {
		c.drawBitmap(catImage, p.x, p.y, new Paint());
	}
}

class Rat {
	/** ねずみの画像を保持する */
	private Bitmap ratImage;
	/** ねずみが表示されているx,y座標を保持する */
	private Point p;
	/** ねずみの移動する方法 */
	private Point move;
	/** ねずみの行動を決める乱数オブジェクト */
	private Random random;
	/** 画面サイズを超えることを判定するために保持 */
	private Point disp;
	
	/** コンストラクタ */
	public Rat(Bitmap ratImage, int w, int h) {
		this.ratImage = ratImage;
		p = new Point();
		move = new Point();
		disp = new Point();
		random = new Random();
		disp.x = w;
		disp.y = h;
		init(w, h);
	}
	/** ねずみの動きを初期化する */
	private void init(int w, int h) {
		//ねずみの初期値
		p.x = w / 2 - ratImage.getWidth() / 2;
		p.y = h / 2 - ratImage.getHeight() / 2;
		int mX = random.nextInt() % 100;
		int mY = random.nextInt() % 100;
		move.x = mX;
		move.y = mY;
	}
	/** ねずみの動きを描画するクラス */
	public void drawMove(Canvas c) {
		p.x += move.x;
		p.y += move.y;
		//画面外に出たらまた穴から表示させるため座標を初期化
		if (p.x < 0 || p.y < 0 || p.x > disp.x || p.y > disp.y) {
			init(disp.x, disp.y);
		}
		c.drawBitmap(ratImage, p.x, p.y, new Paint());
	}
	/** ねこに捕まったかどうかのチェック。捕まったらtrue*/
	public boolean checkHunted(Cat cat) {
		//２点間の距離を計算する
		int centerRatX = p.x + 128 / 2;
		int centerRatY = p.y + 128 / 2;
		int centerCatX = cat.p.x + 128 / 2;
		int centerCatY = cat.p.y + 128 / 2;
		double distance = Math.sqrt(
				Math.pow( (centerRatX - centerCatX), 2 )
					+ Math.pow( (centerRatY - centerCatY), 2 )
				);
		
		double r = 128 / 2;
		if (distance < r) {
			//捕まえられたものとして初期位置に戻す
			init(disp.x, disp.y);
			return true;
		} else {
			return false;
		}
	}
}