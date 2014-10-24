package edu.uah.ontrackcontroller;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.util.Log;
import android.view.DragEvent;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.DragShadowBuilder;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Spinner;
import android.widget.Toast;
import android.widget.ToggleButton;

public class MainActivity extends Activity implements OnClickListener, OnTouchListener {
	image_view track_Layout;

	RelativeLayout displayLayout;
	Button btnAdd;
	Button btnAddSwitch;
	Button btnTieCount;
	Button btnConnect;
	ImageView ImgViewTrains;
	ImageView ImgViewSwitches;
	ImageView ImgViewBarcodes;

	ToggleButton tglbtnSwitch;
	EditText edtxtTieCount;

	final int MAX_TRAINS = 10;
	final int MAX_SWITCHES = 50;
	final int MAX_BUFFER = 10;
	final int IMU_DATA_SIZE = 31;
	final int CURRENT_DATA_SIZE = 3;
	final int MAX_ADDRESS = 127;
	final int NOT_VALID = -1;
	private static final String TAG = "OnTrack Debug";
	protected static final byte CURRENT_HEADER = 11;
	protected static final byte IMU_HEADER = 10;
	final String TAG_TRACK_LAYOUT = "track_layout";
	final String TAG_TRAY_LAYOUT = "tray_layout";

	final String TAG_BARCODES = "barcode";
	final String TAG_SWITCHES = "switch";
	final String TAG_TRAINS = "train";

	byte[][] currentBuffer = new byte[MAX_BUFFER][CURRENT_DATA_SIZE];
	byte[][][] imuBuffer = new byte[MAX_TRAINS][MAX_BUFFER][IMU_DATA_SIZE];
	int currentBufferIndex = 0;
	int imuBufferIndex = 0;
	int imuTrainIndex = 0;
	RelativeLayout trayLayout;
	TextView txtvwStatus; // textview for Status
	Spinner spnTrain; // spinner for train selection
	Spinner spnSwitch;
	List<String> trainList; // string list for train names
	List<String> addressList; // string list to hold all bt device addresses
	List<String> switchList; // string list for train names
	Bitmap[] trainBmp;
	ArrayAdapter<String> dataAdapter; // adapter for spinner and string list
	// connection
	ArrayAdapter<String> switchAdapter;
	VerticalSeekBar vertSeekBar;
	int[] trainNum; // int array to hold train numbers
	int[] switchNum;
	int[] trainAddress;
	int[] switchAddress;
	double xTouch = 0.0;
	double yTouch = 0.0;
	// image_view.trainData[] trainData = new image_view.trainData[MAX_TRAINS];
	int trainIndex = 0; // current index to train array
	int switchIndex = 0;
	private static String address = null; // default address will be changed
	// later in code
	private static final UUID MY_UUID = UUID
			.fromString("00001101-0000-1000-8000-00805F9B34FB"); // default
	// serial
	// port
	// profile
	// UUID

	BluetoothAdapter btAdapter = null; // start all BT items as null to start.
	BluetoothDevice btDevice = null;
	BluetoothSocket btSocket = null;
	OutputStream btOutStream = null;
	InputStream btInStream = null;
	Boolean btconnected = false;
	Boolean exitingNow = false;
	Boolean longPress = false;
	int timeshere =0;

	Thread IMUThread = null;
	boolean currentRunning = false;

	Handler handler = new Handler();
	Runnable currentRun = new Runnable() {
		public void run() {
			try {
				CurrentRequest();
			} catch (IOException e) {

				e.printStackTrace();
			}
			handler.postDelayed(currentRun, 1000);
		}
	};

