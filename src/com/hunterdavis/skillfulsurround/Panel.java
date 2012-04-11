package com.hunterdavis.skillfulsurround;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Random;
import java.util.Vector;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.os.Environment;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Toast;

class Panel extends SurfaceView implements SurfaceHolder.Callback {
	private static final float EPS = (float) 0.000001;

	private CanvasThread canvasthread;

	int _x = 0;
	int _y = 0;
	int scoreybuffer = 20;
	int scorexbuffer = 80;
	public Boolean surfaceCreated;
	public Boolean mutex = false;
	public Boolean enableDisableMutex = false;
	public Bitmap backingBitmap = null;
	public Boolean saveScore = false;
	Boolean drawReady = false;
	Boolean generateBoard = true;
	Boolean scoreChanged = false;
	Boolean holdState = false;
	public int score = 0;
	public int lastConnectedPoint = 5;
	int lastX = 0;
	int lastY = 0;
	int filterX = 0;
	int filterY = 0;
	int difficulty = 0;
	int playerColor = Color.rgb(154, 154, 154);
	int edgebuffer = 25;

	private Vector xvalues;
	private Vector yvalues;

	private Vector playerPoints;
	private Vector initialPoints;

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		synchronized (getHolder()) {
			if (holdState == true) {
				return true;
			}
			int action = event.getAction();
			if (action == MotionEvent.ACTION_DOWN) {
				filterX = (int) event.getX();
				filterY = (int) event.getY();
				addValues((int) event.getX(), (int) event.getY());
				return true;
			} else if (action == MotionEvent.ACTION_MOVE) {

				int historySize = event.getHistorySize();
				for (int i = 0; i < historySize - 1; i++) {
					int hisx = (int) event.getHistoricalX(i);
					int hisy = (int) event.getHistoricalY(i);

					if ((filterX == hisx) && (filterY == hisy)) {
						// we should ignore duplicate values
					} else if (fdistance(filterX, filterY, hisx, hisy) < 5) {
						// we should ignore clicks too close
					}
					else {
						filterX = hisx;
						filterY = hisy;
						addValues(hisx, hisy);
					}
				}

				int newx = (int) event.getX();
				int newy = (int) event.getY();
				if ((filterX == newx) && (filterY == newy)) {
					// we should ignore duplicate values
				} else {
					filterX = newx;
					filterY = newy;
					addValues(newx, newy);
				}

				return true;
			} else if (action == MotionEvent.ACTION_UP) {
				endGame();
				return true;
			}

			return true;
		}
	}

	public void setMutex(Boolean mtx) {
		mutex = mtx;
	}

	public void enableKillMutex() {
		enableDisableMutex = true;
	}

	public int getScore() {
		return score;
	}

	public void setDifficulty(int difficult) {
		difficulty = difficult;
		reset();
	}

	public void reset() {
		generateBoard = true;
		saveScore = false;
		score = 0;
	}

	private void addValues(int x, int y) {
		int width = backingBitmap.getWidth();
		int height = backingBitmap.getHeight();

		if ((x > (width - scorexbuffer)) && (y >= (height - scoreybuffer))) {

			Toast.makeText(getContext(),
					"Sorry, touching the score is a game ending move!",
					Toast.LENGTH_SHORT).show();
			endGame();
			return;
		}

		if ((x < 0) || (y < 0)) {
			endGame();
			return;
		}

		if ((x > width - 1) || (y > height - 1)) {
			endGame();
			return;
		}

		if (backingBitmap.getPixel((int) x, (int) y) != Color.WHITE) {
			endGame();
			return;
		}

		xvalues.addElement(x);
		yvalues.addElement(y);
		Point p = new Point(x, y);
		playerPoints.addElement(p);
		drawReady = true;
	}

	float fdistance(float x1, float y1, float x2, float y2) {
		return (float) Math.sqrt((x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1));
	}

	public Panel(Context context, AttributeSet attrs) {
		super(context, attrs);
		//
		surfaceCreated = false;
		xvalues = new Vector();
		yvalues = new Vector();
		playerPoints = new Vector();
		initialPoints = new Vector();
		holdState = false;
		saveScore = false;

		getHolder().addCallback(this);
		setFocusable(true);
	}

	public void createThread(SurfaceHolder holder) {
		canvasthread = new CanvasThread(getHolder(), this);
		canvasthread.setRunning(true);
		canvasthread.start();
	}

	public void terminateThread() {
		canvasthread.setRunning(false);
		try {
			canvasthread.join();
		} catch (InterruptedException e) {

		}
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		if (mutex == false) {
			generateBoard = true;
			saveScore = false;
		} else if (enableDisableMutex == true) {
			mutex = false;
		}

	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		//
		if (surfaceCreated == false) {
			createThread(holder);
			// Bitmap kangoo = BitmapFactory.decodeResource(getResources(),
			// R.drawable.kangoo);
			surfaceCreated = true;
		}
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		surfaceCreated = false;

	}

	@Override
	public void onDraw(Canvas canvas) {

		Paint paint = new Paint();

		if (saveScore) {
			holdState = true;
			generateBoard = false;
			drawReady = false;

			int width = canvas.getWidth();
			int height = canvas.getHeight();

			paint.setColor(Color.BLACK);
			canvas.drawText("Game Over", (width / 2) - 30, height / 2, paint);

			// clear out the scoreboard to all white
			paint.setColor(Color.WHITE);
			canvas.drawRect(width - scorexbuffer, height - scoreybuffer, width,
					height, paint);

			// paint the scoreboard with the score
			paint.setColor(Color.BLACK);
			canvas.drawText(String.valueOf(score) + " points", width
					- scorexbuffer, height - 4, paint);

			return;
		}

		if (generateBoard == true) {

			backingBitmap = Bitmap.createBitmap(canvas.getWidth(),
					canvas.getHeight(), Bitmap.Config.ARGB_8888);

			canvas.setBitmap(backingBitmap);
			canvas.drawColor(Color.WHITE);

			// clear score
			score = 0;
			lastX = 0;
			lastY = 0;

			// update score view
			scoreChanged = true;
			holdState = false;

			// clear all x and y values
			xvalues.clear();
			yvalues.clear();
			playerPoints.clear();
			initialPoints.clear();

			// add initial random dots to board
			generateInitialLines(canvas);

			generateBoard = false;
		}

		// canvas.drawBitmap(kangoo, 10, 10, null);
		else if (drawReady == true) {

			canvas.setBitmap(backingBitmap);

			int numItems = xvalues.size();
			int width = canvas.getWidth();
			int height = canvas.getHeight();
			Random myrandom = new Random();

			// draw then connect all points that have yet to be connected
			for (int i = 0; i < numItems; i++) {

				// retrieve the point, this is the one to draw
				int newx = (Integer) xvalues.get(i);
				int newy = (Integer) yvalues.get(i);

				paint.setColor(playerColor);

				// draw the new point, or connect line to last one
				if ((lastX == 0) && (lastY == 0)) {
					canvas.drawPoint(newx, newy, paint);
					score += 100;
				} else {
					calcualteanddrawline(canvas, lastX, lastY, newx, newy,
							paint, false);
					// canvas.drawLine(lastX, lastY, newx, newy, paint);
				}
				lastX = newx;
				lastY = newy;

				// canvas.drawRect(newx, newy + 2, newx + 2, newy, paint);
				scoreChanged = true;

			}
			xvalues.clear();
			yvalues.clear();

			drawReady = false;
		}

		if (scoreChanged == true) {
			int width = canvas.getWidth();
			int height = canvas.getHeight();

			// clear out the scoreboard to all white
			paint.setColor(Color.WHITE);
			canvas.drawRect(width - scorexbuffer, height - scoreybuffer, width,
					height, paint);

			// paint the scoreboard with the score
			paint.setColor(Color.BLACK);
			canvas.drawText(String.valueOf(score) + " points", width
					- scorexbuffer, height - 4, paint);
		}

		// update screen
		canvas.drawBitmap(backingBitmap, 0, 0, paint);

		// here we call a canvas operation function to add all the cats
		// drawCatsFromVectors(singleUseCanvas);

		// since we drew to the bitmap, display it
		// canvas.drawBitmap(lastGoodBitmap, 0, 0, null);

	}

	public void calculateFinalScore() {

		// loop first height
		// then width
		// for each white pixel which is between our color
		// score it!
		int playerColorFound = 0;
		int localpixel = 0;
		int tempScore = 0;
		int height = backingBitmap.getHeight();

		int numberInitialPoints = initialPoints.size() - 1;
		int numberPlayerPoints = playerPoints.size() - 1;
		int foundPlayers[] = new int[height];
		int foundInitials[] = new int[height];
		int scoreWidth[] = new int[height];

		for (int i = 0; i < height; i++) {
			foundPlayers[i] = 0;
		}

		for (int i = 0; i < height; i++) {
			scoreWidth[i] = 0;
		}
		for (int i = 0; i < height; i++) {
			foundInitials[i] = 0;
		}
		for (int i = 0; i < numberPlayerPoints; i++) {
			Point p = (Point) playerPoints.get(i);
			foundPlayers[p.y]++;
			if (foundPlayers[p.y] % 2 == 0) {
				scoreWidth[p.y] = p.x - scoreWidth[p.y];
			} else if (foundPlayers[p.y] == 1) {
				scoreWidth[p.y] = p.x;
			}
		}

		for (int i = 0; i < numberInitialPoints; i++) {
			Point p = (Point) initialPoints.get(i);
			foundInitials[p.y]++;
		}

		for (int i = 0; i < height; i++) {
			if (foundPlayers[i] > 1) {
				tempScore += scoreWidth[i] / foundPlayers[i];
			}
		}

		score -= tempScore;
	}

	// we manually calculate all pixels in the short lines
	// so that we can test the backing bitmap for
	// the presense of non-background pixels

	public void calcualteanddrawline(Canvas canvas, int xa, int ya, int xb,
			int yb, Paint paint, Boolean initialLines) {
		int x0 = xa;
		int y0 = ya;
		int x1 = xb;
		int y1 = yb;
		int dx = Math.abs(x1 - x0);
		int dy = Math.abs(y1 - y0);
		int sx = -1;
		if (x0 < x1) {
			sx = 1;
		}
		int sy = -1;
		if (y0 < y1) {
			sy = 1;
		}
		;
		int err = dx - dy;

		Boolean running = true;
		while (running) {

			// test for collision and end game if so
			if ((x0 == xa) && (y0 == ya)) {
				// first pass, do nothing?
			} else {
				if (initialLines == false) {
					if (backingBitmap.getPixel(x0, y0) != Color.WHITE) {
						endGame();
						return;
					}
				}
			}
			// draw new point
			canvas.drawPoint(x0, y0, paint);

			Point p = new Point(x0, y0);
			if (initialLines == true) {
				initialPoints.add(p);
			} else {
				playerPoints.add(p);
				score += 100;
			}

			// iterate till finished
			if ((x0 == x1) && (y0 == y1)) {
				running = false;
			} else {
				int e2 = 2 * err;
				if (e2 > -dy) {
					err = err - dy;
					x0 = x0 + sx;
				}
				if (e2 < dx) {
					err = err + dx;
					y0 = y0 + sy;
				}
			}

		}
	}

	private void endGame() {

		calculateFinalScore();
		saveScore = true;
	}

	private void generateInitialLines(Canvas canvas) {
		Paint paint = new Paint();

		Random myrandom = new Random();
		int startx = 0;
		int starty = 0;
		int stopx = 0;
		int stopy = 0;
		int width = canvas.getWidth();
		int height = canvas.getHeight();

		int numberOfTotalInitiaLines = 5 + difficulty;

		for (int i = 0; i < numberOfTotalInitiaLines; i++) {
			startx = myrandom.nextInt((width - 2*edgebuffer)) + edgebuffer;
			starty = myrandom.nextInt((height - 2*edgebuffer)) + edgebuffer;
			stopx = myrandom.nextInt((width - 2*edgebuffer)) + edgebuffer;
			stopy = myrandom.nextInt((width - 2*edgebuffer)) + edgebuffer;

			if ((startx > (width - scorexbuffer))
					&& (starty >= (height - scoreybuffer))) {
				startx = width - scorexbuffer - myrandom.nextInt(5);
				starty = height - scoreybuffer - myrandom.nextInt(5);
			}

			if ((stopx > (width - scorexbuffer))
					&& (stopy >= (height - scoreybuffer))) {
				stopx = width - scorexbuffer - myrandom.nextInt(5);
				stopy = height - scoreybuffer - myrandom.nextInt(5);
			}

			int red = myrandom.nextInt(255);
			int green = myrandom.nextInt(255);
			int blue = myrandom.nextInt(255);

			int ourColor = Color.rgb(red, green, blue);
			if (ourColor == playerColor) {
				ourColor++;
			}

			paint.setColor(ourColor);
			calcualteanddrawline(canvas, startx, starty, stopx, stopy, paint,
					true);
			// canvas.drawRect(newx, newy + 2, newx + 2, newy, paint);
			// localcanvas.drawPoint(newx, newy, paint);
			// xvalues.add(newx);
			// yvalues.add(newy);
		}

	}

	public Boolean saveImage(Context context, View v) {

		// terminate the running thread and join the data
		terminateThread();

		// now save out the file holmes!
		OutputStream outStream = null;
		String newFileName = "Skillful-Surround-Score-" + score + ".png";
		String extStorageDirectory = Environment.getExternalStorageDirectory()
				.toString();

		if (newFileName != null) {
			File file = new File(extStorageDirectory, newFileName);
			try {
				outStream = new FileOutputStream(file);

				// here we save out our last known good bitmap
				// Panel mypanel = (Panel) findViewById(R.id.SurfaceView01);

				// int left = getLeft();
				// int right = getRight();
				// int top = getTop();
				// int bottom = getBottom();

				// mypanel.setDrawingCacheEnabled(true);
				// mypanel.onLayout(false, left, top, right, bottom);
				// lastGoodBitmap = Bitmap.createBitmap( getWidth(),
				// getHeight(), Bitmap.Config.ARGB_8888);
				// Canvas mycanv = new Canvas(lastGoodBitmap);
				// View view = (View) findViewById(R.id.SurfaceView01);
				// view.draw(mycanv);
				// setDrawingCacheEnabled(true);
				// onLayout(true, left, top, right, bottom);

				// draw(mycanv);
				// lastGoodBitmap = getDrawingCache();
				// setDrawingCacheEnabled(false);

				backingBitmap.compress(Bitmap.CompressFormat.PNG, 100,
						outStream);

				try {
					outStream.flush();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();

					createThread(getHolder());
					return false;
				}
				try {
					outStream.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();

					createThread(getHolder());
					return false;
				}

				Toast.makeText(context, "Saved " + newFileName,
						Toast.LENGTH_LONG).show();
				new SingleMediaScanner(context, file);

			} catch (FileNotFoundException e) {
				// do something if errors out?

				createThread(getHolder());
				return false;
			}
		}

		createThread(getHolder());
		return true;

	}

} // end class