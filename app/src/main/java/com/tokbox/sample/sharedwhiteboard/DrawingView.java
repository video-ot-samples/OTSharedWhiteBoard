package com.tokbox.sample.sharedwhiteboard;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

    public class DrawingView extends View {

        public int width;
        public  int height;
        private Bitmap mBitmap;
        private Canvas mCanvas;
        private Path mPath;
        private Paint   mBitmapPaint;
        Context context;
        private Paint circlePaint;
        private Path circlePath;
        private Paint mPaint = new Paint();

        private MainActivity activity;
        int defaultColor = Color.GREEN;



        public DrawingView(Context c, AttributeSet attrs) {
            super(c, attrs);
            context=c;

            activity = (MainActivity)c;
            Log.d("DrawingView", "DrawingView: " + (c instanceof MainActivity));

            mPath = new Path();
            mPaint.setAntiAlias(true);
            mPaint.setDither(true);
            mPaint.setColor(defaultColor);
            mPaint.setStyle(Paint.Style.STROKE);
            mPaint.setStrokeJoin(Paint.Join.ROUND);
            mPaint.setStrokeCap(Paint.Cap.ROUND);
            mPaint.setStrokeWidth(12);
            mBitmapPaint = new Paint(Paint.DITHER_FLAG);
            circlePaint = new Paint();
            circlePath = new Path();
            circlePaint.setAntiAlias(true);
            circlePaint.setColor(Color.BLUE);
            circlePaint.setStyle(Paint.Style.STROKE);
            circlePaint.setStrokeJoin(Paint.Join.MITER);
            circlePaint.setStrokeWidth(4f);
        }

        @Override
        protected void onSizeChanged(int w, int h, int oldw, int oldh) {
            super.onSizeChanged(w, h, oldw, oldh);

            mBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            mCanvas = new Canvas(mBitmap);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);

            canvas.drawBitmap( mBitmap, 0, 0, mBitmapPaint);
            canvas.drawPath( mPath,  mPaint);
            canvas.drawPath( circlePath,  circlePaint);
        }

        private float mX, mY;
        private static final float TOUCH_TOLERANCE = 4;

        public void touch_start(String remote, float x, float y, int color) {
            mPaint.setColor(color);
            mPath.reset();
            mPath.moveTo(x, y);
            mX = x;
            mY = y;

            if(remote.equals("local"))
            activity.sendSignal("start", x, y, defaultColor);
        }

        public void touch_move(String remote, float x, float y, int color) {
            float dx = Math.abs(x - mX);
            float dy = Math.abs(y - mY);
            mPaint.setColor(color);

            if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
                mPath.quadTo(mX, mY, x, y);
                mX = x;
                mY = y;

                circlePath.reset();
                circlePath.addCircle(mX, mY, 30, Path.Direction.CW);

                if(remote.equals("local"))
                activity.sendSignal("move", x, y, defaultColor);
            }
        }

        public void touch_up(String remote) {
            mPath.lineTo(mX, mY);
            circlePath.reset();
            // commit the path to our offscreen
            mCanvas.drawPath(mPath,  mPaint);
            // kill this so we don't double draw
            mPath.reset();

            if(remote.equals("local"))
            activity.sendSignal("up", 0, 0, mPaint.getColor());
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            float x = event.getX();
            float y = event.getY();

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    touch_start("local", x, y, defaultColor);
                    invalidate();
                    break;
                case MotionEvent.ACTION_MOVE:
                    touch_move("local", x, y, defaultColor);
                    invalidate();
                    break;
                case MotionEvent.ACTION_UP:
                    touch_up("local");
                    invalidate();
                    break;
            }
            return true;
        }

        public void setColor(int color){
            this.defaultColor = color;
        }

}