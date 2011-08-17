/*
 * Added a PageControl view at the bottom of the HorzontalPager
 * Modifications by Fabian Schmid, bases on the HorizontalPager from 
 * 
 * Yoni Samlan which  based on RealViewSwitcher, whose license is:
 *
 * Copyright (C) 2010 Marc Reichelt
 *
 * Work derived from Workspace.java of the Launcher application
 *  see http://android.git.kernel.org/?p=platform/packages/apps/Launcher.git
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.fabischmid.horizontalpager.withpagecontrol;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.graphics.drawable.shapes.Shape;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Scroller;

/**
 * A view group that allows users to switch between multiple screens (layouts) in the same way as
 * the Android home screen (Launcher application).
 * <p>
 * You can add and remove views using the normal methods {@link ViewGroup#addView(View)},
 * {@link ViewGroup#removeView(View)} etc. You may want to listen for updates by calling
 * {@link HorizontalPagerWithPageControl#setOnScreenSwitchListener(OnScreenSwitchListener)} in order to perform
 * operations once a new screen has been selected.
 *
 * Modifications from original version (ysamlan): Animate argument in setCurrentScreen and duration
 * in snapToScreen; onInterceptTouchEvent handling to support nesting a vertical Scrollview inside
 * the RealViewSwitcher; allowing snapping to a view even during an ongoing scroll; snap to
 * next/prev view on 25% scroll change; density-independent swipe sensitivity; width-independent
 * pager animation durations on scrolling to properly handle large screens without excessively
 * long animations.
 * 
 * Modifications from the version of ysamlan: Added page control to the bottom of this view. 
 *
 * Other modifications:
 * (aveyD) Handle orientation changes properly and fully snap to the right position.
 *
 * @author Marc Reichelt, <a href="http://www.marcreichelt.de/">http://www.marcreichelt.de/</a>
 * @version 0.1.0
 */
public final class HorizontalPagerWithPageControl extends ViewGroup {
    /*
     * How long to animate between screens when programmatically setting with setCurrentScreen using
     * the animate parameter
     */
    private static final int ANIMATION_SCREEN_SET_DURATION_MILLIS = 500;
    // What fraction (1/x) of the screen the user must swipe to indicate a page change
    private static final int FRACTION_OF_SCREEN_WIDTH_FOR_SWIPE = 4;
    private static final int INVALID_SCREEN = -1;
    /*
     * Velocity of a swipe (in density-independent pixels per second) to force a swipe to the
     * next/previous screen. Adjusted into mDensityAdjustedSnapVelocity on init.
     */
    private static final int SNAP_VELOCITY_DIP_PER_SECOND = 600;
    // Argument to getVelocity for units to give pixels per second (1 = pixels per millisecond).
    private static final int VELOCITY_UNIT_PIXELS_PER_SECOND = 1000;

    private static final int TOUCH_STATE_REST = 0;
    private static final int TOUCH_STATE_HORIZONTAL_SCROLLING = 1;
    private static final int TOUCH_STATE_VERTICAL_SCROLLING = -1;
    private int mCurrentScreen;
    private int mDensityAdjustedSnapVelocity;
    private boolean mFirstLayout = true;
    private float mLastMotionX;
    private float mLastMotionY;
    private OnScreenSwitchListener mOnScreenSwitchListener;
    private int mMaximumVelocity;
    private int mNextScreen = INVALID_SCREEN;
    private Scroller mScroller;
    private int mTouchSlop;
    private int mTouchState = TOUCH_STATE_REST;
    private VelocityTracker mVelocityTracker;
    private int mLastSeenLayoutWidth = -1;
    //Active and inactive draw
	private Drawable activeDrawable;
	private Drawable inactiveDrawable;
	//The size for the drawables
	private float 	 mIndicatorSize;
	//The screen scale to get px to dip
	private static float SCALE;

    /**
     * Simple constructor to use when creating a view from code.
     *
     * @param context The Context the view is running in, through which it can
     *        access the current theme, resources, etc.
     */
    public HorizontalPagerWithPageControl(final Context context) {
        super(context);
        init();
    }

