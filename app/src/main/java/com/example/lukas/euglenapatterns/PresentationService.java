package com.example.lukas.euglenapatterns;

import android.content.Context;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.media.Image;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.google.android.gms.cast.CastPresentation;
import com.google.android.gms.cast.CastRemoteDisplayLocalService;


public class PresentationService extends CastRemoteDisplayLocalService {

    // First screen
    private FirstScreenPresentation mPresentation;
    public static DrawingView mDrawingView = null;

    private static final String TAG = "PresentationService";

    @Override
    public void onCreate() { super.onCreate(); }

    @Override
    public void onCreatePresentation(Display display) { createPresentation(display); }

    @Override
    public void onDismissPresentation() { dismissPresentation(); }

    private void createPresentation(Display display) {
        dismissPresentation();
        mPresentation = new FirstScreenPresentation(this, display);

        try {
            mPresentation.show();
        }
        catch (WindowManager.InvalidDisplayException ex) {
            Log.e(TAG, "Unable to show presentation, display was removed.", ex);
            dismissPresentation();
        }
    }

    private void dismissPresentation() {
        mDrawingView = null;
        if (mPresentation != null) {
            mPresentation.dismiss();
            mPresentation = null;
        }
    }

    public void updatePattern(Drawable draw) {
        mPresentation.updatePattern(draw);
        mDrawingView.eraseCanvas();
    }

    public void movePattern(View view) {
        mPresentation.movePattern(view);
    }

    // temp
    public void updateGIF(int resID, int duration) {
        mPresentation.updateGIF(resID, duration);
        mDrawingView.eraseCanvas();
    }

    /**
     * The presentation to show on the first screen (the TV).
     * <p>
     * Note that this display may have different metrics from the display on
     * which the main activity is showing so we must be careful to use the
     * presentation's own {@link Context} whenever we load resources.
     * </p>
     */
    private static final class FirstScreenPresentation extends CastPresentation {

        // temp
        private AnimationDrawable mAnimation;

        private Context mContext;
        //private int marginTop;
        //private int marginRight;
        private int marginBottom;
        private int marginLeft;
        private int height;
        private int width;

        private static final int INIT_HEIGHT = 225;
        private static final int INIT_WIDTH = 400;

