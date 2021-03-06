package edu.uah.testui;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
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
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ToggleButton;

public class MainActivity extends Activity {

	final int MAX_TRAINS = 10;
	final int MAX_SWITCHES = 10;
	final int MAX_BUFFER = 10;
	final int IMU_DATA_SIZE = 34;
	final int CURRENT_DATA_SIZE = 3;

	EditText edtxtCustomCommAdd; // edit text for custom address
	EditText edtxtCustomCommCommand; // edit text for custom speed
	EditText edtxtLocoOpCode;
	EditText edtxtLocoAddress;
	EditText edtxtLocoCommand;
	EditText edtxtLocoChecksum;
	SeekBar skbarSpeed; // seek bar for speed control
	Spinner spnTrain; // spinner for train selection
	Spinner spnSwitch;
	Button btnSend; // button for sending packet
	Button btnLocoSend;
	Button btnDirection; // button for changing direction
	Button btnAdd; // button for adding a train
	Button btnAddSwitch;
	Button btnConnect; // button for connecting to bluetooth
	Button btnSwitchPosition;
	ToggleButton btnLoconet; // button for receiving current information
	CheckBox chkbxRawComm; // check box for raw command or speed/direction
	CheckBox chkbxLocoRaw;
	TextView txtvwStatus; // textview for Status
	TextView txtvwCurrent;
	TextView txtvwIMUData;
	List<String> list; // string list for train names
	List<String> addressList; // string list to hold all bt device addresses
	List<String> switchList; // string list for train names
	ArrayAdapter<String> dataAdapter; // adapter for spinner and string list
	// connection
	ArrayAdapter<String> switchAdapter;
	Scanner scan;
	Thread currentThread = null;
	boolean currentRunning = false;

	int[] trainNum; // int array to hold train numbers
	int[] switchAddress;
	byte[][] currentBuffer = new byte[MAX_BUFFER][CURRENT_DATA_SIZE];
	byte[][][] imuBuffer = new byte[MAX_TRAINS][MAX_BUFFER][IMU_DATA_SIZE];
	int currentBufferIndex = 0;
	int imuBufferIndex = 0;
	int imuTrainIndex = 0;
	int trainIndex = 0; // current index to train array
	int switchIndex = 0;
	private String address = null; // default address will be changed
	// later in code
	private static final UUID MY_UUID = UUID
			.fromString("00001101-0000-1000-8000-00805F9B34FB"); // default
	// serial
	// port
	// profile
	// UUID
	private static final String TAG = "OnTrack";
	protected static final byte CURRENT_HEADER = 11;
	protected static final byte IMU_HEADER = 10;

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

	BluetoothAdapter btAdapter = null; // start all BT items as null to start.
	BluetoothDevice btDevice = null;
	BluetoothSocket btSocket = null;
	OutputStream btOutStream = null;
	InputStream btInStream = null;
	Boolean btconnected = false;
	Boolean exitingNow = false;

	BroadcastReceiver bcastReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			BluetoothDevice device = intent
					.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

