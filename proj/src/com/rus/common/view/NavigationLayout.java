package com.rus.common.view;

import com.rus.common.view.NavigationScroller.ScrollingListener;

import android.content.Context;
import android.database.DataSetObserver;
import android.graphics.Rect;
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
	private int mScrollDelta;
	
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
		//TODO remove invisible views
		fillViews();
		fillViewsRight();
		positionChilds();
	}
	
	private int computeLayoutOffset(){
		int layoutOffset = mOffsetX;
		for (int i = 0; i < getChildCount(); i++){
			View v = getChildAt(i);
			if (getPositionForView(v) < mSelected){
				layoutOffset -= v.getMeasuredWidth();
			} else {
				break;
			}
		}
		return layoutOffset;
	}
	
	private void fillViews() {
		//adds more views to the right edge of the view
		//in case we are scrolling to the right so 
		//right edge becomes visible to the user
		int position = getLastVisiblePosition();
		int right = (getChildCount() > 0 ? getChildAt(getChildCount() -1).getRight() + mScrollDelta: 0);
		addViewsToRight(position, right);
		//remove views out of screen from right side
		//in case we are scrolling view to the left
		//and right edge becomes invisible
		Rect invalidRegion = removeViewsFromRight();
		if (!invalidRegion.isEmpty()){
			invalidate(invalidRegion);
		}
	}

	private Rect removeViewsFromRight() {
		Rect invalidRegion = new Rect();
		while (getChildCount() > 0){
			View rightMost = getChildAt(getChildCount() - 1);
			int left = rightMost.getLeft() + mScrollDelta;
			if (left > getWidth()){
//				Log.d(LOG_TAG, "fillViews() remove view left " + left + " width" + getWidth());
				Rect r = new Rect();
				rightMost.getHitRect(r);
				invalidRegion.union(r);
				removeViewInLayout(rightMost);
			} else {
				break;
			}			
		}
		return invalidRegion;
	}
	
	private void addViewsToRight(int position, int right){
		while (right < getWidth() && position < mCount){
			View v = mAdapter.getView(position, null, this);
//			Log.d(LOG_TAG, "fillViews() add view right " + right + " position " + position);
			addAndMeasureView(v);
			right += v.getMeasuredWidth();
			position++;
		}		
	}
	
	private void addAndMeasureView(View v) {
		addAndMeasureView(v, -1);
	}

	private void fillViewsRight(){
		int left = (getChildCount() > 0 ? getChildAt(0).getLeft() + mScrollDelta : 0);
		left = addViewsToLeft(left);
//		adjustSelectedPosition();
		Rect invalidRegion = removeViewsFromLeft();
		if (!invalidRegion.isEmpty()){
			invalidate(invalidRegion);
		}
	}

	private int addViewsToLeft(int left) {
		while (mFirstPosition > 0 && (left >= 0 || mSelected < mFirstPosition)){
			mFirstPosition --;
			View v = mAdapter.getView(mFirstPosition, null, this);
			Log.d(LOG_TAG, "fillViews() add view left " + left + " position " + mFirstPosition);
			addAndMeasureView(v, 0);
			left -= v.getMeasuredWidth();
		}
		return left;
	}

	private Rect removeViewsFromLeft() {
		Rect invalidRegion = new Rect();
		while (getChildCount() > 0){
			View leftMost = getChildAt(0);
			int right = leftMost.getRight() + mScrollDelta;
			if (right < 0 && mSelected >= mFirstPosition){
				mFirstPosition ++;
				Log.d(LOG_TAG, "fillViews() remove view left " + right + " i=" + mFirstPosition);
				Rect r = new Rect();
				leftMost.getHitRect(r);
				invalidRegion.union(r);
				removeViewInLayout(leftMost);
			} else {
				break;
			}
		}
		return invalidRegion;
	}

	private void positionChilds() {
		int layoutOffset = computeLayoutOffset();
		int right = 0;
		for (int i = 0; i < getChildCount(); i++){
			View v = getChildAt(i);
			v.layout(layoutOffset + right, 0, layoutOffset + right + v.getMeasuredWidth(), v.getMeasuredHeight());
			right += v.getMeasuredWidth();
		}
	}

	private void addAndMeasureView(View child, int position){
	    LayoutParams params = child.getLayoutParams();
	    if (params == null) { 
	        params = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.FILL_PARENT); 
	    }
	    addViewInLayout(child, position, params, true);	 
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
		if (mScrollDelta < 0) {
			adjustSelectionRight(); 
		} else {
			adjustSelectionLeft();			
		}
	}

	private void adjustSelectionRight() {
		while (mSelected < getLastVisiblePosition() - 1){
			View selected = getChildAt(mSelected - mFirstPosition);
			if (mOffsetX <= selected.getMeasuredWidth() / -2){
				mSelected++;
				mOffsetX += selected.getMeasuredWidth();
				Log.d(LOG_TAG, "change selected left " + mSelected);
			} else {
				break;
			}
		}
	}

	private void adjustSelectionLeft() {
		while (mSelected > mFirstPosition){
			View beforeSelected = getChildAt(mSelected - mFirstPosition - 1);
			if (mOffsetX >= beforeSelected.getMeasuredWidth() / 2){
				mSelected--;
				mOffsetX -= beforeSelected.getMeasuredWidth();
				Log.d(LOG_TAG, "change selected right " + mSelected);
			} else {
				break;
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
//			Log.d(LOG_TAG, "onStarted()");
		}

		@Override
		public void onScroll(int distance) {
			Log.d(LOG_TAG, "onScroll(" + distance + ")");
			Log.d(LOG_TAG, "onScroll(selected = " + mSelected + ")");
			mScrollDelta = distance;
			mOffsetX += distance;
			adjustSelectedPosition();
			requestLayout();
		}

		@Override
		public void onJustify() {
//			Log.d(LOG_TAG, "onJustify()");
			mScroller.scroll(mOffsetX, 0);
		}

		@Override
		public void onFinished() {
//			Log.d(LOG_TAG, "onFinished()");
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
