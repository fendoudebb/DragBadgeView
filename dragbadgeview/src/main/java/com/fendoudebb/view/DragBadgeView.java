package com.fendoudebb.view;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.FloatEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.drawable.AnimationDrawable;
import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.ScrollingView;
import android.support.v4.view.ViewPager;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.animation.OvershootInterpolator;
import android.widget.AbsListView;
import android.widget.ImageView;
import android.widget.ScrollView;

/**
 * zbj on 27-07-17 17:44.
 */

public class DragBadgeView extends View {
    private static final String TAG = "DragBadgeView";
    private static final String VIEW_TAG = "BadgeView_TAG";

    private String  mText;
    private float   mMaxMoveRange;
    private float   mTextWidth;
    private float   mTextHeight;
    private float   mFontMetricsTop;
    private float   mFontMetricsBottom;
    private boolean mDragEnable;
    private boolean isDragging;
    private int[] mRootViewLocation = new int[2];

    private Paint     mPaint;
    private TextPaint mTextPaint;
    private RectF     mTextRectF;
    private BadgeView mBadgeView;
    private ViewGroup mScrollParent;

    private OnDragBadgeViewListener mListener;

    public interface OnDragBadgeViewListener {
        void onDisappear(String text);
    }

    public void setOnDragBadgeViewListener(OnDragBadgeViewListener listener) {
        mListener = listener;
    }

    public DragBadgeView(Context context) {
        this(context, null);
    }

