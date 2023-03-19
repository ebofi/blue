package com.ru.cordova.printer.bluetooth;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Hashtable;
import java.util.Set;
import java.util.UUID;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Handler;
import android.util.Log;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Bitmap.Config;
import android.util.Xml.Encoding;
import android.util.Base64;

import java.nio.charset.Charset;

import androidx.core.app.ActivityCompat;
import androidx.core.content.PermissionChecker;

public class BluetoothPrinter extends CordovaPlugin {
    private static final String LOG_TAG = "BluetoothPrinter";
    public static final int REQUEST_BLUETOOTH_PERMISSION = 1;

    BluetoothAdapter mBluetoothAdapter;
	BluetoothSocket mmSocket;
	BluetoothDevice mmDevice;
	
    OutputStream mmOutputStream;
	InputStream mmInputStream;
	
    Thread workerThread;
	byte[] readBuffer;
	int readBufferPosition;
	int counter;
	volatile boolean stopWorker;

	Bitmap bitmap;

	public BluetoothPrinter() {}

	@Override
	public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        
        if (action.equals("status")) 
        {
            if (PermissionChecker.checkSelfPermission(this.cordova.getContext(), android.Manifest.permission.BLUETOOTH_SCAN) != PermissionChecker.PERMISSION_GRANTED) {  
                ActivityCompat.requestPermissions(
                    this.cordova.getActivity(),    
                    new String[] { android.Manifest.permission.BLUETOOTH_SCAN, android.Manifest.permission.BLUETOOTH_CONNECT },
                    REQUEST_BLUETOOTH_PERMISSION
                );
            }

            checkBTStatus(callbackContext);
            return true;
        }
        else if( action.equals("list") )
        {
			listBT(callbackContext);
			return true;
		} 
        else if( action.equals("connect") )
        {
			String name = args.getString(0);
			if( findBT(callbackContext, name ) ) 
            {
				try 
                {
					connectBT(callbackContext);
				} 
                catch (IOException e) 
                {
					Log.e(LOG_TAG, e.getMessage());
					e.printStackTrace();
				}
			} 
            else 
            {
				callbackContext.error("Bluetooth Device Not Found: " + name);
			}

			return true;
		} 
        else if( action.equals("disconnect") ) {
            try 
            {
                disconnectBT(callbackContext);
            } 
            catch (IOException e) 
            {
                Log.e(LOG_TAG, e.getMessage());
                e.printStackTrace();
            }

            return true;
        }
	    else if( action.equals("print") ) 
        {
			try 
            {
				String msg = args.getString(0);
				print(callbackContext, msg);
			} 
            catch( IOException e ) 
            {
				Log.e(LOG_TAG, e.getMessage());
				e.printStackTrace();
			}

            return true;
		}
        else if( action.equals("printPOSCommand") ) 
        {
			try 
            {
				String msg = args.getString(0);
                printPOSCommand(callbackContext, hexStringToBytes(msg));
			} 
            catch (IOException e) 
            {
				Log.e(LOG_TAG, e.getMessage());
				e.printStackTrace();
			}

			return true;
		}

