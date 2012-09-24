package com.rus.common.view;

import com.rus.common.view.R;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;

public class NavigationViewActivity extends Activity {
	private int position;
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
    }
    
    public void select(View v){
    	AdapterView<?> view = (AdapterView<?>) findViewById(R.id.navigator);
    	view.setSelection(position >= view.getAdapter().getCount() - 1? position = 0 : ++position);
    }
}