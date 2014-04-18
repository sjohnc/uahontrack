package edu.onTrack.controlsystem;

//Imports
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore.MediaColumns;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.text.InputType;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.DragEvent;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.LinearLayout;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

public class MainActivity extends Activity {

	Button addLayout;
	double heightOfLayout;
	double widthOfLayout;
	// ImageView img,img1;
	int column_index;
	Intent intent = null;
	String logo, imagePath, Logo;
	Cursor cursor;

	ImageView image_view_track_layout;

	// YOU CAN EDIT THIS TO WHATEVER YOU WANT
	private static final int SELECT_PICTURE = 1;

	String selectedImagePath;
	String filemanagerstring;

	GestureDetector gestureDetector;
	boolean tapped;
	Thread IMUThread = null;

	int[] trainNum; // int array to hold train numbers
	int[] switchNum;
	int[] barcodeNum;

	int[] trainAddress;
	int[] switchAddress;
	int[] barcodeAddress;

	String[] stringSpeed = new String[] { "00000", "10000", "00001", "10001",
			"00010", "10010", "00011", "10011", "00100", "10100", "00101",
			"10101", "00110", "10110", "00111", "10111", "01000", "11000",
			"01001", "11001", "01010", "11010", "01011", "11011", "01100",
			"11100", "01101", "11101", "01110", "11110", "01111", "11111" };
	int[] speedArray = new int[stringSpeed.length];

	final int MAX_TRAINS = 127;
	final int MAX_SWITCHES = 127;
	final int MAX_BARCODES = 127;

	protected static final byte IMU_HEADER = 10;
	final int MAX_BUFFER = 10;
	final int IMU_DATA_SIZE = 32;
	final int CURRENT_DATA_SIZE = 3;
	final int MAX_ADDRESS = 127;
	final int NOT_VALID = -1;
	int px;

	byte[] inbuffer = new byte[1024];

	byte[][][] imuBuffer = new byte[MAX_TRAINS][MAX_BUFFER][IMU_DATA_SIZE];

	List<String> trainList; // string list for train names
	List<String> switchList; // string list for train names
	List<String> barcodeList;
	List<String> addressList; // string list to hold all bt device addresses

	List<String> userActionsList;
	Bitmap[] trainBmp;

	// connection
	ArrayAdapter<String> trainAdapter; // adapter for spinner and string list
	ArrayAdapter<String> switchAdapter;
	ArrayAdapter<String> barcodeAdapter;

	// Index
	int trainIndex = 0;
	int switchIndex = 0;
	int barcodeIndex = 0;
	int imuBufferIndex = 0;
	int trainLayoutHeight = 0;
	int trainLayoutWidth = 0;
	int yawOffset = 0;
	double trainLayoutScaleHeight = 0;
	double trainLayoutScaleWidth = 0;

	TextDrawable txtDrawNumber;
	Drawable train_background;
	Drawable train_foreground;

	// Add switch and barcode drawables
	Drawable switch_background;
	Drawable switch_alternative;
	Drawable switch_foreground;

	Drawable barcode_background;
	Drawable barcode_foreground;

	// Train_list, switch_list and barcode_list to store imageView being added
	image_view[] train_list;
	image_view[] switch_list;
	image_view[] barcode_list;

	// connecting Layout Image View's to these three objects
	ImageView imgViewBarcodes;
	ImageView imgViewSwitches;
	ImageView imgViewTrains;

	TextView txtvwStatus;
	// connecting Layout's to these two objects
	RelativeLayout relLayout_Track;
	RelativeLayout relLayout_Tray;
	DisplayMetrics metrics;

	Button btnConnect;
	Button btnIMU;
	Button btnRezero;
	Button btnPacket1;
	Button btnPacket2;
	Button btnPacket3;
	Button btnLocoOn;
	Button btnLocoOff;
	EditText edtxtTieCount;
	EditText edtxtAngle;
	EditText edtxtBarCode;
	ToggleButton tglbtnSwitch;
	VerticalSeekBar vertSeekBar;
	Spinner spnTrain; // spinner for train selection
	Spinner spnSwitch;

	// These are just TAG's
	final String TAG_TRACK = "relLayout_Track";
	final String TAG_TRAY = "tray";
	final String TAG_DEBUG = "OnTrack";
	final String TAG_BARCODES = "barcode";
	final String TAG_SWITCHES = "switch";
	final String TAG_TRAINS = "train";

	// BlueTooth variables
	BluetoothAdapter btAdapter = null; // start all BT items as null to start.
	BluetoothDevice btDevice = null;
	BluetoothSocket btSocket = null;
	OutputStream btOutStream = null;
	InputStream btInStream = null;
	boolean btconnected = false;
	boolean currentRunning = false;
	boolean exitingNow = false;
	boolean firstTouch = true;
	private static String address = null; // default address will be changed
	private static final UUID MY_UUID = UUID
			.fromString("00001101-0000-1000-8000-00805F9B34FB"); // default

	BroadcastReceiver bcastReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
				// Device has disconnected
				btconnected = false;
				txtvwStatus.setText("Disconnected");
				currentRunning = false;
				btnConnect.setText("Connect");
				Log.d(TAG_DEBUG, "currentRunning onReceive");

				// handler.removeCallbacks(currentRun);
				try {
					btSocket.close();
					btSocket = null;
					IMUThread = null;
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
				btconnected = true;
				Log.d(TAG_DEBUG, "Connected");
				txtvwStatus.setText("Connected");
				btnConnect.setText("Disconnect");
				try {
					btInStream = btSocket.getInputStream();
					btOutStream = btSocket.getOutputStream();
					if (IMUThread == null) {
						createIMUThread();
						IMUThread.start();
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
				currentRunning = true;
			}

		}

	};

	public class GestureListener extends
			GestureDetector.SimpleOnGestureListener {

		@Override
		public boolean onDown(MotionEvent e) {
			return false;
		}

		@Override
		public boolean onDoubleTap(MotionEvent e) {

			return true;
		}

	}

	final View.OnTouchListener touchListener = new View.OnTouchListener() {

		@Override
		public boolean onTouch(View v, MotionEvent event) {
			image_view touchedObject = (image_view) v;
			if (gestureDetector.onTouchEvent(event)) {

				userActionsDeleteOrModify(touchedObject);
			} else {
				selectTouchedObject(touchedObject);
			}

			// Need to return false to make sure double tap and
			// longClickListener works together
			return false;
		}
	};
	
	final View.OnClickListener switchClickListener = new View.OnClickListener() {
		
		@Override
		public void onClick(View v) {
			image_view touchedObject = (image_view) v;
			int pos = switchAddress[touchedObject.getMyIndex()];
			if (switch_list[pos].getMyState() == 0) {
				tglbtnSwitch.setChecked(true);
				switch_list[pos].setMyState(1);
			} else {
				tglbtnSwitch.setChecked(false);
				switch_list[pos].setMyState(0);
			}
			locoNetMessagePrepAndSend();
			
		}
	};

	final View.OnLongClickListener longClickListener = new View.OnLongClickListener() {

		@Override
		public boolean onLongClick(View view) {

			View.DragShadowBuilder shadowBuilder = new View.DragShadowBuilder(
					view);
			view.startDrag(null, shadowBuilder, view, 0);
			return false;
		}
	};

	private class BTConnect extends AsyncTask<Void, Void, Void> {
		/*
		 * Creates an alert dialog which displays a list of all bonded bluetooth
		 * devices. This allows you to choose the Bluetooth Device you wish to
		 * connect to. Once the user selects the device from the list, an
		 * attempt to connect is made.
		 */
		// Test alert dialog with list view of all bt devices.
		protected void onPreExecute() {
			// handler.removeCallbacks(currentRun);
			if (btSocket != null)
				try {
					btSocket.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			AlertDialog.Builder builderSingle = new AlertDialog.Builder(
					MainActivity.this);
			builderSingle.setIcon(R.drawable.ic_launcher);
			builderSingle.setTitle("Select One Name:-");
			final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(
					MainActivity.this,
					android.R.layout.select_dialog_singlechoice);
			Set<BluetoothDevice> btDevices = btAdapter.getBondedDevices();
			addressList = new ArrayList<String>();
			for (BluetoothDevice bluetoothDevice : btDevices) {
				arrayAdapter.add(bluetoothDevice.getName());
				addressList.add(bluetoothDevice.getAddress());
			}
			arrayAdapter.add("Add new");
			addressList.add("Add new");
			builderSingle.setNegativeButton("cancel",
					new DialogInterface.OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
							dialog.dismiss();
						}
					});