    /**
     * Constructor that is called when inflating a view from XML. This is called
     * when a view is being constructed from an XML file, supplying attributes
     * that were specified in the XML file. This version uses a default style of
     * 0, so the only attribute values applied are those in the Context's Theme
     * and the given AttributeSet.
     *
     * <p>
     * The method onFinishInflate() will be called after all children have been
     * added.
     *
     * @param context The Context the view is running in, through which it can
     *        access the current theme, resources, etc.
     * @param attrs The attributes of the XML tag that is inflating the view.
     * @see #View(Context, AttributeSet, int)
     */
    public HorizontalPagerWithPageControl(final Context context, final AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    /**
     * Sets up the scroller and touch/fling sensitivity parameters for the pager.
     */
    private void init() {
        mScroller = new Scroller(getContext());
        //get the screen density
        SCALE = getResources().getDisplayMetrics().density;
        //set the indicator size resolution independent
        mIndicatorSize = (7 * SCALE);
        
        // Calculate the density-dependent snap velocity in pixels
        DisplayMetrics displayMetrics = new DisplayMetrics();
        ((WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay()
                .getMetrics(displayMetrics);
        mDensityAdjustedSnapVelocity =
                (int) (displayMetrics.density * SNAP_VELOCITY_DIP_PER_SECOND);

        final ViewConfiguration configuration = ViewConfiguration.get(getContext());
        mTouchSlop = configuration.getScaledTouchSlop();
        mMaximumVelocity = configuration.getScaledMaximumFlingVelocity();
        
        //draw the shapes
        makeShapes();
    }

    @Override
    protected void onMeasure(final int widthMeasureSpec, final int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        final int width = MeasureSpec.getSize(widthMeasureSpec);
        final int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        if (widthMode != MeasureSpec.EXACTLY) {
            throw new IllegalStateException("ViewSwitcher can only be used in EXACTLY mode.");
        }

        final int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        if (heightMode != MeasureSpec.EXACTLY) {
            throw new IllegalStateException("ViewSwitcher can only be used in EXACTLY mode.");
        }

        // The children are given the same width and height as the workspace
        final int count = getChildCount();
        for (int i = 0; i < count; i++) {
            getChildAt(i).measure(widthMeasureSpec, heightMeasureSpec);
        }

        if (mFirstLayout) {
            scrollTo(mCurrentScreen * width, 0);
            mFirstLayout = false;
        }

        else if (width != mLastSeenLayoutWidth) { // Width has changed
            /*
             * Recalculate the width and scroll to the right position to be sure we're in the right
             * place in the event that we had a rotation that didn't result in an activity restart
             * (code by aveyD). Without this you can end up between two pages after a rotation.
             */
            Display display =
                    ((WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE))
                            .getDefaultDisplay();
            int displayWidth = display.getWidth();

            mNextScreen = Math.max(0, Math.min(getCurrentScreen(), getChildCount() - 1));
            final int newX = mNextScreen * displayWidth;
            final int delta = newX - getScrollX();

            mScroller.startScroll(getScrollX(), 0, delta, 0, 0);
        }

        mLastSeenLayoutWidth   = width;
    }

    @Override
    protected void onLayout(final boolean changed, final int l, final int t, final int r,
            final int b) {
        int childLeft = 0;
        final int count = getChildCount();

        for (int i = 0; i < count; i++) {
            final View child = getChildAt(i);
            if (child.getVisibility() != View.GONE) {
                final int childWidth = child.getMeasuredWidth();
                child.layout(childLeft, 0, childLeft + childWidth, child.getMeasuredHeight());
                childLeft += childWidth;
            }
        }
    }

    @Override
    public boolean onInterceptTouchEvent(final MotionEvent ev) {
        /*
         * By Yoni Samlan: Modified onInterceptTouchEvent based on standard ScrollView's
         * onIntercept. The logic is designed to support a nested vertically scrolling view inside
         * this one; once a scroll registers for X-wise scrolling, handle it in this view and don't
         * let the children, but once a scroll registers for y-wise scrolling, let the children
         * handle it exclusively.
         */
        final int action = ev.getAction();
        boolean intercept = false;

        switch (action) {
            case MotionEvent.ACTION_MOVE:
                /*
                 * If we're in a horizontal scroll event, take it (intercept further events). But if
                 * we're mid-vertical-scroll, don't even try; let the children deal with it. If we
                 * haven't found a scroll event yet, check for one.
                 */
                if (mTouchState == TOUCH_STATE_HORIZONTAL_SCROLLING) {
                    /*
                     * We've already started a horizontal scroll; set intercept to true so we can
                     * take the remainder of all touch events in onTouchEvent.
                     */
                    intercept = true;
                } else if (mTouchState == TOUCH_STATE_VERTICAL_SCROLLING) {
                    // Let children handle the events for the duration of the scroll event.
                    intercept = false;
                } else { // We haven't picked up a scroll event yet; check for one.

                    /*
                     * If we detected a horizontal scroll event, start stealing touch events (mark
                     * as scrolling). Otherwise, see if we had a vertical scroll event -- if so, let
                     * the children handle it and don't look to intercept again until the motion is
                     * done.
                     */

                    final float x = ev.getX();
                    final int xDiff = (int) Math.abs(x - mLastMotionX);
                    boolean xMoved = xDiff > mTouchSlop;

                    if (xMoved) {
                        // Scroll if the user moved far enough along the X axis
                        mTouchState = TOUCH_STATE_HORIZONTAL_SCROLLING;
                        mLastMotionX = x;
                    }

                    final float y = ev.getY();
                    final int yDiff = (int) Math.abs(y - mLastMotionY);
                    boolean yMoved = yDiff > mTouchSlop;

                    if (yMoved) {
                        mTouchState = TOUCH_STATE_VERTICAL_SCROLLING;
                    }
                }

                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                // Release the drag.
                mTouchState = TOUCH_STATE_REST;
                break;
            case MotionEvent.ACTION_DOWN:
                /*
                 * No motion yet, but register the coordinates so we can check for intercept at the
                 * next MOVE event.
                 */
                mLastMotionY = ev.getY();
                mLastMotionX = ev.getX();
                break;
            default:
                break;
            }

        return intercept;
    }

    @Override
    public boolean onTouchEvent(final MotionEvent ev) {

        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
        mVelocityTracker.addMovement(ev);

        final int action = ev.getAction();
        final float x = ev.getX();

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                /*
                 * If being flinged and user touches, stop the fling. isFinished will be false if
                 * being flinged.
                 */
                if (!mScroller.isFinished()) {
                    mScroller.abortAnimation();
                }

                // Remember where the motion event started
                mLastMotionX = x;

                if (mScroller.isFinished()) {
                    mTouchState = TOUCH_STATE_REST;
                } else {
                    mTouchState = TOUCH_STATE_HORIZONTAL_SCROLLING;
                }

                break;
            case MotionEvent.ACTION_MOVE:
                final int xDiff = (int) Math.abs(x - mLastMotionX);
                boolean xMoved = xDiff > mTouchSlop;

                if (xMoved) {
                    // Scroll if the user moved far enough along the X axis
                    mTouchState = TOUCH_STATE_HORIZONTAL_SCROLLING;
                }

                if (mTouchState == TOUCH_STATE_HORIZONTAL_SCROLLING) {
                    // Scroll to follow the motion event
                    final int deltaX = (int) (mLastMotionX - x);
                    mLastMotionX = x;
                    final int scrollX = getScrollX();

                    if (deltaX < 0) {
                        if (scrollX > 0) {
                            scrollBy(Math.max(-scrollX, deltaX), 0);
                        }
                    } else if (deltaX > 0) {
                        final int availableToScroll =
                                getChildAt(getChildCount() - 1).getRight() - scrollX - getWidth();

                        if (availableToScroll > 0) {
                            scrollBy(Math.min(availableToScroll, deltaX), 0);
                        }
                    }
                }

                break;

            case MotionEvent.ACTION_UP:
                if (mTouchState == TOUCH_STATE_HORIZONTAL_SCROLLING) {
                    final VelocityTracker velocityTracker = mVelocityTracker;
                    velocityTracker.computeCurrentVelocity(VELOCITY_UNIT_PIXELS_PER_SECOND,
                            mMaximumVelocity);
                    int velocityX = (int) velocityTracker.getXVelocity();

                    if (velocityX > mDensityAdjustedSnapVelocity && mCurrentScreen > 0) {
                        // Fling hard enough to move left
                        snapToScreen(mCurrentScreen - 1);
                    } else if (velocityX < -mDensityAdjustedSnapVelocity
                            && mCurrentScreen < getChildCount() - 1) {
                        // Fling hard enough to move right
                        snapToScreen(mCurrentScreen + 1);
                    } else {
                        snapToDestination();
                    }

                    if (mVelocityTracker != null) {
                        mVelocityTracker.recycle();
                        mVelocityTracker = null;
                    }
                }

                mTouchState = TOUCH_STATE_REST;

                break;
            case MotionEvent.ACTION_CANCEL:
                mTouchState = TOUCH_STATE_REST;
                break;
            default:
                break;
        }

        return true;
    }

    @Override
    public void computeScroll() {
        if (mScroller.computeScrollOffset()) {
            scrollTo(mScroller.getCurrX(), mScroller.getCurrY());
            postInvalidate();
        } else if (mNextScreen != INVALID_SCREEN) {
            mCurrentScreen = Math.max(0, Math.min(mNextScreen, getChildCount() - 1));

            // Notify observer about screen change
            if (mOnScreenSwitchListener != null) {
                mOnScreenSwitchListener.onScreenSwitched(mCurrentScreen);
            }

            mNextScreen = INVALID_SCREEN;
        }
    }

    /**
     * Returns the index of the currently displayed screen.
     *
     * @return The index of the currently displayed screen.
     */
    public int getCurrentScreen() {
        return mCurrentScreen;
    }

    /**
     * Sets the current screen.
     *
     * @param currentScreen The new screen.
     * @param animate True to smoothly scroll to the screen, false to snap instantly
     */
    public void setCurrentScreen(final int currentScreen, final boolean animate) {
        mCurrentScreen = Math.max(0, Math.min(currentScreen, getChildCount() - 1));
        if (animate) {
            snapToScreen(currentScreen, ANIMATION_SCREEN_SET_DURATION_MILLIS);
        } else {
            scrollTo(mCurrentScreen * getWidth(), 0);
        }
        invalidate();
    }

    /**
     * Sets the {@link OnScreenSwitchListener}.
     *
     * @param onScreenSwitchListener The listener for switch events.
     */
    public void setOnScreenSwitchListener(final OnScreenSwitchListener onScreenSwitchListener) {
        mOnScreenSwitchListener = onScreenSwitchListener;
    }

    /**
     * Snaps to the screen we think the user wants (the current screen for very small movements; the
     * next/prev screen for bigger movements).
     */
    private void snapToDestination() {
        final int screenWidth = getWidth();
        int scrollX = getScrollX();
        int whichScreen = mCurrentScreen;
        int deltaX = scrollX - (screenWidth * mCurrentScreen);

        // Check if they want to go to the prev. screen
        if ((deltaX < 0) && mCurrentScreen != 0
                && ((screenWidth / FRACTION_OF_SCREEN_WIDTH_FOR_SWIPE) < -deltaX)) {
            whichScreen--;
            // Check if they want to go to the next screen
        } else if ((deltaX > 0) && (mCurrentScreen + 1 != getChildCount())
                && ((screenWidth / FRACTION_OF_SCREEN_WIDTH_FOR_SWIPE) < deltaX)) {
            whichScreen++;
        }

        snapToScreen(whichScreen);
    }

    /**
     * Snap to a specific screen, animating automatically for a duration proportional to the
     * distance left to scroll.
     *
     * @param whichScreen Screen to snap to
     */
    private void snapToScreen(final int whichScreen) {
        snapToScreen(whichScreen, -1);
    }

    /**
     * Snaps to a specific screen, animating for a specific amount of time to get there.
     *
     * @param whichScreen Screen to snap to
     * @param duration -1 to automatically time it based on scroll distance; a positive number to
     *            make the scroll take an exact duration.
     */
    private void snapToScreen(final int whichScreen, final int duration) {
        /*
         * Modified by Yoni Samlan: Allow new snapping even during an ongoing scroll animation. This
         * is intended to make HorizontalPager work as expected when used in conjunction with a
         * RadioGroup used as "tabbed" controls. Also, make the animation take a percentage of our
         * normal animation time, depending how far they've already scrolled.
         */
        mNextScreen = Math.max(0, Math.min(whichScreen, getChildCount() - 1));
        final int newX = mNextScreen * getWidth();
        final int delta = newX - getScrollX();

        if (duration < 0) {
             // E.g. if they've scrolled 80% of the way, only animation for 20% of the duration
            mScroller.startScroll(getScrollX(), 0, delta, 0, (int) (Math.abs(delta)
                    / (float) getWidth() * ANIMATION_SCREEN_SET_DURATION_MILLIS));
        } else {
            mScroller.startScroll(getScrollX(), 0, delta, 0, duration);
        }
        
        //sets the drawables when the user swipes 
        setActiveInactiveDrawables(whichScreen);
        
        //redraw screen
        invalidate();
    }
    
	/**
	 * Allways called when the user swipes to another view.
	 * Gets the current view an sets the active drawable / shape to the curretn view.
	 * (in the page control)
	 */
	public void setActiveInactiveDrawables(int whichScreen) {
		
		//Getting the Linear Layout where the page control drawables are inside
		LinearLayout linLayout = (LinearLayout) ((ViewGroup) this.getParent())
				.getChildAt(((LinearLayout) this.getParent()).getChildCount() - 1);
		//get every imageview and set the one of the current screen to active
		for (int i = 0; i < this.getChildCount(); i++) {
			ImageView imgView = (ImageView) linLayout.getChildAt(i);
			if (i == whichScreen) {
				imgView.setBackgroundDrawable(activeDrawable);
			} else {
				imgView.setBackgroundDrawable(inactiveDrawable);
			}
		}
	}

    /**
     * Listener for the event that the HorizontalPager switches to a new view.
     */
    public static interface OnScreenSwitchListener {
        /**
         * Notifies listeners about the new screen. Runs after the animation completed.
         *
         * @param screen The new screen index.
         */
        void onScreenSwitched(int screen);
    }
    
	/**
	 * Builds the active and inactive shapes / drawables for the page control
	 */
	private void makeShapes() {

		activeDrawable = new ShapeDrawable();
		inactiveDrawable = new ShapeDrawable();

		activeDrawable.setBounds(0, 0, (int) mIndicatorSize,
				(int) mIndicatorSize);
		inactiveDrawable.setBounds(0, 0, (int) mIndicatorSize,
				(int) mIndicatorSize);

		int i[] = new int[2];
		i[0] = android.R.attr.textColorSecondary;
		i[1] = android.R.attr.textColorSecondaryInverse;
		TypedArray a = getContext().getTheme().obtainStyledAttributes(i);

		Shape s1 = new OvalShape();
		s1.resize(mIndicatorSize, mIndicatorSize);
		Shape s2 = new OvalShape();
		s2.resize(mIndicatorSize, mIndicatorSize);

		((ShapeDrawable) activeDrawable).getPaint().setColor(
				a.getColor(0, Color.DKGRAY));
		((ShapeDrawable) inactiveDrawable).getPaint().setColor(
				a.getColor(1, Color.LTGRAY));

		((ShapeDrawable) activeDrawable).setShape(s1);
		((ShapeDrawable) inactiveDrawable).setShape(s2);
	}
	
	/**
	 * Called by the Activity when all Views are added to the horizontal pager
	 * to 
	 */
	public void addPagerControl() {
		ViewGroup view = (ViewGroup) this.getParent();
		view.addView(initPageControl());
	}
	
	private LinearLayout initPageControl() {
		LinearLayout linearLayout = new LinearLayout(getContext());
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.FILL_PARENT,
				LinearLayout.LayoutParams.FILL_PARENT, 2f);
		linearLayout.setLayoutParams(params);
		linearLayout.setOrientation(LinearLayout.HORIZONTAL);
		linearLayout.setGravity(Gravity.CENTER_HORIZONTAL
				| Gravity.CENTER_VERTICAL);
		linearLayout.setBackgroundColor(Color.BLACK);
		return setPageCount(this.getChildCount(), linearLayout);
	}
	
	/**
	 * Initializes the page control layout at be bottom of the news view.
	 * Draws all page control shapes an set the active shape to the first view
	 * @param pageCount the cout of the pages the user can swipe to
	 * @param linearLayout2 the page control linearlayout
	 * @return the given layout filled with the page control shapes
	 */
	private LinearLayout setPageCount(int pageCount, LinearLayout linearLayout2) {
		for (int i = 0; i < pageCount; i++) {
			final ImageView imageView = new ImageView(getContext());
			LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
					(int) mIndicatorSize, (int) mIndicatorSize);
			params.setMargins((int) mIndicatorSize / 2, (int) mIndicatorSize,
					(int) mIndicatorSize / 2, (int) mIndicatorSize);
			imageView.setLayoutParams(params);
			imageView.setBackgroundDrawable(inactiveDrawable);

			if (i == 0) {
				imageView.setBackgroundDrawable(activeDrawable);
			}
			linearLayout2.addView(imageView);
		}
		return linearLayout2;
	}
}
