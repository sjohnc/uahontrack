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

import edu.onTrack.dragdrop.R;

import android.os.AsyncTask;
import android.os.Bundle;
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
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
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
import android.widget.RelativeLayout.LayoutParams;
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
	final int IMU_DATA_SIZE = 31;
	final int CURRENT_DATA_SIZE = 3;
	final int MAX_ADDRESS = 127;
	final int NOT_VALID = -1;
	int px;

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
	double trainLayoutScale = 0;

	TextDrawable txtDrawNumber;
	Drawable train_background;
	Drawable train_foreground;

	// Add switch and barcode drawables
	Drawable switch_background;
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

	Button btnConnect;
	Button btnIMU;
	Button btnRezero;
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
	Boolean btconnected = false;
	boolean currentRunning = false;
	Boolean exitingNow = false;
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
				// handler.removeCallbacks(currentRun);
				try {
					btSocket.close();
					btSocket = null;
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
				btconnected = true;
				Log.d(TAG_DEBUG, "Connected");
				txtvwStatus.setText("Connected");
				try {
					btInStream = btSocket.getInputStream();
					btOutStream = btSocket.getOutputStream();
				} catch (IOException e) {
					// TODO Auto-generated catch block
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
		private final float textSize = 48;

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
			canvas.drawText(text, px / 2, px / 2 + textSize / 4, paint);
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

		public image_view(Context context) {
			super(context);
			myType = "";
			myIndex = 0;
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
		txtvwStatus = (TextView)findViewById(R.id.txtvwStatus);

		// Layouts
		relLayout_Track = (RelativeLayout) findViewById(R.id.relLayout_Track);
		relLayout_Tray = (RelativeLayout) findViewById(R.id.relLayout_Tray);
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

		btnConnect.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				new BTConnect().execute();

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
				if (switch_list[pos].myState == 0)
					tglbtnSwitch.setChecked(false);
				else
					tglbtnSwitch.setChecked(true);

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
						train_list[spnTrain.getSelectedItemPosition()]
								.setMyState(vertSeekBar.getProgress());
						if (btconnected && btSocket != null
								&& btSocket.isConnected())
							dccMessagePrepAndSend();
					}

					@Override
					public void onStartTrackingTouch(SeekBar seekBar) {

					}

					@Override
					public void onProgressChanged(SeekBar seekBar,
							int progress, boolean fromUser) {
						if (btconnected && btSocket != null
								&& btSocket.isConnected())
							dccMessagePrepAndSend();
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
		final EditText input = new EditText(MainActivity.this);
		if (barcodeIndex < MAX_BARCODES) { // Checks to make sure the maximum
											// number
			// of trains hasn't been exceeded.
			new AlertDialog.Builder(MainActivity.this)
					.setTitle("Add A Barcode")
					.setMessage("Please input the barcode number")
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

			spnSwitch.setSelection(switchAddress[touchedObject.myIndex]);
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

	private void locoNetMessagePrepAndSend() {
		int loconet = 0x01;
		int opcode = 0xB0;
		int address = 0;
		int command = 0;
		int checksum = 0;
		int endbyte = 0x0a;
		int simpleSwitchAddress = 0;
		boolean switchState = tglbtnSwitch.isChecked();
		if (switchState)
			switch_list[spnSwitch.getSelectedItemPosition()].setMyState(1);
		else
			switch_list[spnSwitch.getSelectedItemPosition()].setMyState(0);
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
		Log.d(TAG_DEBUG,
				"Inside createIMUThread");
		if (IMUThread == null)
			IMUThread = new Thread(new Runnable() {
				public void run() {
					int bytes = 0;
					boolean excep = false;
					while (!excep) {
						if (currentRunning && btSocket != null) {
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

							try {

								if (btconnected && btInStream != null) {
									byte[] inbuffer = null;
									
									bytes = btInStream.available();
									// byte[] header = new byte[1];
									
									if (bytes >= IMU_DATA_SIZE) {
										Log.d(TAG_DEBUG,
												"Inside createIMUThread and bytes are: "
														+ bytes);
										txtvwStatus.setText(
														"Inside createIMUThread and bytes are: "
																+ bytes);
										// receive first byte
										inbuffer = new byte[bytes];
										// Log.d(TAG_DEBUG, "number of bytes " +
										// bytes);
										btInStream.read(inbuffer, 0,
												inbuffer.length);
									
										if (bytes == IMU_DATA_SIZE
												&& inbuffer[0] == IMU_HEADER) {
											// inbuffer = new byte[bytes];

											Log.d(TAG_DEBUG,
													"Received IMU data.");
											// btInStream.read(inbuffer, 0,
											// bytes);
											int imuTrainID = (int) (inbuffer[1 & 0xff]);
											for (int i = 0; i < IMU_DATA_SIZE - 2; i++)
												imuBuffer[imuTrainID][0][i] = inbuffer[i + 2];
											imuBufferIndex++;
											// spawn current calc thread.
											updateIMU(imuTrainID);
										}

									}

								}

							} catch (IOException e) {
								Log.d(TAG_DEBUG, "Exception in IMU Thread");
								excep = true;
								e.printStackTrace();
							}
						}
					}
				}
			});

	}

	public void updateTrain(int trainID, int tieCount, float xAccel,
			float yAccel, float yawGyro, float yawDifference) {
		trainLayoutHeight = relLayout_Track.getMeasuredHeight();
		trainLayoutWidth = relLayout_Track.getMeasuredWidth();
		trainLayoutScale = ((double)trainLayoutHeight/243.84);

		int index = trainAddress[trainID];
		if (index != -1) {
			Log.d(TAG_DEBUG, "Train in list #" + trainID);
			float arcLength = (float) (0.36585366 * (float) tieCount * trainLayoutScale);
			float radOffset = (float) (yawGyro * 0.0174532925);
			float distance = (float) (arcLength / radOffset);
			float xCord = (float) (distance * Math.cos(radOffset));
			xCord += train_list[index].getX();
			float yCord = (float) (distance * Math.sin(radOffset));
			yCord += train_list[index].getY();

			
			final RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
					train_list[index].getWidth(),
					train_list[index].getHeight());
			params.topMargin = (int) yCord;
			params.leftMargin = (int) xCord;
			train_list[index].setLayoutParams(params);
			train_list[index].setLeft(params.leftMargin);
			train_list[index].setTop(params.topMargin);
			train_list[index].bringToFront();
			train_list[index].invalidate();
			train_list[index].requestLayout();

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
			prevTieCountBytes[i] = imuBuffer[imuTrainID][1][i + 24];

		}
		for (int i = 0; i < 4; i++) {
			imuBuffer[imuTrainID][1][i + 20] = imuBuffer[imuTrainID][0][i + 20];
			imuBuffer[imuTrainID][1][i + 24] = imuBuffer[imuTrainID][0][i + 24];
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

		final int tieCount = (ByteBuffer.wrap(tieCountBytes).order(
				ByteOrder.LITTLE_ENDIAN).getInt())
				- (ByteBuffer.wrap(prevTieCountBytes).order(
						ByteOrder.LITTLE_ENDIAN).getInt());

		final float prevGyroYaw = ByteBuffer.wrap(prevGyroYawBytes)
				.order(ByteOrder.LITTLE_ENDIAN).getFloat();

		final int imuBarCode = (barCodeBytes[0] & 0xff);

		if (imuBarCode != 0) {
			barCodeReZero(imuTrainID, imuBarCode);
		}
		updateTrain(imuTrainID, tieCount, acelX, acelY, gyroYaw,
				(gyroYaw - prevGyroYaw));

		Log.d(TAG_DEBUG,
				"Train #" + imuTrainID + ": acel x" + String.valueOf(acelX)
						+ ", acel y " + String.valueOf(acelY) + ", acel z "
						+ String.valueOf(acelZ) + ", gyroYaw "
						+ String.valueOf(gyroYaw) + ", gyroPitch "
						+ String.valueOf(gyroPitch) + ", gyroRoll "
						+ String.valueOf(gyroRoll) + ", tieCount "
						+ String.valueOf(tieCount) + ", imuBarCode "
						+ String.valueOf(imuBarCode));

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

	void barCodeReZero(int imuTrainID, int barCode) {
		int left = barcode_list[barcodeAddress[barCode]].getLeft();
		int top = barcode_list[barcodeAddress[barCode]].getTop();
		// train_list[trainAddress[imuTrainID]].setLeft(left);
		// train_list[trainAddress[imuTrainID]].setTop(top);
		train_list[trainAddress[imuTrainID]].setX((float) left);
		train_list[trainAddress[imuTrainID]].setY((float) top);
		train_list[trainAddress[imuTrainID]].bringToFront();
		train_list[trainAddress[imuTrainID]].invalidate();
		train_list[trainAddress[imuTrainID]].requestLayout();
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
		Log.d(TAG_DEBUG, "...In onPause()... thread is: " + IMUThread.isAlive());
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

}