    public DragBadgeView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DragBadgeView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs);
    }

    //初始化
    private void init(AttributeSet attrs) {
        TypedArray array = getContext().obtainStyledAttributes(attrs, R.styleable.DragBadgeView);
        mText = array.getString(R.styleable.DragBadgeView_text);
        float textSize = array.getDimension(R.styleable.DragBadgeView_textSize, sp2px(10));
        int bgColor = array.getColor(R.styleable.DragBadgeView_bgColor, Color.RED);
        int textColor = array.getColor(R.styleable.DragBadgeView_textColor, Color.WHITE);
        mMaxMoveRange = array.getDimension(R.styleable.DragBadgeView_maxMoveRange, dp2px(80));
        mDragEnable = array.getBoolean(R.styleable.DragBadgeView_dragEnable, true);
        array.recycle();

        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setColor(bgColor);

        mTextPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        mTextPaint.setAntiAlias(true);
        mTextPaint.setTextAlign(Paint.Align.CENTER);
        mTextPaint.setTextSize(textSize);
        mTextPaint.setColor(textColor);

        if (mText == null) {
            mText = "-1";
        }

        measureText(mText);

        //绘制文字及背景需要的RectF
        mTextRectF = new RectF();
    }

    /**
     * 测量文字的宽高
     *
     * @param text 需要被测量的文字
     */
    private void measureText(String text) {
        mTextWidth = mTextPaint.measureText(text) + getPaddingLeft() + getPaddingRight();
        Paint.FontMetrics fontMetrics = mTextPaint.getFontMetrics();
        mFontMetricsTop = fontMetrics.top;
        mFontMetricsBottom = fontMetrics.bottom;
        mTextHeight = Math.abs(mFontMetricsTop - mFontMetricsBottom) + getPaddingTop() +
                getPaddingBottom();
        Log.d(TAG, "measureText: mTextWidth: " + mTextWidth
                + ",mTextHeight: " + mTextHeight + ",mText: " + mText);
    }

    /**
     * 设置需要展示的文字
     *
     * @param text 需要展示的文字
     */
    public void setText(String text) {
        mText = text;
        Log.d(TAG, "setText: " + text);

        measureText(mText);
        //请求重新布局,会先调用onMeasure
        requestLayout();
        postInvalidate();

        if (isDragging && mBadgeView != null) {
            updateCacheBitmap();
            mBadgeView.postInvalidate();
        }
    }

    /**
     * 设置控件显示颜色
     *
     * @param color 颜色值
     */
    public void setBgColor(int color) {
        mPaint.setColor(color);
    }

    /**
     * 设置文字大小
     *
     * @param textSize 文字大小必须 大于 0 ,注意sp2px转换
     */
    public void setTextSize(float textSize) {
        if (textSize > 0) {
            mTextPaint.setTextSize(textSize);
        }
    }

    /**
     * 设置能否拖拽
     *
     * @param enable true:能拖拽 false:反之
     */
    public void setDragEnable(boolean enable) {
        mDragEnable = enable;
    }

    /**
     * 获取TextView的缓存bitmap
     */
    private void updateCacheBitmap() {
        mBadgeView.recycleCacheBitmap();
        setDrawingCacheEnabled(true);
        Bitmap drawingCache = getDrawingCache();
        mBadgeView.cacheBitmap = Bitmap.createBitmap(drawingCache);
        setDrawingCacheEnabled(false);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = measureDimension((int) Math.max(mTextWidth, mTextHeight), widthMeasureSpec);
        int height = measureDimension((int) mTextHeight, heightMeasureSpec);
        setMeasuredDimension(width, height);
    }

    private int measureDimension(int defaultSize, int measureSpec) {
        int result;
        int specMode = MeasureSpec.getMode(measureSpec);
        int specSize = MeasureSpec.getSize(measureSpec);
        if (specMode == MeasureSpec.EXACTLY) {//相当于我们设置为match_parent或者为一个具体的值
            result = specSize;
        } else if (specMode == MeasureSpec.AT_MOST) {//相当于我们设置为wrap_content
            result = Math.min(defaultSize, specSize);
        } else {
            result = defaultSize;
        }
        return result;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float tempWidth = getWidth();
        if (getWidth() < getHeight()) {
            tempWidth = getHeight();
        }
        mTextRectF.set(0, 0, tempWidth, getHeight());

        canvas.drawRoundRect(mTextRectF, getHeight() / 2, getHeight() / 2, mPaint);

        //居中drawText
        int centerY = (int) (mTextRectF.centerY() - mFontMetricsTop / 2 - mFontMetricsBottom / 2);

        String temp = mText;
        if (TextUtils.isDigitsOnly(mText)) {
            if (Integer.valueOf(mText) > 99) {
                temp = "99+";
            }
        }
        canvas.drawText(temp, mTextRectF.centerX(), centerY, mTextPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (MotionEventCompat.getActionMasked(event)) {
            case MotionEvent.ACTION_DOWN:
                if (!mDragEnable) {//设置了不可拖动属性
                    return false;
                }
                View root = getRootView();
                //判断DecorView是否为空,是否是ViewGroup
                if (root == null || !(root instanceof ViewGroup)) {
                    return false;
                }
                ViewGroup vg = (ViewGroup) root;
                //找出添加Tag的BadgeView,ListView/RecyclerView多条目时会两个Item同事Action_Down事件
                View badgeView = vg.findViewWithTag(VIEW_TAG);
                if (badgeView != null) {
                    return false;
                }
                root.getLocationOnScreen(mRootViewLocation);
                mScrollParent = getScrollParent(this);
                if (mScrollParent != null) {
                    mScrollParent.requestDisallowInterceptTouchEvent(true);//请求父容器不拦截DOWN事件
                }
                int location[] = new int[2];
                getLocationOnScreen(location);

                int downX = location[0] + (getWidth() / 2) - mRootViewLocation[0];
                int downY = location[1] + (getHeight() / 2) - mRootViewLocation[1];
                int radius = (getHeight()) / 2;

                mBadgeView = new BadgeView(getContext());
//                mBadgeView.setLayoutParams(new ViewGroup.LayoutParams(root.getWidth(),
//                        root.getHeight()));
                if (mBadgeView.isResetAnimatorRunning()) {
                    return false;
                }
                updateCacheBitmap();
                mBadgeView.initPoints(downX, downY, event.getRawX() - mRootViewLocation[0],
                        event.getRawY() - mRootViewLocation[1], radius);
                mBadgeView.setTag(VIEW_TAG);//给BadgeView设置Tag
                View cacheView = vg.findViewWithTag(VIEW_TAG);//如果有之前的BadgeView,清除
                if (cacheView != null) {
                    vg.removeView(cacheView);
                }
                vg.addView(mBadgeView);

                setVisibility(View.INVISIBLE);//设置标记View隐藏
                isDragging = true;
                break;
            case MotionEvent.ACTION_MOVE:
                mBadgeView.updateView(event.getRawX() - mRootViewLocation[0],
                        event.getRawY() - mRootViewLocation[1]);
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_DOWN://多个手指按下时
            case MotionEvent.ACTION_CANCEL:
                //BadgeView移动到屏幕左右两边边缘时点击ListView/RecyclerView条目时触发
                isDragging = false;
                if (mScrollParent != null) {
                    mScrollParent.requestDisallowInterceptTouchEvent(false);
                }
                if (mBadgeView == null) {
                    return true;
                }
                if (mBadgeView.isOutOfRange) {
                    mBadgeView.disappear(event.getRawX() - mRootViewLocation[0],
                            event.getRawY() - mRootViewLocation[1]);
                } else if (!mBadgeView.isResetAnimatorRunning()){
                    mBadgeView.reset();
                }
                break;
            default:
                break;
        }
        return true;
    }

    /**
     * 递归获取父布局中是否含可滑动的ViewGroup
     *
     * @param v 子View
     * @return null或者可滑动的ViewGroup
     */
    private ViewGroup getScrollParent(View v) {
        ViewParent viewParent = v.getParent();
        View parent;
        if (viewParent instanceof View) {
            parent = (View) viewParent;
        } else {
            return null;
        }
        if (parent instanceof AbsListView || parent instanceof ScrollView || parent instanceof
                ViewPager || parent instanceof ScrollingView) {
            return (ViewGroup) parent;
        }
        return getScrollParent(parent);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        Log.d(TAG, "onSizeChanged() called with: w = [" + w + "], h = [" + h + "], oldw = [" +
                oldw + "], oldh = [" + oldh + "]");
        setBackgroundColor(Color.TRANSPARENT);
    }

    /**
     * 拖动时显示的View
     */
    private class BadgeView extends View {
        private Bitmap cacheBitmap;
        private PointF mOriginPoint;
        private PointF mDragPoint;
        private PointF mControlPoint;

        private float   mDragRadius;
        private float   mOriginRadius;
        private boolean isOutOfRange;//手指抬起时是否在超出最大范围
        private boolean isBezierBreak;//贝塞尔曲线是否已经拉断

        private Path mPath;

        private ValueAnimator mAnimator;

        public BadgeView(Context context) {
            super(context);
            init();
        }

        private void init() {
            mPath = new Path();
        }

        public void initPoints(float originX, float originY, float dragX, float dragY, float r) {
            mOriginPoint = new PointF(originX, originY);
            mDragPoint = new PointF(dragX, dragY);
            mControlPoint = new PointF((originX + dragX) / 2.0f, (originY + dragY) / 2.0f);
            mOriginRadius = r;
            mDragRadius = r;
            isOutOfRange = false;
            isBezierBreak = false;
        }

        @Override
        protected void onDraw(Canvas canvas) {

            if (!isOutOfRange && !isBezierBreak) {
                mPath.reset();

                float dx = mDragPoint.x - mOriginPoint.x;
                float dy = mDragPoint.y - mOriginPoint.y;

                //两圆交点偏移量
                float oDx = mOriginRadius;
                float oDy = 0;
                float dDx = mDragRadius;
                float dDy = 0;

                if (dx != 0) {
                    double a = Math.atan(dy / dx);//a:角度
                    oDx = (float) (Math.sin(a) * mOriginRadius);
                    oDy = (float) (Math.cos(a) * mOriginRadius);
                    dDx = (float) (Math.sin(a) * mDragRadius);
                    dDy = (float) (Math.cos(a) * mDragRadius);
                }

                //贝塞尔曲线控制点
                mControlPoint.set((mOriginPoint.x + mDragPoint.x) / 2.0f,
                        (mOriginPoint.y + mDragPoint.y) / 2.0f);
                //移动到第一个二阶贝塞尔曲线开始的点
                mPath.moveTo(mOriginPoint.x + oDx, mOriginPoint.y - oDy);

                mPath.quadTo(mControlPoint.x, mControlPoint.y,
                        mDragPoint.x + dDx, mDragPoint.y - dDy);

                //连接到第二个二阶贝塞尔曲线开始的点
                mPath.lineTo(mDragPoint.x - dDx, mDragPoint.y + dDy);

                mPath.quadTo(mControlPoint.x, mControlPoint.y, mOriginPoint.x - oDx,
                        mOriginPoint.y + oDy);
                mPath.close();
                canvas.drawPath(mPath, mPaint);

                //画固定圆
                canvas.drawCircle(mOriginPoint.x, mOriginPoint.y, mOriginRadius, mPaint);

                /*// 画出贝塞尔曲线可显示的范围
                mPaint.setStyle(Paint.Style.STROKE);
                canvas.drawCircle(mOriginPoint.x, mOriginPoint.y, mMaxMoveRange, mPaint);
                mPaint.setStyle(Paint.Style.FILL);*/
            } else {
                isBezierBreak = true;
            }

            //拖拽的图像
            canvas.drawBitmap(cacheBitmap, mDragPoint.x - cacheBitmap.getWidth() / 2,
                    mDragPoint.y - cacheBitmap.getHeight() / 2, mPaint);
        }

        public void updateDragPoint(float x, float y) {
            mDragPoint.set(x, y);
            BadgeView.this.postInvalidate();
        }

        public void updateView(float x, float y) {
            float distance = (float) Math.sqrt(Math.pow(mOriginPoint.y - mDragPoint.y, 2) +
                    Math.pow(mOriginPoint.x - mDragPoint.x, 2));

            isOutOfRange = distance > mMaxMoveRange;//判断拖拽点是否超出最大范围
            //固定圆,半径缩放
            mOriginRadius = mDragRadius - distance / 10;
            //最小半径5dp
            if (mOriginRadius < dp2px(5)) {
                mOriginRadius = dp2px(5);
            }

            updateDragPoint(x, y);
        }

        //复位
        public void reset() {
            final PointF tempDragPoint = new PointF(mDragPoint.x, mDragPoint.y);
            if (tempDragPoint.x == mOriginPoint.x && tempDragPoint.y == mOriginPoint.y) {
                return;
            }
            final FloatEvaluator evaluator = new FloatEvaluator();
            mAnimator = ValueAnimator.ofFloat(1.0f);
            mAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {

                @Override
                public void onAnimationUpdate(ValueAnimator mAnim) {
                    float percent = mAnim.getAnimatedFraction();
                    updateDragPoint(evaluator.evaluate(percent, tempDragPoint.x, mOriginPoint.x),
                            evaluator.evaluate(percent, tempDragPoint.y, mOriginPoint.y));
                }
            });
            mAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    clearAnimation();//结束时清除动画
                    ViewGroup rootView = (ViewGroup) BadgeView.this.getParent();
                    if (rootView != null) {
                        rootView.removeView(BadgeView.this);
                        DragBadgeView.this.setVisibility(VISIBLE);
                    }
                    recycleCacheBitmap();
                }
            });
            mAnimator.setInterpolator(new OvershootInterpolator());
            mAnimator.setDuration(500);
            mAnimator.start();
        }

        //消失
        public void disappear(float x, final float y) {
            ViewGroup rootView = (ViewGroup) BadgeView.this.getParent();
            if (rootView != null) {
                rootView.removeView(BadgeView.this);//DecorView中移除BadgeView
                //添加消失后的动画
                addExplodeImageView(x, y, rootView);
            }
            recycleCacheBitmap();

            if (mListener != null) {
                mListener.onDisappear(mText);
            }
        }

        /**
         * 回收CacheBitmap
         */
        private void recycleCacheBitmap() {
            if (cacheBitmap != null && !cacheBitmap.isRecycled()) {
                cacheBitmap.recycle();
                cacheBitmap = null;
            }
        }

        /**
         * 判断复位动画是否正在执行
         *
         * @return true:正在执行 false:反之
         */
        public boolean isResetAnimatorRunning() {
            return mAnimator != null && mAnimator.isRunning();
        }

        /**
         * 消失后的动画
         *
         * @param x        BadgeView消失的x坐标
         * @param y        BadgeView消失的y坐标
         * @param rootView DecorView
         */
        private void addExplodeImageView(final float x, final float y, final ViewGroup rootView) {
            final int totalDuration = 500;//动画总时长
            int d = totalDuration / 5;//每帧时长

            final ImageView explodeImage = new ImageView(getContext());
            final AnimationDrawable explodeAnimation = new AnimationDrawable();//创建帧动画
            //添加帧,图片放置在drawable-nodpi下
            explodeAnimation.addFrame(ContextCompat.getDrawable(getContext(), R.drawable.pop1), d);
            explodeAnimation.addFrame(ContextCompat.getDrawable(getContext(), R.drawable.pop2), d);
            explodeAnimation.addFrame(ContextCompat.getDrawable(getContext(), R.drawable.pop3), d);
            explodeAnimation.addFrame(ContextCompat.getDrawable(getContext(), R.drawable.pop4), d);
            explodeAnimation.addFrame(ContextCompat.getDrawable(getContext(), R.drawable.pop5), d);
            //设置动画只播放一次
            explodeAnimation.setOneShot(true);

            explodeImage.setImageDrawable(explodeAnimation);
            explodeImage.setVisibility(INVISIBLE);

            ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(ViewGroup
                    .LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);

            rootView.addView(explodeImage, params);

            explodeImage.post(new Runnable() {
                @Override
                public void run() {
                    explodeImage.setX(x - explodeImage.getWidth() / 2);
                    explodeImage.setY(y - explodeImage.getHeight() / 2);
                    explodeImage.setVisibility(VISIBLE);

                    explodeAnimation.start();

                    Handler handler = explodeImage.getHandler();
                    if (handler != null) {
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                explodeImage.setVisibility(GONE);
                                //动画结束后DecorView中移除ImageView控件
                                rootView.removeView(explodeImage);
                                DragBadgeView.this.setVisibility(INVISIBLE);
                            }
                        }, totalDuration);
                    }
                }
            });
        }
    }

    public float dp2px(float dp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, Resources.getSystem()
                .getDisplayMetrics());
    }

    public float sp2px(float sp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, Resources.getSystem()
                .getDisplayMetrics());
    }

}