        public FirstScreenPresentation(Context context, Display display) {
            super(context, display);
            mContext = context;
            //marginRight = 0;
            //marginTop = 0;
            marginBottom = 0;
            marginLeft = 0;
            height = INIT_HEIGHT;
            width = INIT_WIDTH;
        }

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.remote_screen);
            createDrawingView();
        }

        public void updatePattern(Drawable draw) {
            LinearLayout pattern = (LinearLayout) findViewById(R.id.pattern);
            pattern.setBackground(draw);
        }

        //temp
        public void updateGIF(int resID, int duration) {
            LinearLayout pattern = (LinearLayout) findViewById(R.id.pattern);

            mAnimation = new AnimationDrawable();
            mAnimation.setOneShot(false);

            if (resID == R.id.gif1) {
                mAnimation.addFrame(getResources().getDrawable(R.drawable.pattern1), duration);
                mAnimation.addFrame(getResources().getDrawable(R.drawable.pattern29), duration);
            } else if (resID == R.id.gif2){
                mAnimation.addFrame(getResources().getDrawable(R.drawable.a1), duration);
                mAnimation.addFrame(getResources().getDrawable(R.drawable.b1), duration);
                mAnimation.addFrame(getResources().getDrawable(R.drawable.c1), duration);
                mAnimation.addFrame(getResources().getDrawable(R.drawable.d1), duration);
                mAnimation.addFrame(getResources().getDrawable(R.drawable.e1), duration);
                mAnimation.addFrame(getResources().getDrawable(R.drawable.f1), duration);
                mAnimation.addFrame(getResources().getDrawable(R.drawable.g1), duration);
                mAnimation.addFrame(getResources().getDrawable(R.drawable.h1), duration);
                mAnimation.addFrame(getResources().getDrawable(R.drawable.i1), duration);
                mAnimation.addFrame(getResources().getDrawable(R.drawable.j1), duration);
                mAnimation.addFrame(getResources().getDrawable(R.drawable.k1), duration);
                mAnimation.addFrame(getResources().getDrawable(R.drawable.l1), duration);
                mAnimation.addFrame(getResources().getDrawable(R.drawable.m1), duration);
                mAnimation.addFrame(getResources().getDrawable(R.drawable.n1), duration);
                mAnimation.addFrame(getResources().getDrawable(R.drawable.o1), duration);
                mAnimation.addFrame(getResources().getDrawable(R.drawable.p1), duration);
                mAnimation.addFrame(getResources().getDrawable(R.drawable.q1), duration);
                mAnimation.addFrame(getResources().getDrawable(R.drawable.r1), duration);
                mAnimation.addFrame(getResources().getDrawable(R.drawable.s1), duration);
                mAnimation.addFrame(getResources().getDrawable(R.drawable.t1), duration);
                mAnimation.addFrame(getResources().getDrawable(R.drawable.u1), duration);
                mAnimation.addFrame(getResources().getDrawable(R.drawable.v1), duration);
                mAnimation.addFrame(getResources().getDrawable(R.drawable.w1), duration);
                mAnimation.addFrame(getResources().getDrawable(R.drawable.x1), duration);
            } else {
                mAnimation.addFrame(getResources().getDrawable(R.drawable.circle1), duration);
                mAnimation.addFrame(getResources().getDrawable(R.drawable.circle2), duration);
                mAnimation.addFrame(getResources().getDrawable(R.drawable.circle3), duration);
                mAnimation.addFrame(getResources().getDrawable(R.drawable.circle4), duration);
                mAnimation.addFrame(getResources().getDrawable(R.drawable.circle5), duration);
                mAnimation.addFrame(getResources().getDrawable(R.drawable.circle6), duration);
                mAnimation.addFrame(getResources().getDrawable(R.drawable.circle7), duration);
                mAnimation.addFrame(getResources().getDrawable(R.drawable.circle8), duration);
                mAnimation.addFrame(getResources().getDrawable(R.drawable.circle9), duration);
                mAnimation.addFrame(getResources().getDrawable(R.drawable.circle10), duration);
                mAnimation.addFrame(getResources().getDrawable(R.drawable.circle11), duration);
                mAnimation.addFrame(getResources().getDrawable(R.drawable.circle12), duration);
                mAnimation.addFrame(getResources().getDrawable(R.drawable.circle13), duration);
                mAnimation.addFrame(getResources().getDrawable(R.drawable.circle14), duration);
                mAnimation.addFrame(getResources().getDrawable(R.drawable.circle15), duration);
                mAnimation.addFrame(getResources().getDrawable(R.drawable.circle16), duration);
                mAnimation.addFrame(getResources().getDrawable(R.drawable.circle17), duration);
                mAnimation.addFrame(getResources().getDrawable(R.drawable.circle18), duration);
            }

            pattern.setBackground(mAnimation);
            mAnimation.start();
        }

        // Creates DrawingView to allow draw events on currently selected pattern
        private void createDrawingView() {
            mDrawingView = new DrawingView(mContext);
            LinearLayout mDrawingPad = (LinearLayout) findViewById(R.id.drawing_pad);
            mDrawingPad.addView(mDrawingView);
        }

        /*public void movePattern(View view) {
            Button direction = (Button) view;
            switch(direction.getText().toString()) {
                case ("UP"):
                    if (marginTop > -150) {
                        marginTop -= 5;
                        setMargins(marginTop, marginRight);
                    }
                    break;
                case("DOWN"):
                    if (marginTop < 150) {
                        marginTop += 5;
                        setMargins(marginTop, marginRight);
                    }
                    break;
                case("LEFT"):
                    if (marginRight < 150) {
                        marginRight += 5;
                        setMargins(marginTop, marginRight);
                    }
                    break;
                case("RIGHT"):
                    if (marginRight > -150) {
                        marginRight -= 5;
                        setMargins(marginTop, marginRight);
                    }
                    break;
                case("+"):
                    if (width + 80 <= INIT_WIDTH * 1.4) {
                        width += 80;
                        height += 45;
                        LinearLayout layout = (LinearLayout)findViewById(R.id.pattern);
                        RelativeLayout.LayoutParams params =
                                (RelativeLayout.LayoutParams) layout.getLayoutParams();
                        params.height = height;
                        params.width = width;
                        layout.requestLayout();

                        MainActivity.mDrawingView.scale += 0.036;
                    }
                    break;
                case("-"):
                    if (width - 80 >= INIT_WIDTH * 0.6) {
                        width -= 80;
                        height -= 45;
                        LinearLayout layout = (LinearLayout)findViewById(R.id.pattern);
                        RelativeLayout.LayoutParams params =
                                (RelativeLayout.LayoutParams) layout.getLayoutParams();
                        params.height = height;
                        params.width = width;
                        layout.requestLayout();

                        MainActivity.mDrawingView.scale -= 0.036;
                    }
                    break;
                default:
                    break;
            }
        }*/

        public void movePattern(View view) {
            Button direction = (Button) view;
            switch(direction.getText().toString()) {
                case ("UP"):
                    if (marginBottom > -150) {
                        marginBottom -= 5;
                        setMargins(marginBottom, marginLeft + 380);
                    }
                    break;
                case("DOWN"):
                    if (marginBottom < 150) {
                        marginBottom += 5;
                        setMargins(marginBottom, marginLeft + 380);
                    }
                    break;
                case("LEFT"):
                    if (marginLeft < 150) {
                        marginLeft += 5;
                        setMargins(marginBottom, marginLeft + 380);
                    }
                    break;
                case("RIGHT"):
                    if (marginLeft > -150) {
                        marginLeft -= 5;
                        setMargins(marginBottom, marginLeft + 380);
                    }
                    break;
                case("+"):
                    if (width + 80 <= INIT_WIDTH * 1.4) {
                        width += 80;
                        height += 45;
                        LinearLayout layout = (LinearLayout)findViewById(R.id.pattern);
                        RelativeLayout.LayoutParams params =
                                (RelativeLayout.LayoutParams) layout.getLayoutParams();
                        params.height = height;
                        params.width = width;
                        layout.requestLayout();

                        MainActivity.mDrawingView.scale += 0.036;
                        if (FreeDrawActivity.mDrawingView != null)
                            FreeDrawActivity.mDrawingView.scale += 0.036;
                    }
                    break;
                case("-"):
                    if (width - 80 >= INIT_WIDTH * 0.6) {
                        width -= 80;
                        height -= 45;
                        LinearLayout layout = (LinearLayout)findViewById(R.id.pattern);
                        RelativeLayout.LayoutParams params =
                                (RelativeLayout.LayoutParams) layout.getLayoutParams();
                        params.height = height;
                        params.width = width;
                        layout.requestLayout();

                        MainActivity.mDrawingView.scale -= 0.036;
                        if (FreeDrawActivity.mDrawingView != null)
                            FreeDrawActivity.mDrawingView.scale -= 0.036;
                    }
                    break;
                default:
                    break;
            }
        }

        /*public  void setMargins (int top, int right) {
            LinearLayout pattern = (LinearLayout) findViewById(R.id.pattern);

            if (pattern.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
                ViewGroup.MarginLayoutParams p = (ViewGroup.MarginLayoutParams) pattern.getLayoutParams();
                p.setMargins(0, top, right, 0);
                pattern.requestLayout();
            }
        }*/

        public  void setMargins (int bottom, int left) {
            LinearLayout pattern = (LinearLayout) findViewById(R.id.pattern);

            if (pattern.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
                ViewGroup.MarginLayoutParams p = (ViewGroup.MarginLayoutParams) pattern.getLayoutParams();
                p.setMargins(left, 0, 0, bottom);
                pattern.requestLayout();
            }
        }
    }
}
