package edu.uah.ontrackcontroller;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Vector;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;

public class image_view extends View  {
	final int NUM_TRAINS = 10;
	final int NUM_SWITCHES = 30;
	
	public class trainData {
		//public Bitmap mBitmapMove;
		public ImageView myImgView;
		private float left, right;
		private float moving_speed;
		
		public float xCoordinate;
		public float yCoordinate;

		public float speed;
		public int address;
		
		public trainData() {
			address = 0;
			xCoordinate = (float) 0.0;
			yCoordinate = (float) 0.0;
			speed = (float) 0.0;
			left = 0;
			right = 0;
			moving_speed = 0;
		}
		

//		public Bitmap getmBitmapMove() {
//			return mBitmapMove;
//		}

		public void setmBitmapMove(Bitmap mBitmapMove) {
			
			myImgView.setImageBitmap(mBitmapMove);
		}

		public float getxCoordinate() {
			return xCoordinate;
		}

		public void setCoordinates(float xCoordinate, float yCoordinate) {
			this.yCoordinate = yCoordinate;
			this.xCoordinate = xCoordinate;
		}

		public float getyCoordinate() {
			return yCoordinate;
		}

		public float getSpeed() {
			return speed;
		}

		public void setSpeed(float speed) {
			this.speed = speed;
		}
		
	}
	trainData[] trainInfo;
	trainData[] switchInfo;
/*	private Bitmap mBitmapMove;
	private int left = 0, right = 0;
	private int moving_speed = 5;
	
	
	public Vector<Integer> xCoordinate = new Vector<Integer>();
	public Vector<Integer> yCoordinate = new Vector<Integer>();

	public ArrayList<Integer> speed = new ArrayList<Integer>();
*/
	int numTrains = 0;
	int numSwitches = 0;
	
	public image_view(Context context) {
		super(context);
		trainInfo = new trainData[NUM_TRAINS];
		switchInfo = new trainData[NUM_SWITCHES];
		for(int i = 0; i < NUM_TRAINS; i++){
			trainInfo[i] = new trainData();
			trainInfo[i].myImgView = new ImageView(context);
		}
		for(int i = 0; i < NUM_SWITCHES; i++){
			switchInfo[i] = new trainData();
			switchInfo[i].myImgView = new ImageView(context);
		}
		
		
	//	setTrainImage(context,1);
		setFocusableInTouchMode(true);
		setBackgroundResource(R.drawable.track_layout);
		
		setWillNotDraw(false);
	}
	
	 public void setTrainImage(Context context, int value, int trainIndex){

	    	int id = 0;
	    	switch(value){
	    	case 1:
	    		id = R.drawable.train1;
	    		Log.d("OnTrack", "Train 1 selected");
	    		break;
	    	case 2:
	    		id = R.drawable.train2;
	    		break;
	    	case 3:
	    		id = R.drawable.train3;
	    		break;
	    	case 4:
	    		id = R.drawable.train4;
	    		break;
	    	case 5:
	    		id = R.drawable.train5;
	    		break;
	    	case 6:
	    		id = R.drawable.train6;
	    		break;
	    	case 7:
	    		id = R.drawable.train7;
	    		break;
	    	case 8:
	    		id = R.drawable.train8;
	    		break;
	    	case 9:
	    		id = R.drawable.train9;
	    		break;
	    	case 10:
	    		id = R.drawable.train10;
	    		break;
	    		
	    	}
	    	numTrains = trainIndex+1;
	    	Log.d("OnTrack", "ID is: " + id + " index is: "+ trainIndex);
	    	Bitmap mBitMap = BitmapFactory.decodeResource(context.getResources(),id);
	    	trainInfo[trainIndex].myImgView.setImageBitmap(mBitMap);
	    	
	    }
	 
	 public void setSwitchImage(Context context, int value, int switchIndex)
	    {

	    	int id = R.drawable.football;
	    	
	    	numSwitches = switchIndex+1;
	    	Log.d("OnTrack", "ID is: " + id + " index is: "+ switchIndex);
	    	Bitmap mBitMap = BitmapFactory.decodeResource(context.getResources(),id);
	    	switchInfo[switchIndex].myImgView.setImageBitmap(mBitMap);
	    	
	    }
	

	@Override
	protected void onDraw(Canvas canvas) {

			//CheckCorner(canvas);
			super.onDraw(canvas);
	}
	private void CheckCorner(Canvas canvas) {
		
		trainData currentTrain;
		for(int trainIndex = 0; trainIndex < numTrains; trainIndex++){
			currentTrain = trainInfo[trainIndex];

			BitmapDrawable drawable = (BitmapDrawable) currentTrain.myImgView.getDrawable();
			Bitmap bitmap = drawable.getBitmap();
				canvas.drawBitmap(bitmap, currentTrain.getxCoordinate(),
						currentTrain.getyCoordinate(), new Paint());
			
		}
		trainData currentSwitch;
		for(int switchIndex = 0; switchIndex < numSwitches; switchIndex++){
			currentSwitch = switchInfo[switchIndex];
			currentSwitch.myImgView.bringToFront();
			BitmapDrawable drawable = (BitmapDrawable) currentSwitch.myImgView.getDrawable();
			Bitmap bitmap = drawable.getBitmap();
				canvas.drawBitmap(bitmap, currentSwitch.getxCoordinate(),
					currentSwitch.getyCoordinate(), new Paint());
			
		}
		
		
		this.invalidate();
	}

		
}
