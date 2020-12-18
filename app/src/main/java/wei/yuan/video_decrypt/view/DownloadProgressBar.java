package wei.yuan.video_decrypt.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;

import org.w3c.dom.Attr;

import wei.yuan.video_decrypt.R;


/**
 * 下载进度条
 *
 * @author yugimaster
 */
public class DownloadProgressBar extends View {

    // 绘制背景灰色线条画笔
    private Paint paint = new Paint();
    // 绘制下载进度画笔
    private Paint paintText = new Paint();

    // 获取百分比数字的长宽
    private Rect boundPer = new Rect();

    // 百分比的文字大小
    private int textSize = 25;

    // 下载偏移量
    private float offset = 0f;
    // 距离顶部的偏移量
    private float offsetTop = 18f;
    // 灰色线条距离右边的距离
    private float offsetRight = 0f;
    // 当前下载量
    private float currentValue = 0f;
    // 下载总量
    private float totalValue = 0f;

    // 要显示的百分比值
    private String percentValue = "0%";

    public DownloadProgressBar(Context context) {
        this(context, null);
    }

    public DownloadProgressBar(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DownloadProgressBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        // 自定义属性 给textSize赋初始值
        TypedArray typedArray = getContext().obtainStyledAttributes(attrs,
                R.styleable.downloadProgressBar);
        textSize = (int) typedArray.getDimension(R.styleable.downloadProgressBar_dpTextSize, 36);
        getTextWidth();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // 绘制底色
        paint.setColor(getResources().getColor(R.color.colorLightGrey));
        paint.setStrokeWidth(1);
        canvas.drawLine(0, offsetTop, getWidth(), offsetTop, paint);
        // 绘制进度条颜色
        paint.setColor(getResources().getColor(R.color.colorAccent));
        paint.setStrokeWidth(3);
        canvas.drawLine(0, offsetTop, offset, offsetTop, paint);
        // 绘制白色区域及百分比
        paint.setColor(getResources().getColor(R.color.colorWhite));
        paint.setStrokeWidth(1);
        paintText.setColor(getResources().getColor(R.color.colorAccent));
        paintText.setTextSize(textSize);
        paintText.setAntiAlias(true);
        paintText.getTextBounds(percentValue, 0, percentValue.length(), boundPer);
        canvas.drawLine(offset, offsetTop, offset + boundPer.width() + 4, offsetTop, paint);
        canvas.drawText(percentValue, offset, offsetTop + boundPer.height() / 2 + 2, paintText);
    }

    /**
     * 获取“100%”的宽度
     */
    public void getTextWidth() {
//        Paint paint = new Paint();
        Rect rect = new Rect();
        paint.setTextSize(textSize);
        paint.setAntiAlias(true);
        paint.getTextBounds("100%", 0, "100%".length(), rect);
        offsetRight = rect.width() + 5;
    }

    /**
     * 设置当前进度值
     * @param currentValue
     */
    public void setCurrentValue(float currentValue) {
        this.currentValue = currentValue;
        int percent = (int) (this.currentValue / totalValue * 100);
        if (percent < 100) {
            percentValue = percent + "%";
        } else {
            percentValue = "100%";
        }
        updateCurrentProgressBar();
        invalidate();
    }

    /**
     * 设置总量
     * @param totalValue
     */
    public void setTotalValue(float totalValue) {
        this.totalValue = totalValue;
    }

    /**
     * 更新当前进度条长度
     */
    public void updateCurrentProgressBar() {
        getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (currentValue < totalValue) {
                    offset = (getWidth() - offsetRight) * currentValue / totalValue;
                } else {
                    offset = getWidth() - offsetRight;
                }
            }
        });
    }
}
