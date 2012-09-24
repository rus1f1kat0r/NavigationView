package com.rus.common.view;

import com.rus.common.view.NavigationScroller.ScrollingListener;

import android.content.Context;
import android.database.DataSetObserver;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class NavigationLayout extends AdapterView<BaseAdapter> {
	private static final String LOG_TAG = "NavigationLayout";
	
	private BaseAdapter mAdapter;
	private final DataSetObserver mObserver;
	
	private int mCount;
	private int mFirstPosition;
	private int mSelected;
	private int mOffsetX;
	
	private final NavigationScroller mScroller;
	private final ScrollingListener mScrollListener = new ScrollListener();
	
	public NavigationLayout(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		this.mScroller = new NavigationScroller(context, mScrollListener);
		this.mObserver = new NavigationViewObserver();
		setAdapter(new TestAdapter(context));
	}

	public NavigationLayout(Context context, AttributeSet attrs) {
		this(context, attrs, android.R.style.Theme_Black);
	}

	public NavigationLayout(Context context) {
		this(context, null);
	}

	@Override
	protected void onLayout(boolean arg0, int l, int t, int r, int b) {	
		if (mAdapter == null || mCount == 0){
			return;
		}
		fillViews();
		positionChilds();
	}
	
	private void fillViews() {
		int position = getLastVisiblePosition();
		int right = 0;
		while (right < getWidth() && position < mCount){
			View v = mAdapter.getView(position, null, this);
			addAndMeasureView(v);
			right += v.getMeasuredWidth();
			position++;
		}
	}
	
//	private void fillViewsRight(){
//		int position = mFirstPosition;
//		View left = getChildAt(position);
//		int l = left.getLeft();
//		while (mFirstPosition > 0 && getChildAt(mFirstPosition).getLeft())
//		
//	}

	private void positionChilds() {
		int right = 0;
		int drawOffset = mOffsetX;
		for (int i = 0; i < getChildCount(); i++){
			View v = getChildAt(i);
			if (getPositionForView(v) < mSelected){
				drawOffset -= v.getMeasuredWidth();
			} else {
				break;
			}
		}
		for (int i = 0; i < getChildCount(); i++){
			View v = getChildAt(i);
			v.layout(drawOffset + right, 0, drawOffset + right + v.getMeasuredWidth(), v.getMeasuredHeight());
			right += v.getMeasuredWidth();
		}
	}

	private void addAndMeasureView(View child){
	    LayoutParams params = child.getLayoutParams();
	    if (params == null) { 
	        params = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.FILL_PARENT); 
	    }
	    addViewInLayout(child, -1, params, true);	 
	    int itemHeight = getHeight(); 
	    child.measure(MeasureSpec.AT_MOST | getWidth(), MeasureSpec.EXACTLY | itemHeight);
	}

	@Override
	public BaseAdapter getAdapter() {
		return mAdapter;
	}

	@Override
	public View getSelectedView() {
		if (getChildCount() > 0){
			return getChildAt(mSelected - mFirstPosition);			
		} else {
			return null;
		}
	}
	
	@Override
	public int getFirstVisiblePosition() {
		return mFirstPosition;
	}
	
	@Override
	public int getLastVisiblePosition() {
		return mFirstPosition + getChildCount();
	}
	/**
	 * Same as {@link AdapterView#getPositionForView(View)}, 
	 * but uses our {@link #mFirstPosition}
	 */
	@Override
	public int getPositionForView(View view) {
		View listItem = view;
		try {
			View v;
			while (!(v = (View) listItem.getParent()).equals(this)) {
				listItem = v;
			}
		} catch (ClassCastException e) {
			// We made it up to the window without find this list view
			return INVALID_POSITION;
		}
		
		// Search the children for the list item
		final int childCount = getChildCount();
		for (int i = 0; i < childCount; i++) {
			if (getChildAt(i).equals(listItem)) {
				return mFirstPosition + i;
			}
		}		
		// Child not found!
		return INVALID_POSITION;
	}

	@Override
	public void setAdapter(BaseAdapter arg0) {
		if (arg0 == null){
			throw  new IllegalArgumentException("The navigation items adapter can't be null");
		}
		if (mAdapter != null){
			mAdapter.unregisterDataSetObserver(mObserver);
		}
		mAdapter = arg0;
		mAdapter.registerDataSetObserver(mObserver);
		mCount = mAdapter.getCount();
		mSelected = 0;
		mOffsetX = 0;
	}

	@Override
	public void setSelection(int position) {
		if (position == mSelected){
			return;
		}
		if (position < 0 || position == mCount){
			throw new IllegalArgumentException("Invalid position " + position);
		}
		mSelected = position;
		mOffsetX = 0;
		requestLayout();
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		super.onTouchEvent(event);
		return mScroller.onTouch(this, event);
	}
	
	private void adjustSelectedPosition() {
		if (mSelected < getLastVisiblePosition() - 1){
			View selected = getChildAt(mSelected);
			if (mOffsetX <= selected.getMeasuredWidth() / -2){
				mSelected++;
				mOffsetX += selected.getMeasuredWidth();
			}				
		} 
		if (mSelected > mFirstPosition){
			Log.d(LOG_TAG, "change selected right");
			View beforeSelected = getChildAt(mSelected - 1);
			if (mOffsetX >= beforeSelected.getMeasuredWidth() / 2){
				mSelected--;
				mOffsetX -= beforeSelected.getMeasuredWidth();
			}
		}
	}

	private class NavigationViewObserver extends DataSetObserver{
		@Override
		public void onChanged() {
			super.onChanged();
		}
		
		@Override
		public void onInvalidated() {
			super.onInvalidated();
		}
	}
		
	private final class ScrollListener implements ScrollingListener {
		
		@Override
		public void onStarted() {
			Log.d(LOG_TAG, "onStarted()");
		}

		@Override
		public void onScroll(int distance) {
			Log.d(LOG_TAG, "onScroll(" + distance + ")");
			mOffsetX += distance;
			adjustSelectedPosition();
			requestLayout();
		}

		@Override
		public void onJustify() {
			Log.d(LOG_TAG, "onJustify()");
			mScroller.scroll(mOffsetX, 0);
		}

		@Override
		public void onFinished() {
			Log.d(LOG_TAG, "onFinished()");
		}
	}
	
	private static class TestAdapter extends BaseAdapter{

		private final String[] mItems = new String[]{"Item 1", "Item 2", "Item 3", "Item 4", "Item 5", "Item 6", "Item 7", "Item 8", "Item 9"};
		private final Context mContext;
		
		public TestAdapter(Context mContext) {
			super();
			this.mContext = mContext;
		}

		@Override
		public int getCount() {
			return mItems.length;
		}

		@Override
		public Object getItem(int position) {
			return mItems[position];
		}

		@Override
		public long getItemId(int position) {
			return getItem(position).hashCode();
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if (convertView == null){
				convertView = LayoutInflater.from(mContext).inflate(android.R.layout.simple_list_item_1, parent, false);
			}
			TextView tb = (TextView) convertView.findViewById(android.R.id.text1);
			tb.setText("view " + position);
			convertView.setBackgroundColor(0xff000000 + (0xff<<8*position));
			return convertView;
		}
		
	}
	
}
