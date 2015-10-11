package com.example.lukas.euglenapatterns;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;


public class DrawingView extends View {
    private Paint mPaint;
    private Bitmap mBitmap;
    private Canvas mCanvas;
    private Path mPath;
    private float mX, mY;

    public double scale;

    private static final float TOUCH_TOLERANCE = 4;
    private static final float STROKE_WIDTH = 25;

    public DrawingView(Context context) {
        super(context);
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setDither(true);
        mPaint.setColor(Color.BLACK);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeJoin(Paint.Join.ROUND);
        mPaint.setStrokeCap(Paint.Cap.ROUND);
        mPaint.setStrokeWidth(STROKE_WIDTH);

        // Actually 0.2 scaling
        scale = 0.17;

        if (context == App.getContext())
            mPaint.setStrokeWidth(STROKE_WIDTH);
        else
            mPaint.setStrokeWidth((float) (STROKE_WIDTH * scale));

        mPath = new Path();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        mCanvas = new Canvas(mBitmap);
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
        canvas.drawBitmap(mBitmap, 0, 0, mPaint);
        canvas.drawPath(mPath, mPaint);
    }

    public void eraseCanvas() {
        mCanvas.drawColor(0, PorterDuff.Mode.CLEAR);
        invalidate();
    }

    public void changeColor(int resID) {
        switch(resID) {
            case (R.id.black):
                mPaint.setColor(Color.BLACK);
                break;
            case (R.id.white):
                mPaint.setColor(Color.WHITE);
                break;
            case (R.id.blue):
                mPaint.setColor(Color.BLUE);
                break;
            case (R.id.red):
                mPaint.setColor(Color.RED);
                break;
            case (R.id.yellow):
                mPaint.setColor(Color.YELLOW);
                break;
            case (R.id.green):
                mPaint.setColor(Color.GREEN);
                break;
            //case ("Erase"):
                //eraseCanvas();
               // break;
            default:
                break;
        }
    }

    private void touch_start(float x, float y) {
        mPath.reset();
        mPath.moveTo(x, y);
        mX = x;
        mY = y;

        if (PresentationService.mDrawingView != null) {

            if (this != PresentationService.mDrawingView) {
                long downTime = SystemClock.uptimeMillis();
                long eventTime = SystemClock.uptimeMillis();
                float x1 = (float) Math.ceil(x * scale);
                float y1 = (float) Math.ceil(y * scale);
                int metaState = 0;
                MotionEvent motionEvent = MotionEvent.obtain(
                        downTime,
                        eventTime,
                        MotionEvent.ACTION_DOWN,
                        x1,
                        y1,
                        metaState
                );
                PresentationService.mDrawingView.dispatchTouchEvent(motionEvent);
            }
        }
    }

    private void touch_move(float x, float y) {
        float dx = Math.abs(x - mX);
        float dy = Math.abs(y - mY);
        if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
            mPath.quadTo(mX, mY, (x + mX)/2, (y + mY)/2);
            mX = x;
            mY = y;
        }

        if (PresentationService.mDrawingView != null) {

            if (this != PresentationService.mDrawingView) {
                long downTime = SystemClock.uptimeMillis();
                long eventTime = SystemClock.uptimeMillis();
                float x1 = (float) Math.ceil(x * scale);
                float y1 = (float) Math.ceil(y * scale);
                int metaState = 0;
                MotionEvent motionEvent = MotionEvent.obtain(
                        downTime,
                        eventTime,
                        MotionEvent.ACTION_MOVE,
                        x1,
                        y1,
                        metaState
                );
                PresentationService.mDrawingView.dispatchTouchEvent(motionEvent);
            }
        }
    }

    private void touch_up() {
        mPath.lineTo(mX, mY);
        // commit the path to our offscreen
        mCanvas.drawPath(mPath, mPaint);
        //mPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SCREEN));
        // kill this so we don't double draw
        mPath.reset();
        // mPath= new Path();

        if (PresentationService.mDrawingView != null) {
            if (this != PresentationService.mDrawingView) {
                long downTime = SystemClock.uptimeMillis();
                long eventTime = SystemClock.uptimeMillis();
                float x1 = 0.0f;
                float y1 = 0.0f;
                // List of meta states found here: developer.android.com/reference/android/view/KeyEvent.html#getMetaState()
                int metaState = 0;
                MotionEvent motionEvent = MotionEvent.obtain(
                        downTime,
                        eventTime,
                        MotionEvent.ACTION_UP,
                        x1,
                        y1,
                        metaState
                );
                PresentationService.mDrawingView.dispatchTouchEvent(motionEvent);
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                touch_start(x, y);
                invalidate();
                break;
            case MotionEvent.ACTION_MOVE:
                touch_move(x, y);
                invalidate();
                break;
            case MotionEvent.ACTION_UP:
                touch_up();
                invalidate();
                break;
        }
        return true;
    }
}