			builderSingle.setAdapter(arrayAdapter,
					new DialogInterface.OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
							if (addressList.get(which) == "Add new") {
								Intent intentBluetooth = new Intent();
								intentBluetooth
										.setAction(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS);
								startActivity(intentBluetooth);
							}
							address = addressList.get(which);

						}
					});
			builderSingle.show();
			// txtvwStatus.setText("Connecting to Bluetooth, please wait...");
		}

		@Override
		protected void onPostExecute(Void result) {
			new btmakeconnection().execute();
			txtvwStatus.setText("Connecting...");
			super.onPostExecute(result);

		}

		@Override
		protected Void doInBackground(Void... params) {
			return null;

		}
	}

	private class btmakeconnection extends AsyncTask<Void, Void, Void> {

		int maxTries = 20;
		int curTry = 0;

		@Override
		protected void onPostExecute(Void result) {

			// handler.postDelayed(currentRun, 5000);
			if (curTry >= maxTries) {
				Log.d(TAG_DEBUG, "failed to connect");
				txtvwStatus.setText("Connection Unsuccessful!");
			} else {
				txtvwStatus.setText("Connection Successful!");
				currentRunning = true;

			}

			// filters();
			super.onPostExecute(result);

		}

		@Override
		protected Void doInBackground(Void... params) {
			// Original BT Code that works below:
			while (address == null)
				;
			while ((btSocket == null || !btSocket.isConnected())
					&& curTry < maxTries) {
				curTry++;
				if (BluetoothAdapter.checkBluetoothAddress(address)) {

					btAdapter.cancelDiscovery();
					Log.d(TAG_DEBUG, "Canceled Discovery");
					btDevice = btAdapter.getRemoteDevice(address);
					Log.d(TAG_DEBUG, "Connecting to ... " + btDevice);

					try {

						Log.d(TAG_DEBUG, "trying to create socket");
						btSocket = btDevice
								.createRfcommSocketToServiceRecord(MY_UUID);
						// Here is the part the connection is made, by asking
						// the device to create a RfcommSocket (Unsecure socket
						// I guess), It map a port for us or something like that
						btSocket.connect();
						// txtvwStatus.setText("BlueTooth Connection Successful");
						Log.d(TAG_DEBUG, "Connection made.");

					} catch (IOException e) {

						// txtvwStatus.setText("BlueTooth Connection Unsuccessful");
						Log.d(TAG_DEBUG, "failed to create socket");
						try {
							btSocket.close();
						} catch (IOException e2) {
							Log.d(TAG_DEBUG, "Unable to end the connection");
						}
						Log.d(TAG_DEBUG, "Socket creation failed");
					}
				} else {

					Log.d(TAG_DEBUG, "Bad Address");
					// txtvwStatus.setText("BlueTooth Connection Unsuccessful");
				}
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}

			return null;
		}

	}

	public class TextDrawable extends Drawable {

		private final String text;
		private final Paint paint;
		private final float textSize = 32;

		public TextDrawable(String text, int textColor) {

			this.text = text;

			this.paint = new Paint();
			paint.setColor(textColor);

			paint.setTextSize(textSize);
			paint.setAntiAlias(true);
			paint.setFakeBoldText(true);
			paint.setStyle(Paint.Style.FILL);
			paint.setTextAlign(Paint.Align.CENTER);
		}

		@Override
		public void draw(Canvas canvas) {
			canvas.drawText(text, px / 2, px / 2 + textSize / 3, paint);
		}

		@Override
		public void setAlpha(int alpha) {
			paint.setAlpha(alpha);
		}

		@Override
		public void setColorFilter(ColorFilter cf) {
			paint.setColorFilter(cf);
		}

		@Override
		public int getOpacity() {
			return PixelFormat.TRANSLUCENT;
		}
	}

	// This class is a custom class inherited from ImageView.
	// myType: variable to store if it's train/switch/barcode
	// myIndex: variable to store what is it's index
	public class image_view extends ImageView {

		public String myType;
		public int myIndex;
		public int myState;
		float prevX;
		float prevY;

		public image_view(Context context) {
			super(context);
			myType = "";
			myIndex = 0;

			prevX = 0;
			prevY = 0;
		}

		void setMyIndex(int myID) {
			myIndex = myID;
		}

		void setMyState(int state) {
			myState = state;
		}

		int getMyState() {
			return myState;
		}

		int getMyIndex() {
			return myIndex;
		}

		float getPrevX() {
			return prevX;
		}

		float getPrevY() {
			return prevY;
		}

		void setPrevX(float inPrevX) {
			prevX = inPrevX;
		}

		void setPrevY(float inPrevY) {
			prevY = inPrevY;
		}

		void setMyType(String mytype) {
			myType = mytype;
		}

		String getType() {
			return myType;
		}

	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.ontrackcontroller);

		this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
		getWindow().setSoftInputMode(
				WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
		init();
		metrics = getResources().getDisplayMetrics();
		connectWidgets();
		setListeners();
		BTSetup();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	/*
	 * Start: onCreate Helper Functions connectWidgets(); setListeners();
	 * BTSetup(); init(); filters();
	 */
	private void connectWidgets() {

		// Drawables
		train_background = (Drawable) getResources().getDrawable(
				R.drawable.trainback);
		train_foreground = (Drawable) getResources().getDrawable(
				R.drawable.trainfront);
		switch_background = (Drawable) getResources().getDrawable(
				R.drawable.switchback);
		switch_alternative = (Drawable) getResources().getDrawable(
				R.drawable.switchback2);
		switch_foreground = (Drawable) getResources().getDrawable(
				R.drawable.switchfront);
		barcode_background = (Drawable) getResources().getDrawable(
				R.drawable.barcodeback);
		barcode_foreground = (Drawable) getResources().getDrawable(
				R.drawable.barcodefront);

		// ImageViews
		px = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 30,
				getResources().getDisplayMetrics());

		imgViewBarcodes = (ImageView) findViewById(R.id.imgViewBarcodes);
		imgViewBarcodes.setTag(TAG_BARCODES);
		imgViewBarcodes.setMinimumHeight(px);
		imgViewBarcodes.setMinimumWidth(px);
		imgViewBarcodes.setMaxHeight(px);
		imgViewBarcodes.setMaxWidth(px);

		imgViewSwitches = (ImageView) findViewById(R.id.imgViewSwitches);
		imgViewSwitches.setTag(TAG_SWITCHES);
		imgViewSwitches.setMinimumHeight(px);
		imgViewSwitches.setMinimumWidth(px);
		imgViewSwitches.setMaxHeight(px);
		imgViewSwitches.setMaxWidth(px);

		imgViewTrains = (ImageView) findViewById(R.id.imgViewTrains);
		imgViewTrains.setTag(TAG_TRAINS);
		imgViewTrains.setMinimumHeight(px);
		imgViewTrains.setMinimumWidth(px);
		imgViewTrains.setMaxHeight(px);
		imgViewTrains.setMaxWidth(px);

		// Buttons
		btnConnect = (Button) findViewById(R.id.btnConnect);
		btnIMU = (Button) findViewById(R.id.btnSound);
		btnRezero = (Button) findViewById(R.id.btnBarcode);
		btnPacket1 = (Button) findViewById(R.id.btnLocoPacket1);
		btnPacket2 = (Button) findViewById(R.id.btnLocoPacket2);
		btnPacket3 = (Button) findViewById(R.id.btnLocoPacket3);
		btnLocoOn = (Button) findViewById(R.id.btnLocoTrackOn);
		btnLocoOff = (Button) findViewById(R.id.btnLocoTrackOff);
		btnIMU.setText("IMU Test");
		edtxtBarCode = (EditText) findViewById(R.id.edtxtBarCode);
		edtxtAngle = (EditText) findViewById(R.id.edtxtAngle);
		edtxtTieCount = (EditText) findViewById(R.id.edtxtTieCount);
		vertSeekBar = (VerticalSeekBar) findViewById(R.id.vertSeekBar);
		vertSeekBar.setMax((speedArray.length - 1) * 2);
		vertSeekBar.setProgress(speedArray.length);
		tglbtnSwitch = (ToggleButton) findViewById(R.id.tglbtnSwitch);
		spnTrain = (Spinner) findViewById(R.id.spnTrainSelect);
		spnSwitch = (Spinner) findViewById(R.id.spnSwitchSel);
		txtvwStatus = (TextView) findViewById(R.id.txtvwStatus);

		// Layouts
		relLayout_Track = (RelativeLayout) findViewById(R.id.relLayout_Track);
		relLayout_Tray = (RelativeLayout) findViewById(R.id.relLayout_Tray);

		addLayout = (Button) findViewById(R.id.addLayout);

		image_view_track_layout = (ImageView) findViewById(R.id.relLayout_ImageView);
	}

	private void setListeners() {
		// Set longclicklistener for all 3 tray items
		imgViewBarcodes.setOnLongClickListener(longClickListener);
		imgViewSwitches.setOnLongClickListener(longClickListener);
		imgViewTrains.setOnLongClickListener(longClickListener);

		trainAdapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_spinner_item, trainList);
		trainAdapter
				.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spnTrain.setAdapter(trainAdapter);

		switchAdapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_spinner_item, switchList);
		switchAdapter
				.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spnSwitch.setAdapter(switchAdapter);
		// image_view_track_layout.setOnTouchListener(new View.OnTouchListener()
		// {
		//
		// @Override
		// public boolean onTouch(View v, MotionEvent event) {
		// Toast.makeText(getBaseContext(), "Touched at x: " +event.getX() +
		// " y: " + event.getY() , Toast.LENGTH_SHORT).show();
		// return false;
		// }
		// });
		btnConnect.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				if (btnConnect.getText().toString().contains("Connect"))
					new BTConnect().execute();
				else
					try {
						btSocket.close();
					} catch (IOException e) {
						e.printStackTrace();
					}

			}
		});
		btnPacket1.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				locoNetSendMessage3(0xB5, 0x01, 0x13);

			}
		});
		btnPacket2.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				if (trainIndex > 0)
					locoNetSendMessage3(
							0xBF,
							0x00,
							train_list[spnTrain.getSelectedItemPosition()].myIndex);
				else
					locoNetSendMessage3(0xBF, 0x00, 0x03);

			}
		});
		btnPacket3.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				locoNetSendMessage3(0xBA, 0x01, 0x01);

			}
		});
		btnLocoOn.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				locoNetSendMessage1(0x83);

			}
		});
		btnLocoOff.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				locoNetSendMessage1(0x82);

			}
		});
		btnPacket3.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				locoNetSendMessage3(0xBA, 0x01, 0x01);

			}
		});
		btnRezero.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				if (edtxtBarCode.getText().toString().length() != 0) {
					int barcode = Integer.parseInt(edtxtBarCode.getText()
							.toString());
					int trainID = train_list[spnTrain.getSelectedItemPosition()]
							.getMyIndex();
					// if (trainAddress[trainID] != NOT_VALID)
					barCodeReZero(trainID, barcode);
				}
			}
		});
		btnIMU.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				if (edtxtAngle.getText().toString().length() != 0
						&& edtxtTieCount.getText().toString().length() != 0) {
					int trainID = train_list[spnTrain.getSelectedItemPosition()]
							.getMyIndex();
					// if (trainNum[trainID] != NOT_VALID) {
					int tieCount = Integer.parseInt(edtxtTieCount.getText()
							.toString());

					int angle = Integer.parseInt(edtxtAngle.getText()
							.toString());
					updateTrain(trainID, tieCount, 0, 0, angle, 0);
					// }
				}
			}
		});

		addLayout.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				// in onCreate or any event where your want the user to
				// select a file
				Intent intent = new Intent();
				intent.setType("image/*");
				intent.setAction(Intent.ACTION_GET_CONTENT);
				startActivityForResult(
						Intent.createChooser(intent, "Select Picture"),
						SELECT_PICTURE);
			}
		});

		spnTrain.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> parentView,
					View selectedView, int pos, long id) {
				vertSeekBar.setProgressAndThumb(train_list[pos].myState);

			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
				// nothing selected
			}

		});
		spnSwitch.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> parentView,
					View selectedView, int pos, long id) {
				if (switch_list[pos].getMyState() == 0) {
					tglbtnSwitch.setChecked(false);
				} else {
					tglbtnSwitch.setChecked(true);
				}
				locoNetMessagePrepAndSend();

			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
				// nothing selected

			}

		});
		tglbtnSwitch.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				locoNetMessagePrepAndSend();

			}
		});

		gestureDetector = new GestureDetector(this, new GestureListener());

		// Drag Listener
		View.OnDragListener dragListener = new View.OnDragListener() {

			@Override
			public boolean onDrag(View targetView, DragEvent dragEvent) {
				int action = dragEvent.getAction();

				switch (action) {
				// Dropping item in any layout
				case DragEvent.ACTION_DROP:

					final ImageView originalButton = (ImageView) dragEvent
							.getLocalState();
					final RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
							originalButton.getWidth(),
							originalButton.getHeight());
					params.topMargin = (int) dragEvent.getY()
							- originalButton.getHeight() / 2;
					params.leftMargin = (int) dragEvent.getX()
							- originalButton.getWidth() / 2;
					ViewGroup parentView = (ViewGroup) originalButton
							.getParent();

					// Item is coming from Tray to Track Layout
					if (parentView.getTag().equals(TAG_TRAY)
							&& targetView.getTag().equals(TAG_TRACK)) {

						// TRAIN is coming to layout from Tray
						if (originalButton.getTag().equals(TAG_TRAINS)) {
							addTrainAt(params, originalButton.getTag()
									.toString());
						}
						// Switch is coming to layout from Tray
						else if (originalButton.getTag().equals(TAG_SWITCHES)) {
							addSwitchAt(params, originalButton.getTag()
									.toString());

						}
						// Barcode is coming to layout from Tray
						else if (originalButton.getTag().equals(TAG_BARCODES)) {
							addBarcodeAt(params, originalButton.getTag()
									.toString());
						}

					}
					// This means item is moving inside Track_layout.. dropping
					// item in same layout
					else if (parentView.getTag().equals(TAG_TRACK)
							&& targetView.getTag().equals(TAG_TRACK)) {

						final image_view inLayout = (image_view) dragEvent
								.getLocalState();
						inLayout.setLayoutParams(params);
						inLayout.setLeft(params.leftMargin);
						inLayout.setTop(params.topMargin);
						Toast.makeText(
								getBaseContext(),
								inLayout.getType() + " "
										+ inLayout.getMyIndex(),
								Toast.LENGTH_SHORT).show();
					}

					break;
				}
				return true;
			}
		};

		relLayout_Track.setOnDragListener(dragListener);
		relLayout_Tray.setOnDragListener(dragListener);

		vertSeekBar
				.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

					@Override
					public void onStopTrackingTouch(SeekBar seekBar) {
						if (trainIndex > 0) {
							train_list[spnTrain.getSelectedItemPosition()]
									.setMyState(vertSeekBar.getProgress());
							if (btconnected && btSocket != null
									&& btSocket.isConnected())
								dccMessagePrepAndSend();
						}
					}

					@Override
					public void onStartTrackingTouch(SeekBar seekBar) {

					}

					@Override
					public void onProgressChanged(SeekBar seekBar,
							int progress, boolean fromUser) {
						// if (btconnected && btSocket != null
						// && btSocket.isConnected())
						// dccMessagePrepAndSend();
					}
				});
	}

	private void init() {
		// get objects for train_list, switch_list and barcode_list
		train_list = new image_view[MAX_TRAINS];
		switch_list = new image_view[MAX_SWITCHES];
		barcode_list = new image_view[MAX_BARCODES];

		trainList = new ArrayList<String>();
		switchList = new ArrayList<String>();
		barcodeList = new ArrayList<String>();

		trainNum = new int[MAX_TRAINS];
		switchNum = new int[MAX_SWITCHES];
		barcodeNum = new int[MAX_BARCODES];

		trainAddress = new int[MAX_ADDRESS];
		switchAddress = new int[MAX_ADDRESS];
		barcodeAddress = new int[MAX_ADDRESS];

		for (int i = 0; i < speedArray.length; i++) {
			speedArray[i] = Integer.parseInt(stringSpeed[i], 2);
		}

		for (int i = 0; i < MAX_ADDRESS; i++) {
			trainAddress[i] = NOT_VALID;
			switchAddress[i] = NOT_VALID;
			barcodeAddress[i] = NOT_VALID;
		}

		for (int i = 0; i < MAX_TRAINS; i++) {
			trainNum[i] = NOT_VALID;
		}

		for (int i = 0; i < MAX_SWITCHES; i++) {
			switchNum[i] = NOT_VALID;
		}

		for (int i = 0; i < MAX_BARCODES; i++) {
			barcodeNum[i] = NOT_VALID;
		}

		for (int i = 0; i < 1024; i++) {
			inbuffer[i] = 0;
		}

	}

	protected void filters() {
		IntentFilter f1 = new IntentFilter(
				BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED);
		IntentFilter f2 = new IntentFilter(
				BluetoothDevice.ACTION_ACL_DISCONNECTED);
		IntentFilter f3 = new IntentFilter(BluetoothDevice.ACTION_ACL_CONNECTED);
		this.registerReceiver(bcastReceiver, f1);
		this.registerReceiver(bcastReceiver, f2);
		this.registerReceiver(bcastReceiver, f3);
	}

	private void BTSetup() {
		/*
		 * Bluetooth setup function. Grabs the default adapter of the device it
		 * is working on. If this adapter is null then the device does not have
		 * bluetooth. If the adapter is not enabled, then create an intent
		 * asking the user to enable bluetooth.
		 */
		btAdapter = BluetoothAdapter.getDefaultAdapter();
		if (btAdapter != null) {
			if (!btAdapter.isEnabled()) {
				Intent enableBluetooth = new Intent(
						BluetoothAdapter.ACTION_REQUEST_ENABLE);
				startActivityForResult(enableBluetooth, 0);
			} else {
				Log.d(TAG_DEBUG, "BT is Enabled.");
			}

		} else {
			Log.d(TAG_DEBUG, "BT is null");

		}
		filters();
	}

	/*
	 * End: onCreate Helper Functions connectWidgets(); setListeners();
	 * BTSetup(); init(); filters();
	 */

	/*
	 * Start: UI / Data Addition/Modification addSwitchAt(final
	 * RelativeLayout.LayoutParams params, final String tag); addTrainAt(final
	 * RelativeLayout.LayoutParams params, final String tag) addBarcodeAt(final
	 * RelativeLayout.LayoutParams params, final String tag) {
	 * setTrainImage(Context context, int value, int trainIndex)
	 * setSwitchImage(Context context, int value, int switchIndex)
	 * setBarCodeImage(Context context, int value, int barcodeIndex)
	 * userActionsDeleteOrModify(final image_view touchedObject)
	 * modifySelectedItem(image_view touchedObject) deletSelectedItem(image_view
	 * touchedObject) deleteBarcodeAt(image_view touchedObject)
	 * deleteSwitchAt(image_view touchedObject) deleteTrainAt(image_view
	 * touchedObject) sendStopAndDeleteMessageToMaster() removeBarcode(final int
	 * address, final image_view touchedObject) removeSwitch(final int address,
	 * final image_view touchedObject) removeTrain(final int address, final
	 * image_view touchedObject)
	 */
	public void addSwitchAt(final RelativeLayout.LayoutParams params,
			final String tag) {
		final EditText input = new EditText(MainActivity.this);
		if (switchIndex < MAX_SWITCHES) { // Checks to make sure the maximum
											// number
			// of trains hasn't been exceeded.
			new AlertDialog.Builder(MainActivity.this)
					.setTitle("Add A Switch")
					.setMessage("Please input the switch number")
					.setView(input)
					.setPositiveButton("Ok",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int whichButton) {
									int value;
									try {
										value = Integer.parseInt(input
												.getText().toString());
										if (value >= 0 && value <= 127) {
											if (switchAddress[value] == NOT_VALID) {

												// This is where switch is added
												// to switch_list array
												switch_list[switchIndex] = new image_view(
														getBaseContext());
												switch_list[switchIndex]
														.setLayoutParams(params);
												switch_list[switchIndex]
														.setLeft(params.leftMargin);
												switch_list[switchIndex]
														.setTop(params.topMargin);
												setSwitchImage(
														getBaseContext(),
														value, switchIndex);

												// switch_list[switchIndex].setImageBitmap(BitmapFactory.decodeResource(getApplicationContext().getResources(),
												// R.drawable.football));
												switch_list[switchIndex]
														.setMyIndex(value);
												switch_list[switchIndex]
														.setMyState(0);
												switch_list[switchIndex]
														.setMyType("switch");
												switch_list[switchIndex]
														.setOnLongClickListener(longClickListener);
												switch_list[switchIndex]
														.setTag(tag);

												switch_list[switchIndex]
														.setOnTouchListener(touchListener);
												switch_list[switchIndex]
														.setOnClickListener(switchClickListener);
												
												relLayout_Track
														.addView(switch_list[switchIndex]);

												switchNum[switchIndex] = value;
												switchAddress[value] = switchIndex;
												switchIndex++;
												switchList.add("Switch "
														+ value);

												// Need to add this part...
												// prepareIMUBuffer(value);
												/*
												 * track_Layout.setTrainImage(
												 * getBaseContext(), value,
												 * trainIndex);
												 * addTrainToLayout(value,
												 * true); trainIndex++;
												 * trainAdapter
												 * .notifyDataSetChanged();
												 */

												switchAdapter
														.notifyDataSetChanged();
												spnSwitch
														.setSelection(switchIndex);
												// locoNetMessagePrepAndSend();
											} else {
												Toast.makeText(
														getBaseContext(),
														"Switch already exists",
														Toast.LENGTH_SHORT)
														.show();

											}
										}

									} catch (NumberFormatException nfe) {
										System.out.println("Could not parse "
												+ nfe);
									}

								}
							})
					.setNegativeButton("Cancel",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int whichButton) {
									// Do nothing.
								}
							}).show();
		}
	}

	public void addTrainAt(final RelativeLayout.LayoutParams params,
			final String tag) {
		final EditText input = new EditText(MainActivity.this);
		if (trainIndex < MAX_TRAINS) { // Checks to make sure the maximum number
			// of trains hasn't been exceeded.
			new AlertDialog.Builder(MainActivity.this)
					.setTitle("Add A Train")
					.setMessage("Please input the train number")
					.setView(input)
					.setPositiveButton("Ok",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int whichButton) {
									int value;
									try {
										value = Integer.parseInt(input
												.getText().toString());
										if (value > 0 && value <= 127) {
											if (trainAddress[value] == NOT_VALID) {

												// This is where train is added
												// to train_list array
												train_list[trainIndex] = new image_view(
														getBaseContext());
												train_list[trainIndex]
														.setLayoutParams(params);
												train_list[trainIndex]
														.setLeft(params.leftMargin);
												train_list[trainIndex]
														.setTop(params.topMargin);
												train_list[trainIndex]
														.setMyState(speedArray.length);
												Toast.makeText(
														getBaseContext(),
														"x: "
																+ train_list[trainIndex]
																		.getX()
																+ " y: "
																+ train_list[trainIndex]
																		.getY(),
														Toast.LENGTH_SHORT)
														.show();
												// this function sets train's
												// image based on it's value,
												// currently just starting from
												// 1, 2, 3
												setTrainImage(getBaseContext(),
														value, trainIndex);
												train_list[trainIndex]
														.setMyIndex(value);
												prepareIMUBuffer(value);
												train_list[trainIndex]
														.setMyType("train");
												train_list[trainIndex]
														.setOnLongClickListener(longClickListener);
												train_list[trainIndex]
														.setTag(tag);
												train_list[trainIndex]
														.setOnTouchListener(touchListener);
												relLayout_Track
														.addView(train_list[trainIndex]);

												trainNum[trainIndex] = value;
												trainAddress[value] = trainIndex;

												trainIndex++;
												// prepareIMUBuffer(value);
												/*
												 * track_Layout.setTrainImage(
												 * getBaseContext(), value,
												 * trainIndex);
												 * addTrainToLayout(value,
												 * true); trainIndex++;
												 * trainAdapter
												 * .notifyDataSetChanged();
												 */
												trainList.add("Train " + value);

												trainAdapter
														.notifyDataSetChanged();
												spnTrain.setSelection(trainIndex);
												setScaledLayout();

											} else {
												Toast.makeText(
														getBaseContext(),
														"Train already exists",
														Toast.LENGTH_SHORT)
														.show();

											}
										}

									} catch (NumberFormatException nfe) {
										System.out.println("Could not parse "
												+ nfe);
									}

								}
							})
					.setNegativeButton("Cancel",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int whichButton) {
									// Do nothing.
								}
							}).show();
		}
	}

	public void addBarcodeAt(final RelativeLayout.LayoutParams params,
			final String tag) {
		// final EditText input = new EditText(MainActivity.this);

		LinearLayout layout = new LinearLayout(MainActivity.this);
		layout.setOrientation(LinearLayout.VERTICAL);

		final EditText input = new EditText(MainActivity.this);
		input.setHint("Barcode Number");
		input.setInputType(InputType.TYPE_NUMBER_FLAG_SIGNED);
		layout.addView(input);

		final EditText angleInput = new EditText(MainActivity.this);
		angleInput.setHint("Barcode angle in Degrees");
		angleInput.setInputType(InputType.TYPE_NUMBER_FLAG_SIGNED);
		layout.addView(angleInput);

		if (barcodeIndex < MAX_BARCODES) { // Checks to make sure the maximum
											// number
			// of trains hasn't been exceeded.
			new AlertDialog.Builder(MainActivity.this)
					.setTitle("Add A Barcode")
					.setMessage("Please input the barcode number")
					.setView(layout)
					.setPositiveButton("Ok",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int whichButton) {
									int value;
									int angle;
									try {
										value = Integer.parseInt(input
												.getText().toString());
										if (angleInput.getText().toString()
												.length() != 0)
											angle = Integer.parseInt(angleInput
													.getText().toString());
										else
											angle = -1;
										if (value > 0 && value <= 127) {
											if (barcodeAddress[value] == NOT_VALID) {

												// This is where barcode is
												// added to barcode_list array
												barcode_list[barcodeIndex] = new image_view(
														getBaseContext());
												barcode_list[barcodeIndex]
														.setLayoutParams(params);
												barcode_list[barcodeIndex]
														.setLeft(params.leftMargin);
												barcode_list[barcodeIndex]
														.setTop(params.topMargin);
												barcode_list[barcodeIndex]
														.setMyState(angle);
												setBarcodeImage(
														getBaseContext(),
														value, barcodeIndex);

												// barcode_list[barcodeIndex].setImageBitmap(BitmapFactory.decodeResource(getApplicationContext().getResources(),
												// R.drawable.ic_launcher));
												barcode_list[barcodeIndex]
														.setMyIndex(value);
												barcode_list[barcodeIndex]
														.setMyType("barcode");
												barcode_list[barcodeIndex]
														.setOnLongClickListener(longClickListener);
												barcode_list[barcodeIndex]
														.setTag(tag);
												barcode_list[barcodeIndex]
														.setOnTouchListener(touchListener);
												relLayout_Track
														.addView(barcode_list[barcodeIndex]);

												barcodeNum[barcodeIndex] = value;
												barcodeAddress[value] = barcodeIndex;
												barcodeIndex++;
												barcodeList.add("Barcode "
														+ value);

												// Need to add this part...
												// prepareIMUBuffer(value);
												/*
												 * track_Layout.setTrainImage(
												 * getBaseContext(), value,
												 * trainIndex);
												 * addTrainToLayout(value,
												 * true); trainIndex++;
												 * trainAdapter
												 * .notifyDataSetChanged();
												 */
											} else {
												Toast.makeText(
														getBaseContext(),
														"Barcode already exists",
														Toast.LENGTH_SHORT)
														.show();

											}
										}

									} catch (NumberFormatException nfe) {
										System.out.println("Could not parse "
												+ nfe);
									}

								}
							})
					.setNegativeButton("Cancel",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int whichButton) {
									// Do nothing.
								}
							}).show();

		}
	}

	// Simply sets train's image based on it's value. Currently, supporting only
	// 10
	public void setTrainImage(Context context, int value, int trainIndex) {

		TextDrawable txtDrawNumber = new TextDrawable(String.valueOf(value),
				Color.WHITE);
		Drawable drawableArray[] = new Drawable[] { train_background,
				train_foreground, txtDrawNumber };
		LayerDrawable layerDraw = new LayerDrawable(drawableArray);
		train_list[trainIndex].setImageDrawable(layerDraw);

	}

	// Setting switch's image... currently only 1 image is used. need to support
	// more images.
	public void setSwitchImage(Context context, int value, int switchIndex) {

		TextDrawable txtDrawNumber = new TextDrawable(String.valueOf(value),
				Color.DKGRAY);
		Drawable drawableArray[] = new Drawable[] { switch_background,
				switch_foreground, txtDrawNumber };
		LayerDrawable layerDraw = new LayerDrawable(drawableArray);
		switch_list[switchIndex].setImageDrawable(layerDraw);

	}
	
	public void setSwitchActive(Context context, int value, int switchIndex) {

		TextDrawable txtDrawNumber = new TextDrawable(String.valueOf(value),
				Color.DKGRAY);
		Drawable drawableArray[] = new Drawable[] { switch_alternative,
				switch_foreground, txtDrawNumber };
		LayerDrawable layerDraw = new LayerDrawable(drawableArray);
		switch_list[switchIndex].setImageDrawable(layerDraw);

	}

	public void setBarcodeImage(Context context, int value, int barcodeIndex) {

		TextDrawable txtDrawNumber = new TextDrawable(String.valueOf(value),
				Color.GREEN);
		Drawable drawableArray[] = new Drawable[] { barcode_background,
				barcode_foreground, txtDrawNumber };
		LayerDrawable layerDraw = new LayerDrawable(drawableArray);
		barcode_list[barcodeIndex].setImageDrawable(layerDraw);

	}

	public void selectTouchedObject(final image_view touchedObject) {
		if (touchedObject.getType() == "train") {
			// Toast.makeText(
			// getBaseContext(),
			// "Touching " + touchedObject.getType()
			// + touchedObject.getMyIndex(), Toast.LENGTH_SHORT)
			// .show();
			spnTrain.setSelection(trainAddress[touchedObject.myIndex]);
		} else if (touchedObject.getType() == "switch") {
			// Toast.makeText(
			// getBaseContext(),
			// "Touching " + touchedObject.getType()
			// + touchedObject.getMyIndex(), Toast.LENGTH_SHORT)
			// .show();
			int pos = switchAddress[touchedObject.myIndex];
			spnSwitch.setSelection(pos);
		} else if (touchedObject.getType() == "barcode") {
			Toast.makeText(
					getBaseContext(),
					"Touching " + touchedObject.getType()
							+ touchedObject.getMyIndex(), Toast.LENGTH_SHORT)
					.show();
		}
	}

	public void userActionsDeleteOrModify(final image_view touchedObject) {
		AlertDialog.Builder builderSingle = new AlertDialog.Builder(
				MainActivity.this);
		builderSingle.setIcon(R.drawable.ic_launcher);
		builderSingle.setTitle("Modyfy or Delete:-");
		final ArrayAdapter<String> userActionsAdapter = new ArrayAdapter<String>(
				MainActivity.this, android.R.layout.select_dialog_singlechoice);

		userActionsList = new ArrayList<String>();

		userActionsAdapter.add("Modify");
		userActionsList.add("Modify");
		userActionsAdapter.add("Delete");
		userActionsList.add("Delete");

		builderSingle.setNegativeButton("cancel",
				new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}
				});

		builderSingle.setAdapter(userActionsAdapter,
				new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						if (userActionsList.get(which) == "Modify") {
							modifySelectedItem(touchedObject);
						} else if (userActionsList.get(which) == "Delete") {
							deletSelectedItem(touchedObject);
						}
					}
				});
		builderSingle.show();
	}

	// Leaving this function for now.. If time permits, we will add this
	// fuctionality to modify train/barcode/switch address
	public void modifySelectedItem(image_view touchedObject) {
		if (touchedObject.getType() == "train") {
			Toast.makeText(
					getBaseContext(),
					"Modifying " + touchedObject.getType()
							+ touchedObject.getMyIndex(), Toast.LENGTH_SHORT)
					.show();
		} else if (touchedObject.getType() == "switch") {
			Toast.makeText(
					getBaseContext(),
					"Modifying " + touchedObject.getType()
							+ touchedObject.getMyIndex(), Toast.LENGTH_SHORT)
					.show();
		} else if (touchedObject.getType() == "barcode") {
			Toast.makeText(
					getBaseContext(),
					"Modifying " + touchedObject.getType()
							+ touchedObject.getMyIndex(), Toast.LENGTH_SHORT)
					.show();
		}
	}

	public void deletSelectedItem(image_view touchedObject) {
		if (touchedObject.getType() == "train") {
			Toast.makeText(
					getBaseContext(),
					"Deleting " + touchedObject.getType()
							+ touchedObject.getMyIndex(), Toast.LENGTH_SHORT)
					.show();
			deleteTrainAt(touchedObject);
		} else if (touchedObject.getType() == "switch") {
			Toast.makeText(
					getBaseContext(),
					"Deleting " + touchedObject.getType()
							+ touchedObject.getMyIndex(), Toast.LENGTH_SHORT)
					.show();
			deleteSwitchAt(touchedObject);
		} else if (touchedObject.getType() == "barcode") {
			Toast.makeText(
					getBaseContext(),
					"Deleting " + touchedObject.getType()
							+ touchedObject.getMyIndex(), Toast.LENGTH_SHORT)
					.show();
			deleteBarcodeAt(touchedObject);
		}
	}

	public void deleteBarcodeAt(image_view touchedObject) {
		removeBarcode(touchedObject.getMyIndex(), touchedObject);
	}

	public void deleteSwitchAt(image_view touchedObject) {
		removeSwitch(touchedObject.getMyIndex(), touchedObject);
	}

	public void deleteTrainAt(image_view touchedObject) {
		sendStopAndDeleteMessageToMaster();
		removeTrain(touchedObject.getMyIndex(), touchedObject);
	}

	public void sendStopAndDeleteMessageToMaster() {
		Toast.makeText(getBaseContext(),
				"Need to send a message to Master, Scott!! :) ",
				Toast.LENGTH_SHORT).show();
		// First send stop message to the train
		// Second send delete message to the train
	}

	public void removeBarcode(final int address, final image_view touchedObject) {
		new AlertDialog.Builder(this)
				.setIcon(android.R.drawable.ic_dialog_alert)
				.setTitle(getString(R.string.confirmDelete) + "Barcode?")
				.setMessage(
						getString(R.string.barcodeDelete)
								+ String.valueOf(address))
				.setPositiveButton(R.string.confirmDelete,
						new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								int removeIndex = barcodeAddress[address];

								ViewGroup parentView = (ViewGroup) touchedObject
										.getParent();
								parentView.removeView(touchedObject);
								for (int i = removeIndex; i < barcodeIndex; i++) {
									if (barcodeNum[i + 1] != NOT_VALID) {

										barcodeNum[i] = barcodeNum[i + 1];
										barcodeAddress[barcodeNum[i]] = i;
										barcode_list[i] = barcode_list[i + 1];
										barcodeList.set(i,
												barcodeList.get(i + 1));
									} else {
										barcodeAddress[address] = NOT_VALID;
										barcodeNum[i] = NOT_VALID;
										barcodeList.remove(i);
									}
								}
								barcodeIndex--;
								// trainAdapter.notifyDataSetChanged();
							}

						}).setNegativeButton(R.string.denyDelete, null).show();

	}

	public void removeSwitch(final int address, final image_view touchedObject) {
		new AlertDialog.Builder(this)
				.setIcon(android.R.drawable.ic_dialog_alert)
				.setTitle(getString(R.string.confirmDelete) + "Switch?")
				.setMessage(
						getString(R.string.switchDelete)
								+ String.valueOf(address))
				.setPositiveButton(R.string.confirmDelete,
						new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								int removeIndex = switchAddress[address];

								ViewGroup parentView = (ViewGroup) touchedObject
										.getParent();
								parentView.removeView(touchedObject);
								for (int i = removeIndex; i < switchIndex; i++) {
									if (switchNum[i + 1] != NOT_VALID) {

										switchNum[i] = switchNum[i + 1];
										switchAddress[switchNum[i]] = i;
										switch_list[i] = switch_list[i + 1];
										switchList.set(i, switchList.get(i + 1));
									} else {
										switchAddress[address] = NOT_VALID;
										switchNum[i] = NOT_VALID;
										switchList.remove(i);
									}
								}
								switchIndex--;
								switchAdapter.notifyDataSetChanged();
							}

						}).setNegativeButton(R.string.denyDelete, null).show();

	}

	public void removeTrain(final int address, final image_view touchedObject) {

		new AlertDialog.Builder(this)
				.setIcon(android.R.drawable.ic_dialog_alert)
				.setTitle(getString(R.string.confirmDelete) + "Train?")
				.setMessage(
						getString(R.string.trainDelete)
								+ String.valueOf(address))
				.setPositiveButton(R.string.confirmDelete,
						new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								int removeIndex = trainAddress[address];

								ViewGroup parentView = (ViewGroup) touchedObject
										.getParent();
								parentView.removeView(touchedObject);
								for (int i = removeIndex; i < trainIndex; i++) {
									if (trainNum[i + 1] != NOT_VALID) {

										trainNum[i] = trainNum[i + 1];
										trainAddress[trainNum[i]] = i;
										train_list[i] = train_list[i + 1];
										trainList.set(i, trainList.get(i + 1));
									} else {
										trainAddress[address] = NOT_VALID;
										trainNum[i] = NOT_VALID;
										trainList.remove(i);
									}
								}
								trainIndex--;
								if (btSocket != null && btconnected)
									sendDCCMessage(address, 64, address ^ 64);
								trainAdapter.notifyDataSetChanged();
							}

						}).setNegativeButton(R.string.denyDelete, null).show();

	}

	/*
	 * End: UI / Data Addition/Modification addSwitchAt(final
	 * RelativeLayout.LayoutParams params, final String tag); addTrainAt(final
	 * RelativeLayout.LayoutParams params, final String tag) addBarcodeAt(final
	 * RelativeLayout.LayoutParams params, final String tag) {
	 * setTrainImage(Context context, int value, int trainIndex)
	 * setSwitchImage(Context context, int value, int switchIndex)
	 * userActionsDeleteOrModify(final image_view touchedObject)
	 * modifySelectedItem(image_view touchedObject) deletSelectedItem(image_view
	 * touchedObject) deleteBarcodeAt(image_view touchedObject)
	 * deleteSwitchAt(image_view touchedObject) deleteTrainAt(image_view
	 * touchedObject) sendStopAndDeleteMessageToMaster() removeBarcode(final int
	 * address, final image_view touchedObject) removeSwitch(final int address,
	 * final image_view touchedObject) removeTrain(final int address, final
	 * image_view touchedObject)
	 */

	/*
	 * Start: DCC/LocoNet Communication Helpers dccMessagePrepAndSend()
	 * locoNetMessagePrepAndSend()
	 */
	public void dccMessagePrepAndSend() {
		if (btconnected && btSocket != null && btSocket.isConnected()) {
			// check to see if it is not null, then see if it is connected
			int address = 0;
			int speed = 0;
			int commandbits = 0;

			address = trainNum[spnTrain.getSelectedItemPosition()];
			speed = Math.abs(vertSeekBar.getProgress() - vertSeekBar.getMax()
					/ 2);
			train_list[trainAddress[address]].setMyState(vertSeekBar
					.getProgress());
			Log.d(TAG_DEBUG, "Speed progress bar: " + speed);
			commandbits = speedArray[speed];
			Log.d(TAG_DEBUG, "Speed byte: " + speed);

			if (vertSeekBar.getProgress() > vertSeekBar.getMax() / 2)
				commandbits ^= 96; // 64 is for the 01 in the packet format
			// 32 for the forward direction
			else
				commandbits ^= 64;

			int checksum = address ^ commandbits;
			try {
				sendDCCMessage(address, commandbits, checksum);
			} finally {
				Log.d(TAG_DEBUG, "Sent Message");
			}

		} else
			Log.d(TAG_DEBUG, "Bluetooth Not Connected!");
	}

	private void sendDCCMessage(int address, int command, int checksum) {
		int dcc = 0;
		Log.d(TAG_DEBUG,
				"Sending... \" address# 0x" + Integer.toHexString(address)
						+ " commandbits: 0x" + Integer.toHexString(command)
						+ " checksum: 0x" + Integer.toHexString(checksum)
						+ "\".");
		byte[] outbuffer = new byte[4];
		outbuffer[0] = (byte) (dcc & 0xff);
		outbuffer[1] = (byte) (address & 0xff);
		outbuffer[2] = (byte) (command & 0xff);
		outbuffer[3] = (byte) (checksum & 0xff);
		Log.d(TAG_DEBUG,
				"Sending... \" address# 0x" + Integer.toHexString(outbuffer[1])
						+ " commandbits: 0x"
						+ Integer.toHexString(outbuffer[2]) + " checksum: 0x"
						+ Integer.toHexString(outbuffer[3]) + "\".");

		if (outbuffer != null)
			try {
				btOutStream.write(outbuffer);
				btOutStream.flush();
				// btOutStream.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
	}

	private void locoNetSendMessage1(int out1) {
		int loconet = 0x01;
		int checksum = 0;

		byte[] outbuffer = new byte[3];

		if (btconnected && btSocket != null && btSocket.isConnected()) {

			checksum = ~((out1 & 0xff));

			outbuffer[0] = (byte) (loconet & 0xff);
			outbuffer[1] = (byte) (out1 & 0xff);
			outbuffer[2] = (byte) (checksum & 0xff);

		}
		if (outbuffer != null)
			try {
				btOutStream.write(outbuffer);
				btOutStream.flush();
				// btOutStream.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
	}

	private void locoNetSendMessage3(int out1, int out2, int out3) {
		int loconet = 0x01;
		int checksum = 0;

		byte[] outbuffer = new byte[5];

		if (btconnected && btSocket != null && btSocket.isConnected()) {

			checksum = ~(((out1 & 0xff) ^ (out2 & 0xff)) ^ (out3 & 0xff));

			outbuffer[0] = (byte) (loconet & 0xff);
			outbuffer[1] = (byte) (out1 & 0xff);
			outbuffer[2] = (byte) (out2 & 0xff);
			outbuffer[3] = (byte) (out3 & 0xff);
			outbuffer[4] = (byte) (checksum & 0xff);

		}
		if (outbuffer != null)
			try {
				btOutStream.write(outbuffer);
				btOutStream.flush();
				// btOutStream.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
	}

	private void locoNetMessagePrepAndSend() {
		int loconet = 0x01;
		int opcode = 0xB0;
		int address = 0;
		int command = 0;
		int checksum = 0;
		int endbyte = 0x0a;
		int simpleSwitchAddress = 0;
		boolean switchState = tglbtnSwitch.isChecked();
		if (switchState){
			switch_list[spnSwitch.getSelectedItemPosition()].setMyState(1);
			setSwitchActive(getBaseContext(), switch_list[spnSwitch.getSelectedItemPosition()].myIndex, spnSwitch.getSelectedItemPosition());
		}
		else{
			switch_list[spnSwitch.getSelectedItemPosition()].setMyState(0);
			setSwitchImage(getBaseContext(), switch_list[spnSwitch.getSelectedItemPosition()].myIndex, spnSwitch.getSelectedItemPosition());
		}
		byte[] outbuffer = new byte[6];

		if (btconnected && btSocket != null && btSocket.isConnected()) {

			// check to see if it is not null, then see if it is connected

			simpleSwitchAddress = switchNum[spnSwitch.getSelectedItemPosition()];
			address = simpleSwitchAddress & 0x7f;
			command = ((simpleSwitchAddress >> 7) & 0x0f);
			if (tglbtnSwitch.isChecked()) {
				command = (0x30 | (command & 0x0f));
				switch_list[switchAddress[simpleSwitchAddress]].setMyState(1);
			} else {
				command = (0x10 | (command & 0x0f));
				switch_list[switchAddress[simpleSwitchAddress]].setMyState(0);
			}

			checksum = ~(((opcode & 0xff) ^ (address & 0xff)) ^ (command & 0xff));

			outbuffer[0] = (byte) (loconet & 0xff);
			outbuffer[1] = (byte) (opcode & 0xff);
			outbuffer[2] = (byte) (address & 0xff);
			outbuffer[3] = (byte) (command & 0xff);
			outbuffer[4] = (byte) (checksum & 0xff);
			outbuffer[5] = (byte) (endbyte & 0xff);
			Log.d(TAG_DEBUG,
					"Message Type: " + Integer.toHexString(outbuffer[0])
							+ " OpCode: " + Integer.toHexString(outbuffer[1])
							+ " Address: " + Integer.toHexString(outbuffer[2])
							+ " Command: " + Integer.toHexString(outbuffer[3])
							+ " Checksum: " + Integer.toHexString(outbuffer[4])
							+ " End Byte: " + Integer.toHexString(outbuffer[5]));

			if (outbuffer != null)
				try {
					btOutStream.write(outbuffer);
					btOutStream.flush();
					// btOutStream.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
		}
	}

	// IMU Handling

	public void prepareIMUBuffer(int trainID) {
		for (int i = 0; i < 4; i++) {
			imuBuffer[trainID][0][i] = 0;
			imuBuffer[trainID][0][i + 4] = 0;
			imuBuffer[trainID][0][i + 8] = 0;
			imuBuffer[trainID][0][i + 12] = 0;
			imuBuffer[trainID][0][i + 16] = 0;
			imuBuffer[trainID][0][i + 20] = 0;
			imuBuffer[trainID][0][i + 24] = 0;
		}
		for (int i = 0; i < 4; i++) {
			imuBuffer[trainID][1][i] = 0;
			imuBuffer[trainID][1][i + 4] = 0;
			imuBuffer[trainID][1][i + 8] = 0;
			imuBuffer[trainID][1][i + 12] = 0;
			imuBuffer[trainID][1][i + 16] = 0;
			imuBuffer[trainID][1][i + 20] = 0;
			imuBuffer[trainID][1][i + 24] = 0;
		}
	}

	private void createIMUThread() { // rename createBTRecieveThread
		Log.d(TAG_DEBUG, "Creating IMU Thread");
		if (IMUThread == null)
			IMUThread = new Thread(new Runnable() {
				public void run() {

					boolean excep = false;
					while (!excep) {

						// Log.d(TAG_DEBUG, "currentRunning is: " +
						// currentRunning
						// + " btsocket is : " + (btSocket != null));

						// Keep listening to the InputStream until an
						// exception
						// occurs
						// receive thread should look at first byte of the
						// message.
						// depending on first byte it should either prepare
						// to
						// receive
						// current data, or location/imu data.
						// so it check available, read the first byte,
						// switch on
						// that byte
						// waiting on the correct number of bytes to be
						// available for that type
						// it should then put that incoming message into a
						// buffer to be worked on
						// and raise a flag or spawn a worker thread.

						// Log.d(TAG_DEBUG, "currentRunning before try");
						try {

							if (/* btconnected && */btInStream != null
									&& btSocket != null) {

								// Log.d(TAG_DEBUG, "currentRunning true");
								// Thread.sleep(10);
								// int actualReceivedBytesFirstTime =
								// btInStream.read(inbuffer);
								// // while (actualReceivedBytesFirstTime != -1)
								// // actualReceivedBytesFirstTime =
								// btInStream.read(inbuffer);
								// int actualReceivedBytesSecondTime = 0;
								byte readByte = (byte) (btInStream.read() & 0xff);
								while (readByte != 10) {
									readByte = (byte) (btInStream.read() & 0xff);

								}
								inbuffer[0] = 10;
								for (int i = 1; i < IMU_DATA_SIZE; i++) {
									inbuffer[i] = (byte) (btInStream.read() & 0xff);
								}
								byte checksum = 0;
								for (int i = 0; i < IMU_DATA_SIZE - 1; i++) {
									checksum ^= inbuffer[i];
								}

								if (checksum != inbuffer[IMU_DATA_SIZE - 1])
									Log.d(TAG_DEBUG, "Checksum is invalid");
								else {
									Log.d(TAG_DEBUG, "Checksum is valid");

									/*
									 * if(actualReceivedBytesFirstTime <
									 * IMU_DATA_SIZE) {
									 * actualReceivedBytesSecondTime =
									 * btInStream.read(inbuffer,
									 * actualReceivedBytesFirstTime
									 * ,IMU_DATA_SIZE
									 * -actualReceivedBytesFirstTime);
									 * 
									 * }
									 */

									// int totalBytes =
									// actualReceivedBytesFirstTime
									// + actualReceivedBytesSecondTime;

									Log.d(TAG_DEBUG,
											"currentRunning receieved a "
													+ readByte + " byte");

									// ensuring IMU_header and train 3 are
									// received
									// in sequence.
									if (inbuffer[0] == IMU_HEADER) {
										Log.d(TAG_DEBUG,
												"currentRunning Received IMU data.");
										int imuTrainID = ((int) (inbuffer[1])) & 0xff;
										for (int i = 0; i < IMU_DATA_SIZE - 2; i++)
											imuBuffer[imuTrainID][0][i] = inbuffer[i + 2];
										imuBufferIndex++;
										// spawn current calc thread.
										updateIMU(imuTrainID);
									}

									/*
									 * bytes = btInStream.available(); // byte[]
									 * header = new byte[1];
									 * 
									 * 
									 * if (bytes >= IMU_DATA_SIZE) {
									 * Log.d(TAG_DEBUG,
									 * "Inside createIMUThread and bytes are: "
									 * + bytes);
									 * 
									 * // receive first byte inbuffer = new
									 * byte[IMU_DATA_SIZE]; // Log.d(TAG_DEBUG,
									 * "number of bytes " + // bytes);
									 * btInStream.read(inbuffer, 0,
									 * IMU_DATA_SIZE);
									 * 
									 * if (bytes >= IMU_DATA_SIZE && inbuffer[0]
									 * == IMU_HEADER) { // inbuffer = new
									 * byte[bytes];
									 * 
									 * Log.d(TAG_DEBUG, "Received IMU data.");
									 * // btInStream.read(inbuffer, 0, //
									 * bytes); int imuTrainID = ((int)
									 * (inbuffer[1])) & 0xff; for (int i = 0; i
									 * < IMU_DATA_SIZE - 2; i++)
									 * imuBuffer[imuTrainID][0][i] = inbuffer[i
									 * + 2]; imuBufferIndex++; // spawn current
									 * calc thread. updateIMU(imuTrainID); }
									 * 
									 * }
									 */
									for (int i = 0; i < IMU_DATA_SIZE; i++) {
										inbuffer[i] = 0;
									}
								}
							} else {
								// Log.d(TAG_DEBUG, "currentRunning false");
							}

						} catch (IOException e) {
							Log.d(TAG_DEBUG, "Exception in IMU Thread");
							excep = true;
							e.printStackTrace();
						}
					}
				}

			});
		Log.d(TAG_DEBUG, "Thread exiting");

	}

	public void updateTrain(int trainID, int tieCount, float xAccel,
			float yAccel, float yawGyro, float yawDifference) {
		// once used relLayout_Track for movement, now need to use
		// image_view_track_layout
		// trainLayoutHeight = image_view_track_layout.getMeasuredHeight();
		// trainLayoutWidth = image_view_track_layout.getMeasuredWidth();
		// Log.d(TAG_DEBUG, "Tracklayout height is: " + trainLayoutHeight +
		// " Tracklayout width is: " + trainLayoutWidth);
		// heightOfLayout & widthOfLayout is in Inches, * 2.54 to get to cm.
		// we move pixels when moving so we need to find the ratio of pixels per
		// cm.
		// degrees to radians is pi/180 thus the 0.017453

		final int index = trainAddress[trainID];
		if (index != -1) {
			Log.d(TAG_DEBUG, "Train in list #" + trainID);

			// int yawNeg = 1;

			// if (yawGyro < 0)
			// yawNeg = -1;
			//
			// if (yawNeg == -1)
			// yawGyro *= -1;

			float direction = 1;
			if ((train_list[index].getMyState()) < speedArray.length) {
				Log.d(TAG_DEBUG, "Setting reverse");
				// direction = -1;
				yawGyro = yawGyro + 180;
			}
			// else
			// {
			// direction = -1;
			// yawGyro = yawGyro + 180;
			// }
			float xCord = 0;
			float yCord = 0;
			// if (Math.abs(yawGyro) < 6) {
			// small angle approximation
			float radianAngle = (float) (yawGyro * 0.01745);
			// ruler measured big track
			// float distance = (float) (0.36585 * tieCount * direction);

			// caliper measured big track.
			float distance = (float) (0.36979 * tieCount * direction);

			// round track layout size
			// float distance = (float) (0.34287 * tieCount * direction);

			// old approximation
			double cos = Math.cos(radianAngle);
			xCord = (float) ((double) distance * cos * trainLayoutScaleWidth);
			if (train_list[index].getPrevX() == 0)
				xCord += train_list[index].getX();
			else
				xCord += train_list[index].getPrevX();
			train_list[index].setPrevX(xCord);
			// if (yawNeg == 1) {
			double sin = Math.sin(radianAngle);
			yCord = (float) ((double) distance * sin * trainLayoutScaleHeight);
			yCord = Math.round(yCord);
			// Log.d(TAG_DEBUG,
			// "Sine of angle: " + yawGyro + " Sin is: "
			// + Math.sin(radianAngle));
			if (train_list[index].getPrevY() == 0)
				yCord += train_list[index].getY();
			else
				yCord += train_list[index].getPrevY();
			train_list[index].setPrevY(yCord);
			xCord = Math.round(xCord);
			yCord = Math.round(yCord);
			// } else {
			// double sin = Math.sin(radianAngle);
			// yCord = (float) (distance * sin * trainLayoutScaleHeight);
			// Log.d(TAG_DEBUG, "Sine of angle: " + yawGyro + " Sin is: " +
			// Math.sin(radianAngle));
			// yCord = Math.round(yCord);
			// yCord = train_list[index].getY() - yCord;
			// }
			// cos approx: cos theta = 1 - (theta^2)/2
			// xCord = (float) (distance
			// * (1 - (radianAngle * radianAngle) / 2) *
			// trainLayoutScaleWidth);
			// xCord += train_list[index].getX();
			// // sin approx: sin theta = theta
			// yCord = (float) (distance * radianAngle *
			// trainLayoutScaleHeight);
			// yCord += train_list[index].getY();
			/*
			 * } else { float arcLength = (float) (0.36585366 * (float)
			 * tieCount); float radianAngle = (float) (yawGyro * 0.0174532925);
			 * float distance = (float) (arcLength / radianAngle) * direction;
			 * xCord = (float) (distance * Math.cos(radianAngle) *
			 * trainLayoutScaleWidth); xCord += train_list[index].getX(); yCord
			 * = (float) (distance * Math.sin(radianAngle) *
			 * trainLayoutScaleHeight); yCord += train_list[index].getY(); }
			 */

			Log.d(TAG_DEBUG, "Moving train #" + trainID + " from x: "
					+ train_list[index].getX() + " to x: " + xCord
					+ ". Moving from y:" + train_list[index].getY() + " to y: "
					+ yCord);
			final RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
					train_list[index].getWidth(), train_list[index].getHeight());
			params.height = px;
			params.width = px;
			params.topMargin = (int) yCord;
			params.leftMargin = (int) xCord;
			train_list[index].post(new Runnable() {

				@Override
				public void run() {
					train_list[index].setLayoutParams(params);
					train_list[index].setLeft(params.leftMargin);
					train_list[index].setTop(params.topMargin);
					train_list[index].bringToFront();
					train_list[index].invalidate();
					train_list[index].requestLayout();
				}
			});

		}
	}

	private void updateIMU(int imuTrainID) {
		// new Thread(new Runnable() {
		// public void run() {
		Log.d(TAG_DEBUG, "Inside updateIMU function.");
		byte[] acelXBytes = new byte[4];
		byte[] acelYBytes = new byte[4];
		byte[] acelZBytes = new byte[4];
		byte[] gyroYawBytes = new byte[4];
		byte[] gyroPitchBytes = new byte[4];
		byte[] gyroRollBytes = new byte[4];
		byte[] tieCountBytes = new byte[4];
		byte[] prevGyroYawBytes = new byte[4];
		byte[] prevTieCountBytes = new byte[4];
		byte[] barCodeBytes = new byte[1];

		for (int i = 0; i < 4; i++) {
			acelXBytes[i] = imuBuffer[imuTrainID][0][i];
			acelYBytes[i] = imuBuffer[imuTrainID][0][i + 4];
			acelZBytes[i] = imuBuffer[imuTrainID][0][i + 8];
			gyroRollBytes[i] = imuBuffer[imuTrainID][0][i + 12];
			gyroPitchBytes[i] = imuBuffer[imuTrainID][0][i + 16];
			gyroYawBytes[i] = imuBuffer[imuTrainID][0][i + 20];
			prevGyroYawBytes[i] = imuBuffer[imuTrainID][1][i + 20];
			tieCountBytes[i] = imuBuffer[imuTrainID][0][i + 24];

		}

		barCodeBytes[0] = imuBuffer[imuTrainID][0][28];

		final float acelX = ByteBuffer.wrap(acelXBytes)
				.order(ByteOrder.LITTLE_ENDIAN).getFloat();
		final float acelY = ByteBuffer.wrap(acelYBytes)
				.order(ByteOrder.LITTLE_ENDIAN).getFloat();
		final float acelZ = ByteBuffer.wrap(acelZBytes)
				.order(ByteOrder.LITTLE_ENDIAN).getFloat();

		final float gyroYaw = ByteBuffer.wrap(gyroYawBytes)
				.order(ByteOrder.LITTLE_ENDIAN).getFloat();

		final float gyroPitch = ByteBuffer.wrap(gyroPitchBytes)
				.order(ByteOrder.LITTLE_ENDIAN).getFloat();

		final float gyroRoll = ByteBuffer.wrap(gyroRollBytes)
				.order(ByteOrder.LITTLE_ENDIAN).getFloat();

		final float prevGyroYaw = ByteBuffer.wrap(prevGyroYawBytes)
				.order(ByteOrder.LITTLE_ENDIAN).getFloat();

		final int imuBarCode = (barCodeBytes[0] & 0xff);

		if (imuBarCode != 0) {
			int barCodeYaw = barCodeReZero(imuTrainID, imuBarCode);
			Log.d(TAG_DEBUG, "IMU Barcode: " + imuBarCode);
			if (barCodeYaw != -1)
				yawOffset = (int) (gyroYaw - barCodeYaw);
		}

		float calibratedYaw = (gyroYaw - yawOffset);
		Log.d(TAG_DEBUG, "Yaw: gyro:" + gyroYaw + " calibrated: "
				+ calibratedYaw + " offset: " + yawOffset);
		for (int i = 0; i < 4; i++)
			prevTieCountBytes[i] = imuBuffer[imuTrainID][1][i + 24];

		final int curTieCount = (ByteBuffer.wrap(tieCountBytes).order(
				ByteOrder.LITTLE_ENDIAN).getInt());
		final int prevTieCount = (ByteBuffer.wrap(prevTieCountBytes).order(
				ByteOrder.LITTLE_ENDIAN).getInt());

		int tieCount = curTieCount - prevTieCount;
		if (tieCount < 0)
			tieCount = curTieCount;

		Log.d(TAG_DEBUG,
				"currentRunning Train #" + imuTrainID + ": acel x"
						+ String.valueOf(acelX) + ", acel y "
						+ String.valueOf(acelY) + ", acel z "
						+ String.valueOf(acelZ) + ", gyroYaw "
						+ String.valueOf(gyroYaw) + ", gyroPitch "
						+ String.valueOf(gyroPitch) + ", gyroRoll "
						+ String.valueOf(gyroRoll) + ", prevTieCount "
						+ String.valueOf(prevTieCount) + ", curTieCount "
						+ String.valueOf(curTieCount) + ", tieCountDiff "
						+ String.valueOf(tieCount) + ", imuBarCode "
						+ String.valueOf(imuBarCode));

		updateTrain(imuTrainID, tieCount, acelX, acelY, calibratedYaw,
				(gyroYaw - prevGyroYaw));

		for (int i = 0; i < 4; i++) {
			imuBuffer[imuTrainID][1][i + 20] = imuBuffer[imuTrainID][0][i + 20];
			imuBuffer[imuTrainID][1][i + 24] = imuBuffer[imuTrainID][0][i + 24];
		}

		/*
		 * txtvwStatus.post(new Runnable() {
		 * 
		 * @Override public void run() { txtvwStatus.setText("Train #" +
		 * imuTrainID + ": acel x" + String.valueOf(acelX) + ", acel y " +
		 * String.valueOf(acelY) + ", acel z " + String.valueOf(acelZ) +
		 * ", gyroYaw " + String.valueOf(gyroYaw) + ", gyroPitch " +
		 * String.valueOf(gyroPitch) + ", gyroRoll " + String.valueOf(gyroRoll)
		 * + ", tieCount " + String.valueOf(tieCount) + ", imuBarCode " +
		 * String.valueOf(imuBarCode));
		 * 
		 * } });
		 */

	}

	int barCodeReZero(final int imuTrainID, final int barCode) {

		for (int i = 0; i < 4; i++)
			imuBuffer[imuTrainID][1][i + 24] = 0;
		Log.d(TAG_DEBUG, "Barcode is: " + barCode);
		if (barcodeIndex > 0 && barCode != 3 && barCode != 4) {
			if (barcodeAddress[barCode] != -1) {
				final int left = barcode_list[barcodeAddress[barCode]]
						.getLeft();
				final int top = barcode_list[barcodeAddress[barCode]].getTop();
				// train_list[trainAddress[imuTrainID]].setLeft(left);
				// train_list[trainAddress[imuTrainID]].setTop(top);

				final RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
						train_list[barcodeAddress[barCode]].getWidth(),
						train_list[barcodeAddress[barCode]].getHeight());
				params.height = px;
				params.width = px;
				params.topMargin = top;
				params.leftMargin = left;
				train_list[barcodeAddress[barCode]].post(new Runnable() {

					@Override
					public void run() {
						train_list[barcodeAddress[barCode]]
								.setLayoutParams(params);
						train_list[barcodeAddress[barCode]]
								.setLeft(params.leftMargin);
						train_list[barcodeAddress[barCode]]
								.setTop(params.topMargin);
						train_list[barcodeAddress[barCode]].bringToFront();
						train_list[barcodeAddress[barCode]].invalidate();
						train_list[barcodeAddress[barCode]].requestLayout();
					}
				});

				return barcode_list[barcodeAddress[barCode]].getMyState();
			}
		}
		return -1;

	}

	// Android Lifecycle Management
	protected void onDestroy() {
		if (btconnected && btSocket != null && btSocket.isConnected()) { // check
			// to
			// see
			// if
			// it
			// is
			// not
			// null,
			// then
			// see
			// if
			// it
			// is
			// connected.

			// handler.removeCallbacks(currentRun);
			try {
				currentRunning = false;
				Log.d(TAG_DEBUG, "currentRunning onDestroy");
				Thread.sleep(5);

				if (btSocket != null)
					btSocket.close();
				if (btInStream != null)
					btInStream.close();
				if (btOutStream != null)
					btOutStream.close();

			} catch (IOException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

		}

		unregisterReceiver(bcastReceiver);
		super.onDestroy();
	}

	@Override
	public void onResume() {
		super.onResume();
		if (IMUThread == null) {
			createIMUThread();
			IMUThread.start();
		}
		Log.d(TAG_DEBUG, "...onResume - try connect...");

		// Set up a pointer to the remote node using it's address.
		/*
		 * if (address != null) {
		 * 
		 * new btmakeconnection().execute();
		 * 
		 * }
		 */
	}

	@Override
	public void onPause() {
		if (IMUThread != null)
			Log.d(TAG_DEBUG,
					"...In onPause()... thread is: " + IMUThread.isAlive());
		if (btOutStream != null) {
			try {
				btOutStream.flush();
			} catch (IOException e) {

			}
		}
		/*
		 * if (!exitingNow && btSocket != null) try { Log.d(TAG_DEBUG,
		 * "Closing socket. thread is: " + currentThread.isAlive());
		 * btSocket.close(); Log.d(TAG_DEBUG, "Closed socket. thread is: " +
		 * currentThread.isAlive()); } catch (IOException e2) { Log.d(TAG_DEBUG,
		 * "Closing socket exception caught thread is: " +
		 * currentThread.isAlive()); }
		 */

		super.onPause();
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			// Ask the user if they want to quit
			new AlertDialog.Builder(this)
					.setIcon(android.R.drawable.ic_dialog_alert)
					.setTitle(R.string.quit)
					.setMessage(R.string.really_quit)
					.setPositiveButton(R.string.yes,
							new DialogInterface.OnClickListener() {

								@Override
								public void onClick(DialogInterface dialog,
										int which) {
									exitingNow = true;
									if (btSocket != null && btconnected) {
										sendDCCMessage(0, 0, 0);
										try {
											btOutStream.flush();
										} catch (IOException e1) {
											e1.printStackTrace();
										}
									}
									// Stop the activity
									MainActivity.this.finish();
								}

							}).setNegativeButton(R.string.no, null).show();

			return true;
		} else {
			return super.onKeyDown(keyCode, event);
		}
	}

	// UPDATED
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode == Activity.RESULT_OK) {
			if (requestCode == SELECT_PICTURE) {
				Uri selectedImageUri = data.getData();

				// OI FILE Manager
				// filemanagerstring = selectedImageUri.getPath();

				// MEDIA GALLERY

				image_view_track_layout.setImageURI(selectedImageUri);
				selectedImagePath = getPath(selectedImageUri);
				imagePath.getBytes();
				// TextView txt = (TextView)findViewById(R.id.title);
				// txt.setText(imagePath.toString());

				@SuppressWarnings("unused")
				Bitmap bm = BitmapFactory.decodeFile(imagePath);

				// img1.setImageBitmap(bm);

				getHeightAndWidthOfLayout();

			}

		}

	}

	/*
	 * //UPDATED
	 * 
	 * @Override public void onActivityResult(int requestCode, int resultCode,
	 * Intent data) { if (resultCode == Activity.RESULT_OK) { if (requestCode ==
	 * SELECT_PICTURE) { Uri selectedImageUri = data.getData();
	 * 
	 * //MEDIA GALLERY selectedImagePath = getPath(selectedImageUri);
	 * 
	 * BitmapDrawable background = new BitmapDrawable(imagePath.toString());
	 * relLayout_Track.setBackground(background);
	 * 
	 * getHeightAndWidthOfLayout(); } } }
	 */

	public void getHeightAndWidthOfLayout() {
		// final EditText input = new EditText(MainActivity.this);

		LinearLayout layout = new LinearLayout(MainActivity.this);
		layout.setOrientation(LinearLayout.VERTICAL);

		final EditText heightInput = new EditText(MainActivity.this);
		heightInput.setHint("Height in Inches");
		heightInput.setInputType(InputType.TYPE_NUMBER_FLAG_SIGNED);
		heightInput.setText("96");
		layout.addView(heightInput);

		final EditText widthInput = new EditText(MainActivity.this);
		widthInput.setHint("Width in Inches");
		widthInput.setInputType(InputType.TYPE_NUMBER_FLAG_SIGNED);
		widthInput.setText("192");
		layout.addView(widthInput);

		new AlertDialog.Builder(MainActivity.this)
				.setTitle("Add Height and width of Image")
				.setMessage("Please Input the Height and Width")
				.setView(layout)
				.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						try {
							heightOfLayout = Double.parseDouble(heightInput
									.getText().toString());
							widthOfLayout = Double.parseDouble(widthInput
									.getText().toString());
							if (heightOfLayout > 0 && widthOfLayout > 0) {
								Toast.makeText(
										getBaseContext(),
										"heightOfLayout is: " + heightOfLayout
												+ " widthOfLayout is: "
												+ widthOfLayout,
										Toast.LENGTH_SHORT).show();
							}
						} catch (NumberFormatException nfe) {
							System.out.println("Could not parse " + nfe);
						}

					}
				})
				.setNegativeButton("Cancel",
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int whichButton) {
								// Do nothing.
							}
						}).show();

		// image_view_track_layout.measure(
		// image_view_track_layout.getMeasuredWidth(),
		// image_view_track_layout.getMeasuredHeight());
		int imgHeight = image_view_track_layout.getMeasuredHeight();
		int imgWidth = image_view_track_layout.getMeasuredWidth();
		int origImgHeight = image_view_track_layout.getDrawable()
				.getIntrinsicHeight();
		int origImgWidth = image_view_track_layout.getDrawable()
				.getIntrinsicWidth();
		double heightRatio = (double) imgHeight / (double) origImgHeight;
		double widthRatio = (double) imgWidth / (double) origImgWidth;
		double origRatioHW = (double) origImgHeight / (double) origImgWidth;
		if (heightRatio < widthRatio) {
			imgWidth = (int) (origImgWidth * heightRatio);
			imgHeight = (int) (imgWidth * origRatioHW);
		} else {
			imgHeight = (int) (origImgHeight * widthRatio);
			imgWidth = (int) (imgHeight * (1 / origRatioHW));
		}

		trainLayoutHeight = imgHeight;
		trainLayoutWidth = imgWidth;

	}

	void setScaledLayout() {
		// 243.84 old hardcoded height in cm for divisor
		trainLayoutScaleHeight = trainLayoutHeight / (heightOfLayout * 2.54);
		// 487.68 old hardcoded width in cm for divisor
		trainLayoutScaleWidth = trainLayoutWidth / (widthOfLayout * 2.54);

		Log.d(TAG_DEBUG, "Tracklayout height is: " + trainLayoutHeight
				+ " Tracklayout width is:  " + trainLayoutWidth
				+ " user input height in inches: " + heightOfLayout
				+ " user input width in inches: " + widthOfLayout
				+ " scale for height: " + trainLayoutScaleHeight
				+ " scale for width: " + trainLayoutScaleWidth);
	}

	// UPDATED!
	public String getPath(Uri uri) {
		String[] projection = { MediaColumns.DATA };
		@SuppressWarnings("deprecation")
		Cursor cursor = managedQuery(uri, projection, null, null, null);
		column_index = cursor.getColumnIndexOrThrow(MediaColumns.DATA);
		cursor.moveToFirst();
		imagePath = cursor.getString(column_index);

		return cursor.getString(column_index);
	}

}