			if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
				// Device has disconnected
				btconnected = false;
				txtvwCurrent.setText("Disconnected");
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
				txtvwCurrent.setText("Connected");
				currentRunning = true;
			}

		}

	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		Log.d("OnTrack", "On Create");
		init(); // initialize all values and tie ui components to code
		setupButtonListeners(); // sets up button listeners for each button
		BTSetup(); // initialize all bluetooth settings.
		this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
		getWindow().setSoftInputMode(
				WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
		filters();

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

	@Override
	public void onResume() {
		super.onResume();
		if (currentThread == null) {
			createCurrentThread();
			currentThread.start();
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
		Log.d(TAG, "...In onPause()... thread is: " + currentThread.isAlive());
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
			txtvwStatus.setText("Connecting to Bluetooth, please wait...");
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
			if (curTry >= maxTries)
				txtvwStatus.setText("Connection Unsuccessful!");
			else {
				txtvwStatus.setText("Connection Successful!");
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
					Log.d("OnTrack", "Canceled Discovery");
					btDevice = btAdapter.getRemoteDevice(address);
					Log.d("OnTrack", "Connecting to ... " + btDevice);

					try {

						Log.d("OnTrack", "trying to create socket");
						btSocket = btDevice
								.createRfcommSocketToServiceRecord(MY_UUID);
						// Here is the part the connection is made, by asking
						// the device to create a RfcommSocket (Unsecure socket
						// I guess), It map a port for us or something like that
						btSocket.connect();
						// txtvwStatus.setText("BlueTooth Connection Successful");
						Log.d("OnTrack", "Connection made.");

					} catch (IOException e) {

						// txtvwStatus.setText("BlueTooth Connection Unsuccessful");
						Log.d("OnTrack", "failed to create socket");
						try {
							btSocket.close();
						} catch (IOException e2) {
							Log.d("OnTrack", "Unable to end the connection");
						}
						Log.d("OnTrack", "Socket creation failed");
					}
				} else {

					Log.d("OnTrack", "Bad Address");
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

	public void setStatus(String input) {
		txtvwStatus.setText(input);
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
				Log.d("OnTrack", "BT is Enabled.");
			}

		} else {
			Log.d("OnTrack", "BT is null");

		}
	}

	private void init() {
		Log.d("OnTrack", "Starting Init");
		/*
		 * Handles all of the UI components Sets up the train int array Sets up
		 * the max for the seekbar Defaults the direction to forward. Adds a few
		 * trains to the string list array and the int array while increasing
		 * the index each time. Attaches the list to the spinner via an adapter
		 */
		edtxtCustomCommAdd = (EditText) findViewById(R.id.edtxtCustCommAddress);
		edtxtCustomCommCommand = (EditText) findViewById(R.id.edtxtCustCommCommand);
		skbarSpeed = (SeekBar) findViewById(R.id.skbarSpeed);
		spnTrain = (Spinner) findViewById(R.id.spnTrain);
		btnSend = (Button) findViewById(R.id.btnSend);
		btnDirection = (Button) findViewById(R.id.btnDirection);
		btnAdd = (Button) findViewById(R.id.btnAdd);
		btnConnect = (Button) findViewById(R.id.btnConnect);
		btnLoconet = (ToggleButton) findViewById(R.id.btnLoconet);
		txtvwStatus = (TextView) findViewById(R.id.txtvwStatus);
		txtvwCurrent = (TextView) findViewById(R.id.txtvwCurrent);
		chkbxRawComm = (CheckBox) findViewById(R.id.chkbxRawComm);
		edtxtLocoOpCode = (EditText) findViewById(R.id.edtxtLocoOpcode);
		edtxtLocoAddress = (EditText) findViewById(R.id.edtxtLocoAdd);
		edtxtLocoCommand = (EditText) findViewById(R.id.edtxtLocoComm);
		edtxtLocoChecksum = (EditText) findViewById(R.id.edtxtLocoChecksum);
		chkbxLocoRaw = (CheckBox) findViewById(R.id.ckboxLocoRaw);
		btnLocoSend = (Button) findViewById(R.id.btnLocoSend);
		spnSwitch = (Spinner) findViewById(R.id.spnSwitch);
		btnAddSwitch = (Button) findViewById(R.id.btnAddSwitch);
		btnSwitchPosition = (Button) findViewById(R.id.btnSwitchPosition);
		txtvwIMUData = (TextView) findViewById(R.id.txtvwIMUData);

		trainNum = new int[MAX_TRAINS];
		switchAddress = new int[MAX_SWITCHES];

		chkbxRawComm.setChecked(false);
		chkbxLocoRaw.setChecked(false);
		edtxtCustomCommAdd.setEnabled(chkbxRawComm.isChecked());
		edtxtCustomCommCommand.setEnabled(chkbxRawComm.isChecked());
		edtxtLocoOpCode.setEnabled(chkbxLocoRaw.isChecked());
		edtxtLocoAddress.setEnabled(chkbxLocoRaw.isChecked());
		edtxtLocoCommand.setEnabled(chkbxLocoRaw.isChecked());
		edtxtLocoChecksum.setEnabled(chkbxLocoRaw.isChecked());

		skbarSpeed.setMax(31);
		btnDirection.setText("Forward");
		btnSwitchPosition.setText("Straight");
		btnLoconet.setTextOff("Diverge");
		btnLoconet.setTextOn("Straight");
		// edtxtCustomCommAdd.setText("");
		// edtxtCustomCommCommand.setText("");

		list = new ArrayList<String>();
		switchList = new ArrayList<String>();

		// default starting values
		for (int i = 1; i <= 5; i++) {

			trainNum[trainIndex] = i;
			list.add("Train " + String.valueOf(trainNum[trainIndex]));
			trainIndex++;
			switchAddress[switchIndex] = i - 1;
			switchList.add("Switch "
					+ String.valueOf(switchAddress[switchIndex]));
			switchIndex++;

		}
		switchAdapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_spinner_item, switchList);
		switchAdapter
				.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spnSwitch.setAdapter(switchAdapter);

		dataAdapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_spinner_item, list);
		dataAdapter
				.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spnTrain.setAdapter(dataAdapter);
		
		for (int i = 0; i < MAX_BUFFER; i++)
			for (int j = 0; j < CURRENT_DATA_SIZE; j++)
				currentBuffer[i][j] = 0;
		for (int i = 0; i < MAX_TRAINS; i++)
			for (int j = 0; j < MAX_BUFFER; j++)
				for (int k = 0; k < IMU_DATA_SIZE; k++)
					imuBuffer[i][j][k] = 0;

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
		edtxtCustomCommAdd.setText("");
		edtxtCustomCommCommand.setText("");

		
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
											switchAddress[switchIndex] = value;
											switchIndex++;
											switchList.add("Switch " + value);
										} else
											txtvwStatus
													.setText("Switch out of range.");
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
		} else
			txtvwStatus.setText("No more switches allowed.");
	}

	private void createCurrentThread() { //rename createBTRecieveThread
		currentThread = new Thread(new Runnable() {
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
								Log.d("OnTrack", "Inside createCurrentThread and bytes are: "+bytes);
								//byte[] header = new byte[1];
								
								if (bytes > 0) {
									// receive first byte
									inbuffer = new byte[bytes];
									Log.d("OnTrack", "number of bytes " + bytes);
									btInStream.read(inbuffer, 0, inbuffer.length);
								//	bytes = btInStream.available();
									Log.d("OnTrack", "Received message.");
									
									
									if (bytes == CURRENT_DATA_SIZE
											&& inbuffer[0] == CURRENT_HEADER) {
									

									//	btInStream.read(inbuffer, 0, bytes);

										// switch on inbuffer[0] should go here
										currentBuffer[currentBufferIndex
												% MAX_BUFFER][0] = inbuffer[1];
										currentBuffer[currentBufferIndex
												% MAX_BUFFER][1] = inbuffer[2];
										currentBufferIndex++;
										// spawn current calc thread.
										if (currentBufferIndex >= MAX_BUFFER)
											updateCurrent();
									} else if (bytes == IMU_DATA_SIZE
											&& inbuffer[0] == IMU_HEADER) {
										//inbuffer = new byte[bytes];

										Log.d("OnTrack", "Received IMU data.");
										//btInStream.read(inbuffer, 0, bytes);
										int imuTrainID = (int) (inbuffer[1 & 0xff]);
										for (int i = 0; i < IMU_DATA_SIZE- 2; i++)
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
		new Thread(new Runnable() {
			public void run() {
		
				byte[] acelXBytes = new byte[4];
				byte[] acelYBytes = new byte[4];
				byte[] acelZBytes = new byte[4];
				byte[] gyroYawBytes = new byte[4];
				byte[] gyroPitchBytes = new byte[4];
				byte[] gyroRawBytes = new byte[4];
				byte[] tieCountBytes = new byte[4];
				byte[] imuChecksumBytes = new byte[4];
		
				for (int i = 0; i < 4; i++) {
					acelXBytes[i] = imuBuffer[imuTrainID][0][i];
					acelYBytes[i] = imuBuffer[imuTrainID][0][i + 4];
					acelZBytes[i] = imuBuffer[imuTrainID][0][i + 8];
					gyroYawBytes[i] = imuBuffer[imuTrainID][0][i + 12];
					gyroPitchBytes[i] = imuBuffer[imuTrainID][0][i + 16];
					gyroRawBytes[i] = imuBuffer[imuTrainID][0][i + 20];
					tieCountBytes[i] = imuBuffer[imuTrainID][0][i + 24];
					imuChecksumBytes[i] = imuBuffer[imuTrainID][0][i + 28];
		
				}
				Log.d("OnTrack", "Inside updateIMU function.");
				
				
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
				
				final float gyroRaw = ByteBuffer.wrap(gyroRawBytes)
						.order(ByteOrder.LITTLE_ENDIAN).getFloat();
				
				final int tieCount = ByteBuffer.wrap(tieCountBytes)
						.order(ByteOrder.LITTLE_ENDIAN).getInt();
				final float imuChecksum = ByteBuffer.wrap(imuChecksumBytes)
						.order(ByteOrder.LITTLE_ENDIAN).getFloat();
		
				txtvwIMUData.post(new Runnable() {
		
					@Override
					public void run() {
						txtvwIMUData.setText("Train #" + imuTrainID + ": acel x"
								+ String.valueOf(acelX) + ", acel y " + String.valueOf(acelY)
								+ ", acel z " + String.valueOf(acelZ) + ", gyroYaw " + String.valueOf(gyroYaw) + 
								", gyroPitch " + String.valueOf(gyroPitch) + ", gyroRaw " + String.valueOf(gyroRaw) +
								", tieCount " + String.valueOf(tieCount) + ", imuChecksum "
								+ String.valueOf(imuChecksum));
		
					}
				});
			}
		});
	
	}

	private void updateCurrent() {
		new Thread(new Runnable() {
			public void run() {
				int current[] = new int[MAX_BUFFER];
				int sum = 0;
				Log.d("OnTrack", "Current: being calc");
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
				txtvwCurrent.post(new Runnable() {
		
					@Override
					public void run() {
						txtvwCurrent.setText(String.valueOf(update) + " mA");
					}
				});
			}
		});
	}

	public void btnSendListener() {
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
			// testing one more time.
			int address = 0;
			int speed = 0;
			int commandbits = 0;
			if (edtxtCustomCommAdd.getText().toString().length() > 0
					&& edtxtCustomCommCommand.getText().toString().length() > 0
					&& chkbxRawComm.isChecked()) {
				// txtvwStatus.setText("Sending... \" train# 0x"+
				// Integer.toHexString(buffer[0]) + " commandbits: 0x"+
				// Integer.toHexString(buffer[1]) + " checksum: 0x" +
				// Integer.toHexString(buffer[2]) + "\".");

				try {
					// address =
					// Integer.parseInt(edtxtCustomCommAdd.getText().toString());
					// commandbits =
					// Integer.parseInt(edtxtCustomCommCommand.getText().toString());
					address = Integer.parseInt(edtxtCustomCommAdd.getText()
							.toString(), 16);
					commandbits = Integer.parseInt(edtxtCustomCommCommand
							.getText().toString(), 16);
				} catch (NumberFormatException nfe) {
					System.out.println("Could not parse " + nfe);
					txtvwStatus.setText("Could not Parse");
				}
			}
			/*
			 * else if(edtxtCustomCommAdd.getText().toString().length() > 0 &&
			 * edtxtCustomCommCommand.getText().toString().length() > 0){ try {
			 * address =
			 * Integer.parseInt(edtxtCustomCommAdd.getText().toString()); speed
			 * = Integer.parseInt(edtxtCustomCommCommand.getText().toString());
			 * } catch(NumberFormatException nfe) {
			 * System.out.println("Could not parse " + nfe);
			 * txtvwStatus.setText("Could not Parse"); } if(speed <= 31 && speed
			 * >= 0){ for (int i = 1; i <= speed; i++){ commandbits^= 0x10; if
			 * (i%2 == 0) commandbits+= 0x01; }
			 * 
			 * 
			 * if(btnDirection.getText() == "Forward" ) commandbits^= 96; //64
			 * is for the 01 in the packet format 32 for the forward direction
			 * else commandbits^= 64; } else{ txtvwStatus.setText(
			 * "Speed not in range for non-raw input, please use 0-32"); }
			 * 
			 * }
			 */
			else {
				address = trainNum[spnTrain.getSelectedItemPosition()];
				speed = skbarSpeed.getProgress();
				for (int i = 1; i <= speed; i++) {
					commandbits ^= 0x10;
					if (i % 2 == 0)
						commandbits += 0x01;
				}

				if (btnDirection.getText() == "Forward")
					commandbits ^= 96; // 64 is for the 01 in the packet format
				// 32 for the forward direction
				else
					commandbits ^= 64;
			}
			int checksum = address ^ commandbits;
			try {
				sendMessage(address, commandbits, checksum);
			} finally {
				Log.d("OnTrack", "Sent Message");
			}
		} else
			txtvwStatus.setText("Bluetooth Not Connected!");
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
											trainNum[trainIndex] = value;
											trainIndex++;
											list.add("Train " + value);
										} else
											txtvwStatus
													.setText("Train out of range.");
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
		} else
			txtvwStatus.setText("No more trains allowed.");
	}

	private void CurrentRequest() throws IOException {

		if (btconnected && btSocket != null && btSocket.isConnected()) {

			Log.d("OnTrack", "Current: connection is good!");
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

					Log.d("OnTrack", "Current: Outputting via bluetooth");
					btOutStream.write(outbuffer);
					btOutStream.flush();
				} catch (IOException e) {
					e.printStackTrace();
				}
			/*
			 * while (btconnected && btInStream != null &&
			 * btInStream.available() >= 2) { Log.d("OnTrack",
			 * "Current: reading now."); try { Log.d("OnTrack",
			 * "Current: before read"); btInStream.read(inbuffer, 0, 2); int
			 * current = (inbuffer[0] & 0xff); current = current << 8; current =
			 * current & 0xff00; current = current | (inbuffer[1] & 0xff);
			 * current = current & 0xffff; current = (int) (current * 4.296);
			 * 
			 * txtvwCurrent.setText(String.valueOf(current) + " mA");
			 * Log.d("OnTrack", "Current: " + String.valueOf(current) + " mA");
			 * Log.d("OnTrack", "Current: byte1 :" +
			 * Integer.toHexString(inbuffer[0]) + " byte 2: " +
			 * Integer.toHexString(inbuffer[1] & 0xff));
			 * 
			 * } catch (IOException e) { e.printStackTrace(); } }
			 */
		}

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
			if (edtxtLocoOpCode.getText().length() > 0
					&& edtxtLocoAddress.getText().length() > 0
					&& edtxtLocoCommand.getText().length() > 0
					&& edtxtLocoChecksum.getText().length() > 0
					&& chkbxLocoRaw.isChecked()) {
				opcode = Integer.parseInt(edtxtLocoOpCode.getText().toString(),
						16);
				address = Integer.parseInt(edtxtLocoAddress.getText()
						.toString(), 16);
				command = Integer.parseInt(edtxtLocoCommand.getText()
						.toString(), 16);
				checksum = Integer.parseInt(edtxtLocoChecksum.getText()
						.toString(), 16);
			} else {
				simpleSwitchAddress = switchAddress[spnSwitch
						.getSelectedItemPosition()];
				address = simpleSwitchAddress & 0x7f;
				command = ((simpleSwitchAddress >> 7) & 0x0f);
				if (btnSwitchPosition.getText().toString() == "Straight")
					command = (0x30 | (command & 0x0f));
				else
					command = (0x10 | (command & 0x0f));
			}
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

	private void btnLoconetHardCoded() {
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
			if (btnLoconet.getText().toString() == "Diverge") {
				int loconet = 0x01;
				int opcode = 0xb0;
				int address = 0x00;
				int command = 0x30;
				int checksum = 0x7f;
				int endbyte = 0x0a;
				txtvwStatus.setText("Sending... \" address# 0x"
						+ Integer.toHexString(address) + " commandbits: 0x"
						+ Integer.toHexString(command) + " checksum: 0x"
						+ Integer.toHexString(checksum) + "\".");
				byte[] outbuffer = new byte[6];
				outbuffer[0] = (byte) (loconet & 0xff);
				outbuffer[1] = (byte) (opcode & 0xff);
				outbuffer[2] = (byte) (address & 0xff);
				outbuffer[3] = (byte) (command & 0xff);
				outbuffer[4] = (byte) (checksum & 0xff);
				outbuffer[5] = (byte) (endbyte & 0xff);
				txtvwStatus.setText("Sending... \" address# 0x"
						+ Integer.toHexString(outbuffer[1])
						+ " commandbits: 0x"
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
			} else {
				int loconet = 0x01;
				int opcode = 0xb0;
				int address = 0x00;
				int command = 0x10;
				int checksum = 0x5f;
				int endbyte = 0x0a;
				txtvwStatus.setText("Sending... \" address# 0x"
						+ Integer.toHexString(address) + " commandbits: 0x"
						+ Integer.toHexString(command) + " checksum: 0x"
						+ Integer.toHexString(checksum) + "\".");
				byte[] outbuffer = new byte[6];
				outbuffer[0] = (byte) (loconet & 0xff);
				outbuffer[1] = (byte) (opcode & 0xff);
				outbuffer[2] = (byte) (address & 0xff);
				outbuffer[3] = (byte) (command & 0xff);
				outbuffer[4] = (byte) (checksum & 0xff);
				outbuffer[5] = (byte) (endbyte & 0xff);
				txtvwStatus.setText("Sending... \" address# 0x"
						+ Integer.toHexString(outbuffer[1])
						+ " commandbits: 0x"
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

		}

	}

	private void setupButtonListeners() {

		btnSend.setOnClickListener(new View.OnClickListener() {
			/*
			 * (non-Javadoc)
			 * 
			 * @see android.view.View.OnClickListener#onClick(android.view.View)
			 * Description: Send button handler. Expected Results: Capture
			 * current train being selected, calculate speed value from the seek
			 * bar position. calculate checksum from train address and speed
			 * information. Call send message function with formed message.
			 */
			@Override
			public void onClick(View arg0) {
				btnSendListener();
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
			}
		});

		btnLoconet.setOnClickListener(new View.OnClickListener() {
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
				btnLoconetHardCoded();
			}
		});

		btnDirection.setOnClickListener(new View.OnClickListener() {
			/*
			 * (non-Javadoc)
			 * 
			 * @see android.view.View.OnClickListener#onClick(android.view.View)
			 * Description: On click listener for direction button. Expected
			 * Results: Click button to toggle between forward and reverse.
			 * Logic is inside the send function to control messages related to
			 * direction, simply toggles the button text.
			 */
			@Override
			public void onClick(View arg0) {
				if (btnDirection.getText() == "Forward")
					btnDirection.setText("Reverse");
				else
					btnDirection.setText("Forward");
			}
		});
		btnSwitchPosition.setOnClickListener(new View.OnClickListener() {
			/*
			 * (non-Javadoc)
			 * 
			 * @see android.view.View.OnClickListener#onClick(android.view.View)
			 * Description: On click listener for direction button. Expected
			 * Results: Click button to toggle between forward and reverse.
			 * Logic is inside the send function to control messages related to
			 * direction, simply toggles the button text.
			 */
			@Override
			public void onClick(View arg0) {
				if (btnSwitchPosition.getText() == "Straight")
					btnSwitchPosition.setText("Divert");
				else
					btnSwitchPosition.setText("Straight");
			}
		});
		btnAddSwitch.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				btnAddSwitchListener();

			}
		});

		skbarSpeed
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

		chkbxRawComm.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				edtxtCustomCommAdd.setEnabled(chkbxRawComm.isChecked());
				edtxtCustomCommCommand.setEnabled(chkbxRawComm.isChecked());
				spnTrain.setEnabled(!chkbxRawComm.isChecked());
				btnAdd.setEnabled(!chkbxRawComm.isChecked());
				skbarSpeed.setEnabled(!chkbxRawComm.isChecked());
				btnDirection.setEnabled(!chkbxRawComm.isChecked());
			}
		});

		chkbxLocoRaw.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				edtxtLocoOpCode.setEnabled(chkbxLocoRaw.isChecked());
				edtxtLocoAddress.setEnabled(chkbxLocoRaw.isChecked());
				edtxtLocoCommand.setEnabled(chkbxLocoRaw.isChecked());
				edtxtLocoChecksum.setEnabled(chkbxLocoRaw.isChecked());
			}
		});

		btnLocoSend.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				btnLocoSendListener();

			}
		});

		btnConnect.setOnClickListener(new View.OnClickListener() {
			/*
			 * (non-Javadoc)
			 * 
			 * @see android.view.View.OnClickListener#onClick(android.view.View)
			 * Description: Send button handler. Expected Results: Capture
			 * current train being selected, calculate speed value from the seek
			 * bar position. calculate checksum from train address and speed
			 * information. Call send message function with formed message.
			 */
			@Override
			public void onClick(View arg0) {
				new BTConnect().execute();
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
}
