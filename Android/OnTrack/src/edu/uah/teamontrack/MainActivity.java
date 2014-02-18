package edu.uah.teamontrack;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

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
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends Activity {
	private String address = null; // default address will be changed
	// later in code
	private static final UUID MY_UUID = UUID
			.fromString("00001101-0000-1000-8000-00805F9B34FB"); // default
	// serial
	// port
	// profile
	// UUID
	private static final String TAG = "OnTrack";

	TextView txtvwBTStream;
	TextView txtvwStatus;
	Button btnBTConnect;

	List<String> addressList; // string list to hold all bt device addresses

	BluetoothAdapter btAdapter = null; // start all BT items as null to start.
	BluetoothDevice btDevice = null;
	BluetoothSocket btSocket = null;
	OutputStream btOutStream = null;
	InputStream btInStream = null;
	Boolean btconnected = false;

	BroadcastReceiver bcastReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			BluetoothDevice device = intent
					.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

			if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
				// Device has disconnected
				btconnected = false;
				txtvwStatus.setText("Disconnected");
				try {
					btSocket.close();
					btSocket = null;
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
				btconnected = true;
				txtvwBTStream.setText("Connected");
			}

		}

	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		txtvwBTStream = (TextView) findViewById(R.id.txtvwBTStream);
		txtvwStatus = (TextView) findViewById(R.id.txtvwStatus);
		btnBTConnect = (Button) findViewById(R.id.btnBTConnect);

		btnBTConnect.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				new BTConnect().execute();
			}
		});

		// btsetup
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
		// intent filters
		IntentFilter f1 = new IntentFilter(
				BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED);
		IntentFilter f2 = new IntentFilter(
				BluetoothDevice.ACTION_ACL_DISCONNECTED);
		IntentFilter f3 = new IntentFilter(BluetoothDevice.ACTION_ACL_CONNECTED);
		this.registerReceiver(bcastReceiver, f1);
		this.registerReceiver(bcastReceiver, f2);
		this.registerReceiver(bcastReceiver, f3);

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

			if (curTry >= maxTries)
				txtvwStatus.setText("Connection Unsuccessful!");
			else {
				txtvwStatus.setText("Connection Successful!");
				new Thread(new Runnable() {
					public void run() {
						
						try {
							btInStream = btSocket.getInputStream();
						} catch (IOException e) {
							e.printStackTrace();
						}
						// Keep listening to the InputStream until an exception
						// occurs
						while (true) {
							try {
								int bytes = 0;
								bytes = btInStream.available();
								byte[] inbuffer = new byte[bytes];
								if (btconnected && btInStream != null
										&& bytes > 0) {

									Log.d("OnTrack", "Current: before read Bytes avail: " + String.valueOf(bytes));
									btInStream.read(inbuffer,0,bytes);
									/*int current = (inbuffer[0] & 0xff);
									current = current << 8;
									current = current & 0xff00;
									current = current | (inbuffer[1] & 0xff);
									current = current & 0xffff;
									current = (int) (current * 4.296);
									final int update = current;*/
									final String update = new String(inbuffer, 0, bytes);
									txtvwBTStream.post(new Runnable() {
										
										@Override
										public void run() {
											txtvwBTStream.append(update);
										}
									});
									/*Log.d("OnTrack",
											"Current: "
													+ String.valueOf(current)
													+ " mA");
									Log.d("OnTrack",
											"Current: byte1 :"
													+ Integer
															.toHexString(inbuffer[0])
													+ " byte 2: "
													+ Integer
															.toHexString(inbuffer[1] & 0xff));*/

								}
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
					}
				}).start();
			}
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

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

}
