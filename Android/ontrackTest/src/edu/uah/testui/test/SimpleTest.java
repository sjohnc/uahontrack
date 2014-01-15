package edu.uah.testui.test;

import edu.uah.testui.MainActivity;
import edu.uah.testui.R;
import android.app.Activity;
import android.test.ActivityInstrumentationTestCase2;
import android.test.UiThreadTest;
import android.webkit.WebView.FindListener;
import android.widget.Button;
import android.widget.TextView;

public class SimpleTest extends ActivityInstrumentationTestCase2<MainActivity> {

	private Button btnDirection;
	public SimpleTest() {
		super("edu.uah.testui", MainActivity.class);
	}

	protected void setUp() throws Exception {
		
		MainActivity mainActivity = getActivity();
		btnDirection = (Button) mainActivity.findViewById(R.id.btnDirection);
		super.setUp();
	}

	@UiThreadTest
	public void testBtnDirection(){
		for(int i = 0; i < 10; i++){
			btnDirection.performClick();
			assertEquals("Reverse",btnDirection.getText().toString());
			btnDirection.performClick();
			assertEquals("Forward",btnDirection.getText().toString());
		}
	}
}
