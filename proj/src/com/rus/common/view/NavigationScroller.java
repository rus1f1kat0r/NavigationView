package com.rus.common.view;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.Scroller;

public class NavigationScroller extends Scroller implements OnTouchListener {
    
    private static final int SCROLLING_DURATION = 200;
    public static final int MIN_DELTA_FOR_SCROLLING = 1;
    // Messages
    private final int MESSAGE_SCROLL = 0;
    private final int MESSAGE_JUSTIFY = 1;
   
    private final ScrollingListener mScrollListener;
    private int mLastX;
    private float mLastTouchX;
    private boolean mScrolling;
    
    // animation handler
    private Handler animationHandler = new AnimationHandler();

	public NavigationScroller(Context context, ScrollingListener listener) {
		super(context);
		this.mScrollListener = listener;
	}
    /**
     * Scroll the wheel
     * @param distance the scrolling distance
     * @param time the scrolling duration
     */
    public void scroll(int distance, int time) {
		Log.d("NavigationScroller", "scroll(" + distance+")");
        forceFinished(true);
        mLastX = 0;        
        startScroll(0, 0, distance, 0, time != 0 ? time : SCROLLING_DURATION);
        setNextMessage(MESSAGE_SCROLL);
        startScrolling();
    }
   
    /**
     * Stops scrolling
     */
    public void stopScrolling() {
        forceFinished(true);
    }
    
	@Override
	public boolean onTouch(View v, MotionEvent event) {
		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN:
			mLastTouchX = event.getX();
			forceFinished(true);
			clearMessages();
			break;
			
		case MotionEvent.ACTION_MOVE:
			// perform scrolling
			int distanceX = (int)(event.getX() - mLastTouchX);
			if (distanceX != 0) {
				startScrolling();
				mScrollListener.onScroll(distanceX);
				mLastTouchX = event.getX();
			}
			break;
		}
		
		if (event.getAction() == MotionEvent.ACTION_UP) {
			justify();
		}
		return true;
	}
    /**
     * Justifies wheel
     */
    private void justify() {
        mScrollListener.onJustify();
        setNextMessage(MESSAGE_JUSTIFY);
    }

    /**
     * Starts scrolling
     */
    private void startScrolling() {
        if (!mScrolling) {
            mScrolling = true;
            mScrollListener.onStarted();
        }
    }

    /**
     * Finishes scrolling
     */
    void finishScrolling() {
        if (mScrolling) {
            mScrollListener.onFinished();
            mScrolling = false;
        }
    }
    /**
     * Set next message to queue. Clears queue before.
     * 
     * @param message the message to set
     */
    private void setNextMessage(int message) {
        clearMessages();
        animationHandler.sendEmptyMessage(message);
    }

    /**
     * Clears messages from queue
     */
    private void clearMessages() {
        animationHandler.removeMessages(MESSAGE_SCROLL);
        animationHandler.removeMessages(MESSAGE_JUSTIFY);
    }
    
    private final class AnimationHandler extends Handler {
		public void handleMessage(Message msg) {
            computeScrollOffset();
            int currX = getCurrX();
            int delta = mLastX - currX;
            mLastX = currX;
            if (delta != 0) {
                mScrollListener.onScroll(delta);
            }
            
            // scrolling is not finished when it comes to final Y
            // so, finish it manually 
            if (Math.abs(currX - getFinalX()) < MIN_DELTA_FOR_SCROLLING) {
                currX = getFinalX();
                forceFinished(true);
            }
            if (!isFinished()) {
                animationHandler.sendEmptyMessage(msg.what);
            } else if (msg.what == MESSAGE_SCROLL) {
                justify();
            } else {
                finishScrolling();
            }
        }
	}
    
	public interface ScrollingListener {
        void onScroll(int distance);
        void onStarted();
        void onFinished();
        void onJustify();
    }
}
