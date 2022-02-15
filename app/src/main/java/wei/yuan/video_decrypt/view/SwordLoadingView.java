package wei.yuan.video_decrypt.view;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Camera;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Xfermode;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.animation.RotateAnimation;

public class SwordLoadingView extends View {

    private final static String TAG = "SwordLoadingView";

    private Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private ValueAnimator animator;

    private Matrix rotateMatrix = new Matrix();

    private Xfermode xfermode = new PorterDuffXfermode(PorterDuff.Mode.DST_OUT);

    private Camera camera = new Camera();

    private float radius = 0f;

    public SwordLoadingView(Context context) {
        this(context, null);
    }

    public SwordLoadingView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SwordLoadingView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        Log.d(TAG, "SwordLoadingView constructor");
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.WHITE);

        // Z 轴是逆时针，取负数，得到顺时针的旋转
        animator = ValueAnimator.ofFloat(0f, -360f);
        animator.setInterpolator(null);
        animator.setRepeatCount(RotateAnimation.INFINITE);
        animator.setDuration(1000);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                invalidate();
            }
        });
    }

    public ValueAnimator getAnimator() {
        return animator;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        Log.d(TAG, "onSizeChanged");
        radius = Math.min(w, h) / 3f;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        Log.d(TAG, "onDraw");
        canvas.drawColor(Color.BLACK);
        drawSword(canvas, 35f, -45f, 0f);
        drawSword(canvas, 50f, 10f, 120f);
        drawSword(canvas, 35f, 55f, 240f);
    }

    private void drawSword(Canvas canvas, float rotateX, float rotateY, float startValue) {
        Log.d(TAG, "drawSword");
        int layerId = canvas.saveLayer(0f, 0f, (float)getWidth(), (float)getHeight(),
                null, Canvas.ALL_SAVE_FLAG);
        rotateMatrix.reset();
        camera.save();
        camera.rotateX(rotateX);
        camera.rotateY(rotateY);
        camera.rotateZ((float)animator.getAnimatedValue() + startValue);
        camera.getMatrix(rotateMatrix);
        camera.restore();

        float halfW = getWidth() / 2f;
        float halfH = getHeight() / 2f;

        rotateMatrix.preTranslate(-halfW, -halfH);
        rotateMatrix.postTranslate(halfW, halfH);
        canvas.concat(rotateMatrix);
        canvas.drawCircle(halfW, halfH, radius, paint);
        paint.setXfermode(xfermode);
        canvas.drawCircle(halfW, halfH - 0.05f * radius, radius * 1.01f, paint);
        canvas.restoreToCount(layerId);
        paint.setXfermode(null);
    }
}