		return false;
	}

    /**
	 * checkBTStatus: This will return the status of BT adapter: true or false
	 *
	 * @param callbackContext
	 */
    boolean checkBTStatus(CallbackContext callbackContext) 
    {
        try 
        {
            mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if(mBluetoothAdapter.isEnabled()) 
            {
                callbackContext.success("true");
                return true;
            } 
            else 
            {
                callbackContext.success("false");
                return false;
            }
        } 
        catch (Exception e) 
        {
            String errMsg = e.getMessage();
            Log.e(LOG_TAG, errMsg);
            e.printStackTrace();
            callbackContext.error(errMsg);
        }

        return false;
    }

	/**
	 * listBT: This will return the array list of paired bluetooth printers
	 *
	 * @param callbackContext
	 */
	void listBT(CallbackContext callbackContext) 
    {
		BluetoothAdapter mBluetoothAdapter = null;
		String errMsg = null;
		try 
        {
			mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

			if( mBluetoothAdapter == null ) 
            {
				errMsg = "No bluetooth adapter available";
				Log.e(LOG_TAG, errMsg);
				callbackContext.error(errMsg);
				return;
			}

			if(!mBluetoothAdapter.isEnabled()) 
            {
				Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
				this.cordova.getActivity().startActivityForResult(enableBluetooth, 0);
			}

			Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
			if( pairedDevices.size() > 0 ) 
            {
				JSONArray json = new JSONArray();
				for (BluetoothDevice device : pairedDevices) 
                {
                    Log.v(LOG_TAG, "DEVICE getName-> " + device.getName());
                    Log.v(LOG_TAG, "DEVICE getAddress-> " + device.getAddress());
                    Log.v(LOG_TAG, "DEVICE getType-> " + device.getType());

					json.put(device.getName());
				}

				callbackContext.success(json);
			} 
            else 
            {
				callbackContext.error("No Bluetooth Device Found");
			}
		}
		catch (Exception e) 
        {
			errMsg = e.getMessage();
			Log.e(LOG_TAG, errMsg);
			e.printStackTrace();
			callbackContext.error(errMsg);
		}
	}

	/**
	 * findBT: This will find a bluetooth printer device
	 *
	 * @param callbackContext
	 * @param name of the device connected
	 * @return
	 */
	boolean findBT(CallbackContext callbackContext, String name) 
    {
		try
        {
			mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
			if(mBluetoothAdapter == null) 
            {
				Log.e(LOG_TAG, "No bluetooth adapter available");
			}

			if(!mBluetoothAdapter.isEnabled()) 
            {
				Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
				this.cordova.getActivity().startActivityForResult(enableBluetooth, 0);
			}

			Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
			if (pairedDevices.size() > 0) 
            {
				for( BluetoothDevice device : pairedDevices )
                {
					if( device.getName().equalsIgnoreCase(name) ) 
                    {
						mmDevice = device;
						return true;
					}
				}
			}

			Log.d(LOG_TAG, "Bluetooth Device Found: " + mmDevice.getName());
		}
		catch (Exception e) 
        {
			String errMsg = e.getMessage();
			Log.e(LOG_TAG, errMsg);
			e.printStackTrace();
			callbackContext.error(errMsg);
		}

		return false;
	}

    /**
     * connectBT: Tries to open a connection to the bluetooth printer device
     *
     * @param callbackContext
     * @return
     * @throws IOException
     */
	boolean connectBT(CallbackContext callbackContext) throws IOException 
    {
		try 
        {
			// Standard SerialPortService ID
			UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
			mmSocket = mmDevice.createRfcommSocketToServiceRecord(uuid);
			mmSocket.connect();
			mmOutputStream = mmSocket.getOutputStream();
			mmInputStream = mmSocket.getInputStream();
			beginListenForData();
			Log.d(LOG_TAG, "Bluetooth Opened: " + mmDevice.getName());
			callbackContext.success("Bluetooth Opened: " + mmDevice.getName());
			return true;
		}
		catch (Exception e) 
        {
			String errMsg = e.getMessage();
			Log.e(LOG_TAG, errMsg);
			e.printStackTrace();
			callbackContext.error(errMsg);
		}

		return false;
	}

    /**
     * beginListenForData: After opening a connection to bluetooth printer device,
     * we have to listen and check if a data were sent to be printed.
     */
	private void beginListenForData() 
    {
		try 
        {
			final Handler handler = new Handler();
			// This is the ASCII code for a newline character
			final byte delimiter = 10;
			stopWorker = false;
			readBufferPosition = 0;
			readBuffer = new byte[1024];
			workerThread = new Thread(new Runnable() {
				public void run() {
					while (!Thread.currentThread().isInterrupted() && !stopWorker) {
						try {
							int bytesAvailable = mmInputStream.available();
							if (bytesAvailable > 0) {
								byte[] packetBytes = new byte[bytesAvailable];
								mmInputStream.read(packetBytes);
								for (int i = 0; i < bytesAvailable; i++) {
									byte b = packetBytes[i];
									if (b == delimiter) {
										byte[] encodedBytes = new byte[readBufferPosition];
										System.arraycopy(readBuffer, 0, encodedBytes, 0, encodedBytes.length);
									} else {
										readBuffer[readBufferPosition++] = b;
									}
								}
							}
						} catch (IOException ex) {
							stopWorker = true;
						}
					}
				}
			});
			workerThread.start();
		} 
        catch (NullPointerException e) 
        {
			e.printStackTrace();
		} 
        catch (Exception e) 
        {
			e.printStackTrace();
		}
	}

    /**
     * print: This will send text data to bluetooth printer
     *
     * @param callbackContext
     * @param msg
     * @return
     * @throws IOException
     */
	boolean print(CallbackContext callbackContext, String msg) throws IOException 
    {
		try 
        {
			// Force Portuguese Encoding
			byte[] bytes = msg.getBytes (Charset.forName("IBM860"));
			mmOutputStream.write(bytes);

			// tell the user data were sent
			Log.d(LOG_TAG, "Data Sent");
			callbackContext.success("Data Sent");

			return true;
		}
		catch (Exception e) 
        {
			String errMsg = e.getMessage();
			Log.e(LOG_TAG, errMsg);
			e.printStackTrace();
			callbackContext.error(errMsg);
		}

		return false;
	}

    /**
     * printPOSCommand: This will send POS Command to bluetooth printer
     * @param callbackContext
     * @param buffer
     * @return
     * @throws IOException
     */
    boolean printPOSCommand(CallbackContext callbackContext, byte[] buffer) throws IOException {
        try {
            mmOutputStream.write(buffer);
            // tell the user data were sent
			Log.d(LOG_TAG, "Data Sent");
            callbackContext.success("Data Sent");
            return true;
        }
        catch (Exception e) {
            String errMsg = e.getMessage();
            Log.e(LOG_TAG, errMsg);
            e.printStackTrace();
            callbackContext.error(errMsg);
        }

        return false;
    }

    /**
     * disconnectBT: disconnect bluetooth printer.
     * @param callbackContext
     * @return
     * @throws IOException
     */
	boolean disconnectBT(CallbackContext callbackContext) throws IOException {
		try {
			stopWorker = true;
			mmOutputStream.close();
			mmInputStream.close();
			mmSocket.close();
            Log.d(LOG_TAG, "Bluetooth Disconnect");
			callbackContext.success("Bluetooth Disconnect");

			return true;
		}
		catch (Exception e) {
			String errMsg = e.getMessage();
			Log.e(LOG_TAG, errMsg);
			e.printStackTrace();
			callbackContext.error(errMsg);
		}

		return false;
	}

	private static byte[] hexStringToBytes(String hexString) {
        hexString = hexString.toLowerCase();
        String[] hexStrings = hexString.split(" ");
        byte[] bytes = new byte[hexStrings.length];
        for (int i = 0; i < hexStrings.length; i++) {
            char[] hexChars = hexStrings[i].toCharArray();
            bytes[i] = (byte) (charToByte(hexChars[0]) << 4 | charToByte(hexChars[1]));
        }
        return bytes;
    }

    private static byte charToByte(char c) {
		return (byte) "0123456789abcdef".indexOf(c);
	}

}