	BroadcastReceiver bcastReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
				// Device has disconnected
				btconnected = false;
				// txtvwCurrent.setText("Disconnected");
				currentRunning = false;
				handler.removeCallbacks(currentRun);
				try {
					btSocket.close();
					btSocket = null;
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
				btconnected = true;
				timeshere++;
				txtvwStatus.setText("Connected" + String.valueOf(timeshere));
				currentRunning = true;
			}

		}

	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		track_Layout = new image_view(this);

		displayLayout = (RelativeLayout) findViewById(R.id.displayLayout);
		displayLayout.addView(track_Layout);
		//track_Layout.setOnClickListener(this);
		//track_Layout.setOnTouchListener(this);
		BTSetup(); // initialize all bluetooth settings.
		this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
		getWindow().setSoftInputMode(
				WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

		init();
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
				Log.d(TAG, "BT is Enabled.");
			}

		} else {
			Log.d(TAG, "BT is null");

		}
		//filters();
	}

	private void init() {
		btnConnect = (Button) findViewById(R.id.btnLights);
		btnConnect.setText("Connect");
		vertSeekBar = (VerticalSeekBar) findViewById(R.id.vertSeekBar);
		tglbtnSwitch = (ToggleButton) findViewById(R.id.tglbtnSwitch);
		ImgViewBarcodes = (ImageView) findViewById(R.id.ImgViewBarcodes);
		ImgViewSwitches = (ImageView) findViewById(R.id.ImgViewSwitches);
		ImgViewTrains = (ImageView) findViewById(R.id.ImgViewTrains);

		ImgViewBarcodes.setTag(TAG_BARCODES);
		ImgViewSwitches.setTag(TAG_SWITCHES);
		ImgViewTrains.setTag(TAG_TRAINS);
		trayLayout = (RelativeLayout) findViewById(R.id.trayLayout);
		ImgViewBarcodes.setOnLongClickListener(clickListener);
		ImgViewSwitches.setOnLongClickListener(clickListener);
		ImgViewTrains.setOnLongClickListener(clickListener);

		displayLayout.setTag(TAG_TRACK_LAYOUT);
		trayLayout.setTag(TAG_TRAY_LAYOUT);

		View.OnDragListener dragListener = new View.OnDragListener() {

			@Override
			public boolean onDrag(View targetView, DragEvent dragEvent) {
				int action = dragEvent.getAction();

				switch(action) 
				{
				case DragEvent.ACTION_DROP:

					final ImageView originalImgView = (ImageView) dragEvent.getLocalState();
					final RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(originalImgView.getWidth(),originalImgView.getHeight());
					params.topMargin = (int) dragEvent.getY() - originalImgView.getHeight()/2;
					params.leftMargin = (int) dragEvent.getX() - originalImgView.getWidth()/2;
					ViewGroup parentView = (ViewGroup) originalImgView.getParent();

					if(parentView.getTag().equals(TAG_TRAY_LAYOUT) && targetView.getTag().equals(TAG_TRACK_LAYOUT)){
						//moving from tray to track.
						ImageView ImgView = new ImageView(getBaseContext());
						ImgView.setLayoutParams(params);
						ImgView.setOnLongClickListener(clickListener);
						ImgView.setTag(originalImgView.getTag());


						if(originalImgView.getTag().equals(TAG_TRAINS)){
							ImgView.setImageBitmap(BitmapFactory.decodeResource(getApplicationContext().getResources(), R.drawable.train1));
							addTrainAt();
						}
						else if(originalImgView.getTag().equals(TAG_SWITCHES)){
							ImgView.setImageBitmap(BitmapFactory.decodeResource(getApplicationContext().getResources(), R.drawable.football));
							addSwitchAt(params.leftMargin, params.topMargin);
						}
						else if(originalImgView.getTag().equals(TAG_BARCODES)){
							ImgView.setImageBitmap(BitmapFactory.decodeResource(getApplicationContext().getResources(), R.drawable.ic_launcher));
						}

						displayLayout.addView(ImgView);
					}
					else if(parentView.getTag().equals(TAG_TRACK_LAYOUT) && targetView.getTag().equals(TAG_TRACK_LAYOUT)){
						//moving inside the track to the track.
						originalImgView.setLayoutParams(params);
					}

					break;
				}
				return true;
			}
		};

		displayLayout.setOnDragListener(dragListener);
		trayLayout.setOnDragListener(dragListener);

		vertSeekBar
		.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				if (btconnected && btSocket != null
						&& btSocket.isConnected())
					btnSendListener();
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {

			}

			@Override
			public void onProgressChanged(SeekBar seekBar,
					int progress, boolean fromUser) {
				if (btconnected && btSocket != null
						&& btSocket.isConnected())
					btnSendListener();
			}
		});
		track_Layout.setOnLongClickListener(new View.OnLongClickListener() {

			@Override
			public boolean onLongClick(View v) {
				Toast.makeText(getBaseContext(), "Long Click Listener", Toast.LENGTH_LONG).show();
				return false;
			}
		});
		btnAdd = (Button) findViewById(R.id.btnAddTrain);
		btnAddSwitch = (Button) findViewById(R.id.btnAddSwitch);
		btnAddSwitch.setEnabled(false);
		btnAddSwitch.setVisibility(View.INVISIBLE);
		spnTrain = (Spinner) findViewById(R.id.spnTrainSelect);
		spnSwitch = (Spinner) findViewById(R.id.spnSwitchSel);
		txtvwStatus = (TextView) findViewById(R.id.txtvwStatus);


		// edtxtTieCount = (EditText)findViewById(R.id.edtxtTieCount);

		trainList = new ArrayList<String>();
		switchList = new ArrayList<String>();
		trainNum = new int[MAX_TRAINS];
		switchNum = new int[MAX_SWITCHES];
		trainAddress = new int[MAX_ADDRESS];
		switchAddress = new int[MAX_ADDRESS];
		for (int i = 0; i < MAX_ADDRESS; i++){
			trainAddress[i] = NOT_VALID;
			switchAddress[i] = NOT_VALID;

		}
		for (int i = 0; i < MAX_TRAINS; i++){
			trainNum[i] = NOT_VALID;
		}	
		for (int i = 0; i < MAX_SWITCHES; i++){
			switchNum[i] = NOT_VALID;
		}
		// default starting values
		/*
		 * for (int i = 1; i < 5; i++) {
		 * 
		 * trainNum[trainIndex] = i; list.add("Train " +
		 * String.valueOf(trainNum[trainIndex])); trainIndex++;
		 * switchAddress[switchIndex] = i; switchList.add("Switch " +
		 * String.valueOf(switchAddress[switchIndex])); switchIndex++; }
		 */

		dataAdapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_spinner_item, trainList);
		dataAdapter
		.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spnTrain.setAdapter(dataAdapter);

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

		tglbtnSwitch.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				btnLocoSendListener();

			}
		});

		btnAdd.setOnClickListener(new View.OnClickListener() {
			/*
			 * (non-Javadoc)
			 * 
			 * @see android.view.View.OnClickListener#onClick(android.view.View)
			 * Description: Add button handler Expected Results: Opens dialog
			 * for input. Should only take integers between 1 and 127, 0 is
			 * reserved for broadcast messages. Currently takes the address
			 * number and appends it to "Train " to handle naming from the list,
			 * future should allow naming of trains. Adds to both the string
			 * list for selection, and also the int array for easier
			 * calculation.
			 */
			@Override
			public void onClick(View arg0) {
				btnAddListener();
				Log.d(TAG, "adding train");

			}
		});
		btnAddSwitch.setOnClickListener(new View.OnClickListener() {
			/*
			 * (non-Javadoc)
			 * 
			 * @see android.view.View.OnClickListener#onClick(android.view.View)
			 * Description: Add button handler Expected Results: Opens dialog
			 * for input. Should only take integers between 1 and 127, 0 is
			 * reserved for broadcast messages. Currently takes the address
			 * number and appends it to "Train " to handle naming from the list,
			 * future should allow naming of trains. Adds to both the string
			 * list for selection, and also the int array for easier
			 * calculation.
			 */
			@Override
			public void onClick(View arg0) {
				btnAddSwitchListener();
				Log.d(TAG, "adding switch");

			}
		});

	}

	final View.OnLongClickListener clickListener = new View.OnLongClickListener() {

		@Override
		public boolean onLongClick(View view) {

			View.DragShadowBuilder shadowBuilder = new View.DragShadowBuilder(view);
			view.startDrag(null, shadowBuilder, view, 0);
			return false;
		}
	};


	public void btnSendListener() {
		if (btconnected && btSocket != null && btSocket.isConnected()) { 
			// check to see if it is not null, then see if it is connected 
			int address = 0;
			int speed = 0;
			int commandbits = 0;

			address = trainNum[spnTrain.getSelectedItemPosition()];
			speed = Math.abs(vertSeekBar.getProgress() - vertSeekBar.getMax()
					/ 2);
			for (int i = 1; i <= speed; i++) {
				commandbits ^= 0x10;
				if (i % 2 == 0)
					commandbits += 0x01;
			}

			if (vertSeekBar.getProgress() > vertSeekBar.getMax() / 2)
				commandbits ^= 96; // 64 is for the 01 in the packet format
			// 32 for the forward direction
			else
				commandbits ^= 64;

			int checksum = address ^ commandbits;
			try {
				sendMessage(address, commandbits, checksum);
			} finally {
				Log.d(TAG, "Sent Message");
			}
		} else
			txtvwStatus.setText("Bluetooth Not Connected!");
	}

	private void btnLocoSendListener() {
		int loconet = 0x01;
		int opcode = 0xB0;
		int address = 0;
		int command = 0;
		int checksum = 0;
		int endbyte = 0x0a;
		int simpleSwitchAddress = 0;
		byte[] outbuffer = new byte[6];

		if (btconnected && btSocket != null && btSocket.isConnected()) { 

			// check to see if it is not null, then see if it is connected 

			simpleSwitchAddress = switchNum[spnSwitch.getSelectedItemPosition()];
			address = simpleSwitchAddress & 0x7f;
			command = ((simpleSwitchAddress >> 7) & 0x0f);
			if (tglbtnSwitch.isChecked())
				command = (0x30 | (command & 0x0f));
			else
				command = (0x10 | (command & 0x0f));

			checksum = ~(((opcode & 0xff) ^ (address & 0xff)) ^ (command & 0xff));

			outbuffer[0] = (byte) (loconet & 0xff);
			outbuffer[1] = (byte) (opcode & 0xff);
			outbuffer[2] = (byte) (address & 0xff);
			outbuffer[3] = (byte) (command & 0xff);
			outbuffer[4] = (byte) (checksum & 0xff);
			outbuffer[5] = (byte) (endbyte & 0xff);
			txtvwStatus.setText("Message Type: "
					+ Integer.toHexString(outbuffer[0]) + " OpCode: "
					+ Integer.toHexString(outbuffer[1]) + " Address: "
					+ Integer.toHexString(outbuffer[2]) + " Command: "
					+ Integer.toHexString(outbuffer[3]) + " Checksum: "
					+ Integer.toHexString(outbuffer[4]) + " End Byte: "
					+ Integer.toHexString(outbuffer[5]));

			try {
				btOutStream = btSocket.getOutputStream();
			} catch (IOException e1) {
				e1.printStackTrace();
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
	}

	private void sendMessage(int address, int command, int checksum) {

		int dcc = 0;
		txtvwStatus.setText("Sending... \" address# 0x"
				+ Integer.toHexString(address) + " commandbits: 0x"
				+ Integer.toHexString(command) + " checksum: 0x"
				+ Integer.toHexString(checksum) + "\".");
		byte[] outbuffer = new byte[4];
		outbuffer[0] = (byte) (dcc & 0xff);
		outbuffer[1] = (byte) (address & 0xff);
		outbuffer[2] = (byte) (command & 0xff);
		outbuffer[3] = (byte) (checksum & 0xff);
		txtvwStatus.setText("Sending... \" address# 0x"
				+ Integer.toHexString(outbuffer[1]) + " commandbits: 0x"
				+ Integer.toHexString(outbuffer[2]) + " checksum: 0x"
				+ Integer.toHexString(outbuffer[3]) + "\".");

		try {
			btOutStream = btSocket.getOutputStream();
		} catch (IOException e1) {
			e1.printStackTrace();
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

	public void btnTieCountListener() {
		/*
		 * Log.d(TAG, "btn listener " +
		 * Integer.parseInt(edtxtTieCount.getText().toString())); int address =
		 * Integer
		 * .parseInt(list.get(spnTrain.getSelectedItemPosition()).substring(6));
		 * Log.d(TAG, "Address: " + address); updateTrain(address,
		 * Integer.parseInt(edtxtTieCount.getText().toString()));
		 */
	}

	public void addTrainAt(){
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
						if (value >= 0 && value <= 127) {
							if(trainAddress[value] == NOT_VALID){
								trainNum[trainIndex] = value;
								trainAddress[value] = trainIndex;
								prepareIMUBuffer(value);
								track_Layout.setTrainImage(
										getBaseContext(), value,
										trainIndex);
								addTrainToLayout(value, true);

								trainList.add("Train " + value);

								dataAdapter.notifyDataSetChanged();
								spnTrain.setSelection(trainIndex);
								trainIndex++;
							} 
							else {
								Toast.makeText(getBaseContext(), "Train already exists", Toast.LENGTH_SHORT).show();

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

	public void btnAddListener() {

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
						if (value >= 0 && value <= 127) {
							if(trainAddress[value] == NOT_VALID){
								trainNum[trainIndex] = value;
								trainAddress[value] = trainIndex;
								prepareIMUBuffer(value);
								track_Layout.setTrainImage(
										getBaseContext(), value,
										trainIndex);
								addTrainToLayout(value, true);

								trainList.add("Train " + value);

								dataAdapter.notifyDataSetChanged();
								spnTrain.setSelection(trainIndex);
								trainIndex++;
							} 
							else {
								Toast.makeText(getBaseContext(), "Train already exists", Toast.LENGTH_SHORT).show();

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

	public void prepareIMUBuffer(int trainID){
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

	public void btnAddSwitchListener() {

		final EditText input = new EditText(MainActivity.this);
		if (switchIndex < MAX_SWITCHES) { // Checks to make sure the maximum
			// number of trains hasn't been
			// exceeded.
			new AlertDialog.Builder(MainActivity.this)
			.setTitle("Add A Switch")
			.setMessage("Please input the switch address number")
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
							switchNum[switchIndex] = value;
							switchIndex++;
							switchList.add("Switch " + value);
							switchAdapter
							.notifyDataSetChanged();
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

	public void addTrainToLayout(int value, boolean isTrain) {
		Random random = new Random();
		float Speed = random.nextFloat();
		float x = Math.round(20) * (value*2 + 1);
		float y = Math.round(20) * (value*2 + 1);
		if(isTrain){
			track_Layout.trainInfo[trainIndex].address = value;
			Log.d(TAG, "Added Train at x: " + x + " y: " + y);
			track_Layout.trainInfo[trainIndex].setCoordinates(x,y);
			track_Layout.trainInfo[trainIndex].setSpeed(Speed);

		}
		else{

			track_Layout.switchInfo[switchIndex].setSpeed(Speed);
		}
		track_Layout.invalidate();
	}

	public void updateTrain(int trainID, int tieCount, float xAccel, float yAccel, float yawGyro, float yawDifference) {


		int index = trainAddress[trainID];
		if(index != -1){
			Log.d(TAG, "Train in list #" + trainID);
			float arcLength = (float) (0.36585366 * (float) tieCount);
			float radOffset = (float) (yawGyro * 0.0174532925);
			float distance = (float)(arcLength/radOffset);
			float xCord = (float) (distance * Math.cos(radOffset));
			xCord += track_Layout.trainInfo[index].getxCoordinate();
			float yCord = (float) (distance * Math.sin(radOffset));
			yCord += track_Layout.trainInfo[index].getyCoordinate();

			track_Layout.trainInfo[index].setCoordinates(xCord,yCord);

		}
	}

	private class BTConnect extends AsyncTask<Void, Void, Void> {
		/*
		 * Creates an alert dialog which displays a list of all bonded bluetooth
		 * devices. This allows you to choose the Bluetooth Device you wish to
		 * connect to. Once the user selects the device from the list, an
		 * attempt to connect is made.
		 */
		// Test alert dialog with list view of all bt devices.
		protected void onPreExecute() {
			handler.removeCallbacks(currentRun);
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

			handler.postDelayed(currentRun, 5000);
			if (curTry >= maxTries) {
				Log.d(TAG, "failed to connect");
				// txtvwStatus.setText("Connection Unsuccessful!");
			} else {
				// txtvwStatus.setText("Connection Successful!");
				currentRunning = true;
			}

			filters();
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
					Log.d(TAG, "Canceled Discovery");
					btDevice = btAdapter.getRemoteDevice(address);
					Log.d(TAG, "Connecting to ... " + btDevice);

					try {

						Log.d(TAG, "trying to create socket");
						btSocket = btDevice
								.createRfcommSocketToServiceRecord(MY_UUID);
						// Here is the part the connection is made, by asking
						// the device to create a RfcommSocket (Unsecure socket
						// I guess), It map a port for us or something like that
						btSocket.connect();
						// txtvwStatus.setText("BlueTooth Connection Successful");
						Log.d(TAG, "Connection made.");

					} catch (IOException e) {

						// txtvwStatus.setText("BlueTooth Connection Unsuccessful");
						Log.d(TAG, "failed to create socket");
						try {
							btSocket.close();
						} catch (IOException e2) {
							Log.d(TAG, "Unable to end the connection");
						}
						Log.d(TAG, "Socket creation failed");
					}
				} else {

					Log.d(TAG, "Bad Address");
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

	private void CurrentRequest() throws IOException {

		if (btconnected && btSocket != null && btSocket.isConnected()) {

			Log.d(TAG, "Current: connection is good!");
			byte[] outbuffer = new byte[1];
			outbuffer[0] = (byte) (3 & 0xff);
			// byte[] inbuffer = new byte[2];
			try {
				btInStream = btSocket.getInputStream();
				btOutStream = btSocket.getOutputStream();
			} catch (IOException e) {
				e.printStackTrace();
			}
			if (outbuffer != null)
				try {

					Log.d(TAG, "Current: Outputting via bluetooth");
					btOutStream.write(outbuffer);
					btOutStream.flush();
				} catch (IOException e) {
					e.printStackTrace();
				}
		}
	}

	private void createIMUThread() { // rename createBTRecieveThread
		IMUThread = new Thread(new Runnable() {
			public void run() {
				boolean excep = false;
				while (!excep) {
					if (currentRunning && btSocket != null) {
						// Keep listening to the InputStream until an exception
						// occurs
						// receive thread should look at first byte of the
						// message.
						// depending on first byte it should either prepare to
						// receive
						// current data, or location/imu data.
						// so it check available, read the first byte, switch on
						// that byte
						// waiting on the correct number of bytes to be
						// available for that type
						// it should then put that incoming message into a
						// buffer to be worked on
						// and raise a flag or spawn a worker thread.

						try {

							if (btconnected && btInStream != null) {
								byte[] inbuffer = null;
								btInStream = btSocket.getInputStream();
								int bytes = 0;
								bytes = btInStream.available();
								// byte[] header = new byte[1];

								if (bytes >= IMU_DATA_SIZE) {
									Log.d(TAG,"Inside createIMUThread and bytes are: " + bytes);
									// receive first byte
									inbuffer = new byte[bytes];
									//Log.d(TAG, "number of bytes " + bytes);
									btInStream.read(inbuffer, 0,
											inbuffer.length);
									// bytes = btInStream.available();
									//Log.d(TAG, "Received message.");

									/*if (bytes == CURRENT_DATA_SIZE
											&& inbuffer[0] == CURRENT_HEADER) {

										// btInStream.read(inbuffer, 0, bytes);

										// switch on inbuffer[0] should go here
										currentBuffer[currentBufferIndex
										              % MAX_BUFFER][0] = inbuffer[1];
										currentBuffer[currentBufferIndex
										              % MAX_BUFFER][1] = inbuffer[2];
										currentBufferIndex++;
										// spawn current calc thread.
										if (currentBufferIndex >= MAX_BUFFER)
											updateCurrent();
									} else */
									if (bytes == IMU_DATA_SIZE
											&& inbuffer[0] == IMU_HEADER) {
										// inbuffer = new byte[bytes];

										Log.d(TAG, "Received IMU data.");
										// btInStream.read(inbuffer, 0, bytes);
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
							// TODO Auto-generated catch block
							excep = true;
							e.printStackTrace();
						}
					}
				}
			}
		});

	}

	private void updateIMU(final int imuTrainID) {
		//		new Thread(new Runnable() {
		//			public void run() {
		Log.d(TAG, "Inside updateIMU function.");
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

		final int tieCount = (ByteBuffer.wrap(tieCountBytes)
				.order(ByteOrder.LITTLE_ENDIAN).getInt()) - (ByteBuffer.wrap(prevTieCountBytes)
						.order(ByteOrder.LITTLE_ENDIAN).getInt());

		final float prevGyroYaw = ByteBuffer.wrap(prevGyroYawBytes)
				.order(ByteOrder.LITTLE_ENDIAN).getFloat();


		final int imuBarCode = (barCodeBytes[0] & 0xff);

		updateTrain(3, tieCount, acelX, acelY, gyroYaw, (gyroYaw - prevGyroYaw));

		Log.d(TAG, "Train #"
				+ imuTrainID + ": acel x" + String.valueOf(acelX) +
				", acel y " + String.valueOf(acelY) + ", acel z " +
				String.valueOf(acelZ) + ", gyroYaw " +
				String.valueOf(gyroYaw) + ", gyroPitch " +
				String.valueOf(gyroPitch) + ", gyroRoll " +
				String.valueOf(gyroRoll) + ", tieCount " +
				String.valueOf(tieCount) + ", imuBarCode " +
				String.valueOf(imuBarCode));

		txtvwStatus.post(new Runnable() { 
			@Override public void run() { txtvwStatus.setText("Train #"
					+ imuTrainID + ": acel x" + String.valueOf(acelX) +
					", acel y " + String.valueOf(acelY) + ", acel z " +
					String.valueOf(acelZ) + ", gyroYaw " +
					String.valueOf(gyroYaw) + ", gyroPitch " +
					String.valueOf(gyroPitch) + ", gyroRoll " +
					String.valueOf(gyroRoll) + ", tieCount " +
					String.valueOf(tieCount) + ", imuBarCode " +
					String.valueOf(imuBarCode));

			} 
		});

	}
	//		});
	//
	//	}

	private void updateCurrent() {
		new Thread(new Runnable() {
			public void run() {
				int current[] = new int[MAX_BUFFER];
				int sum = 0;
				Log.d(TAG, "Current: being calc");
				for (int i = 0; i < MAX_BUFFER; i++) {
					current[i] = (currentBuffer[i][0] & 0xff);
					current[i] = current[i] << 8;
					current[i] = current[i] & 0xff00;
					current[i] = current[i] | (currentBuffer[i][1] & 0xff);
					current[i] = current[i] & 0xffff;
					current[i] = (int) (current[i] * 4.296);
					sum += current[i];
				}
				final double update = (double) sum / (double) MAX_BUFFER;
				txtvwStatus.post(new Runnable() {

					@Override
					public void run() {
						txtvwStatus.setText(String.valueOf(update) + " mA");
					}
				});
			}
		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
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
			sendMessage(0, 0, 0);
			try {
				btOutStream.flush();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			handler.removeCallbacks(currentRun);
			try {
				currentRunning = false;
				Thread.sleep(5);
				btSocket.close();
				btSocket = null;
				btInStream.close();
				btOutStream.close();

			} catch (IOException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
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
		Log.d(TAG, "...onResume - try connect...");

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
		Log.d(TAG, "...In onPause()... thread is: " + IMUThread.isAlive());
		if (btOutStream != null) {
			try {
				btOutStream.flush();
			} catch (IOException e) {

			}
		}
		/*
		 * if (!exitingNow && btSocket != null) try { Log.d(TAG,
		 * "Closing socket. thread is: " + currentThread.isAlive());
		 * btSocket.close(); Log.d(TAG, "Closed socket. thread is: " +
		 * currentThread.isAlive()); } catch (IOException e2) { Log.d(TAG,
		 * "Closing socket exception caught thread is: " +
		 * currentThread.isAlive()); }
		 */

		super.onPause();
	}

	@Override
	protected void onStop() {
		// TODO Auto-generated method stub

		super.onStop();
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
					// Stop the activity
					MainActivity.this.finish();
				}

			}).setNegativeButton(R.string.no, null).show();

			return true;
		} else {
			return super.onKeyDown(keyCode, event);
		}
	}

	public void addSwitchAt(final double x, final double y){
		final EditText input = new EditText(MainActivity.this);
		if (switchIndex < MAX_SWITCHES) { // Checks to make sure the maximum
			// number of trains hasn't been
			// exceeded.
			new AlertDialog.Builder(MainActivity.this)
			.setTitle("Add A Switch")
			.setMessage("Please input the switch address number")
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
							if(switchAddress[value] == NOT_VALID){
								switchNum[switchIndex] = value;
								switchAddress[value] = switchIndex;
								track_Layout.setSwitchImage(getBaseContext(), value, switchIndex);
								track_Layout.switchInfo[switchIndex].address = value;
								Log.d(TAG, "Added Switch at x: " + x + " y: " + y);
								track_Layout.switchInfo[switchIndex].setCoordinates((float)x, (float) y);
								switchList.add("Switch " + value);
								switchAdapter.notifyDataSetChanged();
								spnSwitch.setSelection(switchIndex);
								switchIndex++;
							}
							else{
								Toast.makeText(getBaseContext(), "Switch already exists", Toast.LENGTH_SHORT).show();
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
	public void removeTrain(final int address){
		new AlertDialog.Builder(this)
		.setIcon(android.R.drawable.ic_dialog_alert)
		.setTitle(getString(R.string.confirmDelete) + "Train?")
		.setMessage(getString(R.string.trainDelete) + String.valueOf(address))
		.setPositiveButton(R.string.confirmDelete,
				new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog,
					int which) {
				int removeIndex = trainAddress[address];
				for(int i = removeIndex; i < trainIndex; i++){
					if(trainNum[i+1] != NOT_VALID){

						trainNum[i] = trainNum[i+1];
						trainAddress[trainNum[i]] = i;
						track_Layout.trainInfo[i].address = track_Layout.trainInfo[i+1].address;
						track_Layout.trainInfo[i].myImgView = track_Layout.trainInfo[i+1].myImgView;
						track_Layout.trainInfo[i].speed = track_Layout.trainInfo[i+1].speed;
						track_Layout.trainInfo[i].xCoordinate = track_Layout.trainInfo[i+1].xCoordinate;
						track_Layout.trainInfo[i].yCoordinate = track_Layout.trainInfo[i+1].yCoordinate;
						trainList.set(i, trainList.get(i+1));
					}
					else{
						trainAddress[address] = NOT_VALID;
						trainNum[i] = NOT_VALID;
						trainList.remove(i);
					}
				}
				trainIndex--;
				dataAdapter.notifyDataSetChanged();
				track_Layout.numTrains--;
			}

		}).setNegativeButton(R.string.denyDelete, null).show();

	}

	public void removeSwitch(final int address){
		new AlertDialog.Builder(this)
		.setIcon(android.R.drawable.ic_dialog_alert)
		.setTitle(getString(R.string.confirmDelete) + "Switch?")
		.setMessage(getString(R.string.switchDelete) + String.valueOf(address))
		.setPositiveButton(R.string.confirmDelete,
				new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog,
					int which) {
				int removeIndex = switchAddress[address];
				for(int i = removeIndex; i < switchIndex; i++){
					if(switchNum[i+1] != NOT_VALID){

						switchNum[i] = switchNum[i+1];
						switchAddress[switchNum[i]] = i;
						track_Layout.switchInfo[i].address = track_Layout.switchInfo[i+1].address;
						track_Layout.switchInfo[i].myImgView = track_Layout.switchInfo[i+1].myImgView;
						track_Layout.switchInfo[i].speed = track_Layout.switchInfo[i+1].speed;
						track_Layout.switchInfo[i].xCoordinate = track_Layout.switchInfo[i+1].xCoordinate;
						track_Layout.switchInfo[i].yCoordinate = track_Layout.switchInfo[i+1].yCoordinate;
						switchList.set(i, switchList.get(i+1));
					}
					else{
						switchAddress[address] = NOT_VALID;
						switchNum[i] = NOT_VALID;
						switchList.remove(i);
					}
				}
				switchIndex--;
				switchAdapter.notifyDataSetChanged();
				track_Layout.numSwitches--;
			}

		}).setNegativeButton(R.string.denyDelete, null).show();

	}

	public int isTrainClicked(double x, double y){
		for(int i = 0; i < trainIndex; i++){
			//			Log.d(TAG, i + " Train is at x: " + track_Layout.trainInfo[i].getxCoordinate());
			//			Log.d(TAG, i + " Train is at y: " + track_Layout.trainInfo[i].getyCoordinate());
			if( (Math.abs(track_Layout.trainInfo[i].getxCoordinate() - x) < 50) && (Math.abs(track_Layout.trainInfo[i].getyCoordinate() - y) < 50)){
				int address = track_Layout.trainInfo[i].address;
				int position = trainAddress[address];
				//Log.d(TAG, "Touched at train # " + address);
				//				Log.d(TAG, "Change spinner to position # " + position);
				spnTrain.setSelection(position);
				return address;
			}
		}
		return 0;
	}
	public int isSwitchClicked(double x, double y){
		for(int i = 0; i < switchIndex; i++){
			//Log.d(TAG, i + " Switch is at x: " + track_Layout.switchInfo[i].getxCoordinate());
			//Log.d(TAG, i + " Switch is at y: " + track_Layout.switchInfo[i].getyCoordinate());
			if( (Math.abs(track_Layout.switchInfo[i].getxCoordinate() - x) < 50) && (Math.abs(track_Layout.switchInfo[i].getyCoordinate() - y) < 50)){
				int address = track_Layout.switchInfo[i].address;
				int position = switchAddress[address];
				//Log.d(TAG, "Touched at switch # " + address);
				//Log.d(TAG, "Change spinner to position # " + position);
				spnSwitch.setSelection(position);
				return address;
			}
		}
		return 0;
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		Log.d(TAG, "Touched somewhere");
		xTouch = Math.round(event.getX());
		yTouch = Math.round(event.getY());

		/*if(event.getAction() == MotionEvent.ACTION_DOWN){
				longPress = false;
			ClipData data = ClipData.newPlainText("", "");
			DragShadowBuilder shadowBuilder = new View.DragShadowBuilder();
			Canvas testcanvas = new Canvas(BitmapFactory.decodeResource(this.getResources(),R.drawable.football));
			shadowBuilder.onDrawShadow(testcanvas);
			track_Layout.startDrag(data, shadowBuilder, track_Layout, 0);
		}*/

		//select spinner based on switch/train touched.

		return false;
	}

	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		double x = xTouch;
		double y = yTouch;
		int trainTouched = isTrainClicked(x, y);
		int switchTouched = isSwitchClicked(x, y);
		Log.d(TAG, "Clicked at x: " + x + " y: " + y);
		if(longPress){
			longPress = false;
			if(trainTouched != 0){
				removeTrain(trainTouched);
				Log.d(TAG, "Long press on train");
			}
			else if(switchTouched != 0){
				removeSwitch(switchTouched);
				Log.d(TAG, "Long press on switch");
			}
			else{
				//addSwitchAt(x, y);
			}
		}
	}


}
