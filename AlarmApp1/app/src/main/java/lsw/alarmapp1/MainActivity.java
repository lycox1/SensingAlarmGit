package lsw.alarmapp1;


import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

import adparser.AdElement;
import adparser.AdParser;
//import lsw.alarmapp1.R;

public class MainActivity extends AppCompatActivity {

//public class MainActivity extends ListActivity {

    int hour, minute;
    TextView deviceNameView, addressView, rssiView;

    private static final String LOG_TAG = "BLEScan";
    private LeDeviceListAdapter mLeDeviceListAdapter;
    private Handler mHandler;
    private boolean mScanning;
    private BluetoothAdapter mBluetoothAdapter;

    private static final int REQUEST_ENABLE_BT = 1;
    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 10000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

//        getActionBar().setTitle("BLE Device Scan");

        findViewById(R.id.alarmSet).setOnClickListener(mClickListener);
        deviceNameView = (TextView)findViewById(R.id.DeviceName);
        addressView = (TextView)findViewById(R.id.Address);
        rssiView = (TextView)findViewById(R.id.Rssi);

        mHandler = new Handler();

        // Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "BLE can not be supported in this device.", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            Toast.makeText(this, "BLE can be supported in this device.", Toast.LENGTH_SHORT).show();
        }

        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        // Checks if Bluetooth is supported on the device.
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "This device don't have bluetooth adapter", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        if (!mScanning) {
            menu.findItem(R.id.menu_stop).setVisible(false);
            menu.findItem(R.id.menu_scan).setVisible(true);
            menu.findItem(R.id.menu_refresh).setActionView(null);
        } else {
            menu.findItem(R.id.menu_stop).setVisible(true);
            menu.findItem(R.id.menu_scan).setVisible(false);
            menu.findItem(R.id.menu_refresh).setActionView(
                    R.layout.actionbar_indeterminate_progress); // res/layout/actionbar_indeterminate_progress.xml
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_scan:
                Log.d(LOG_TAG, "onOptionsItemSelected menu_scan");
                mLeDeviceListAdapter.clear();
                scanLeDevice(true);
                break;
            case R.id.menu_stop:
                Log.d(LOG_TAG, "onOptionsItemSelected menu_stop");
                scanLeDevice(false);
                break;
        }
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
        // fire an intent to display a dialog asking the user to grant permission to enable it.
        if (!mBluetoothAdapter.isEnabled()) {
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        }

        // Initializes list view adapter.
        mLeDeviceListAdapter = new LeDeviceListAdapter();
//        setListAdapter(mLeDeviceListAdapter);
        scanLeDevice(true);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // User chose not to enable Bluetooth.
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            finish();
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onPause() {
        super.onPause();
        scanLeDevice(false);
        mLeDeviceListAdapter.clear();
    }

/*    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        final BluetoothDevice device = mLeDeviceListAdapter.getDevice(position);
        if (device == null) return;
*/
/*        final Intent intent = new Intent(this, DeviceControlActivity.class);
        intent.putExtra(DeviceControlActivity.EXTRAS_DEVICE_NAME, device.getName());
        intent.putExtra(DeviceControlActivity.EXTRAS_DEVICE_ADDRESS, device.getAddress());
        if (mScanning) {
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
            mScanning = false;
        }
        startActivity(intent);
        */
//    }

    private void scanLeDevice(final boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    invalidateOptionsMenu();
                }
            }, SCAN_PERIOD);

            mScanning = true;
            mBluetoothAdapter.startLeScan(mLeScanCallback);
        } else {
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }
        Log.d(LOG_TAG, "scanLeDevice " + enable );
		invalidateOptionsMenu();
	}

    static class ViewHolder {
        TextView deviceName;
        TextView deviceAd;
        TextView deviceRssi;
        TextView deviceAddress;
    }

    class DeviceHolder {
        BluetoothDevice device;
        String additionalData;
        int rssi;

        public DeviceHolder(BluetoothDevice device, String additionalData, int rssi) {
            this.device = device;
            this.additionalData = additionalData;
            this.rssi = rssi;
        }
    }
    // Adapter for holding devices found through scanning.
    private class LeDeviceListAdapter extends BaseAdapter {
        private ArrayList<BluetoothDevice> mLeDevices;
        private ArrayList<DeviceHolder> mLeHolders;
        private LayoutInflater mInflator;

        public LeDeviceListAdapter() {
            super();
            mLeDevices = new ArrayList<BluetoothDevice>();
            mLeHolders = new ArrayList<DeviceHolder>();
            mInflator = MainActivity.this.getLayoutInflater();
        }

        public void addDevice(DeviceHolder deviceHolder) {
            if(!mLeDevices.contains(deviceHolder.device)) {
                mLeDevices.add(deviceHolder.device);
                mLeHolders.add(deviceHolder);
            }
        }

        public BluetoothDevice getDevice(int position) {
            return mLeDevices.get(position);
        }

        public void clear() {
            mLeDevices.clear();
            mLeHolders.clear();
        }

        @Override
        public int getCount() {
            return mLeDevices.size();
        }

        @Override
        public Object getItem(int i) {
            return mLeDevices.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            Log.d(LOG_TAG, "getView() is called ");
            ViewHolder viewHolder;
            // General ListView optimization code.
            if (view == null) {
                view = mInflator.inflate(R.layout.listitem_device, null);
                viewHolder = new ViewHolder();
                viewHolder.deviceAddress = (TextView) view.findViewById(R.id.device_address);
                viewHolder.deviceAd = (TextView) view.findViewById(R.id.device_ad);
                viewHolder.deviceRssi = (TextView) view.findViewById(R.id.device_rssi);
                viewHolder.deviceName = (TextView) view.findViewById(R.id.device_name);
                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) view.getTag();
            }

            BluetoothDevice device = mLeDevices.get(i);
            DeviceHolder deviceHolder = mLeHolders.get(i);
            final String deviceName = device.getName();
            if (deviceName != null && deviceName.length() > 0)
                viewHolder.deviceName.setText(deviceName);
            else
                viewHolder.deviceName.setText(R.string.unknown_device);
            viewHolder.deviceAddress.setText(device.getAddress());
            viewHolder.deviceAd.setText(deviceHolder.additionalData);
            viewHolder.deviceRssi.setText("rssi: "+Integer.toString(deviceHolder.rssi));
            return view;
        }
    }
    // Device scan callback.
    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {

                @Override
                public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
                    String deviceName = device.getName();
                    StringBuffer b = new StringBuffer();
                    int byteCtr = 0;
                    for( int i = 0 ; i < scanRecord.length ; ++i ) {
                        if( byteCtr > 0 )
                            b.append( " ");
                        b.append( Integer.toHexString( ((int)scanRecord[i]) & 0xFF));
                        ++byteCtr;
                        if( byteCtr == 8 ) {
                            Log.d(LOG_TAG, new String(b));
                            byteCtr = 0;
                            b = new StringBuffer();
                        }
                    }
                    ArrayList<AdElement> ads = AdParser.parseAdData(scanRecord);
                    StringBuffer sb = new StringBuffer();
                    for( int i = 0 ; i < ads.size() ; ++i ) {
                        AdElement e = ads.get(i);
                        if( i > 0 )
                            sb.append(" ; ");
                        sb.append(e.toString());
                    }
                    String additionalData = new String(sb);
                    Log.d(LOG_TAG, "additionalData: " + additionalData);
                    DeviceHolder deviceHolder = new DeviceHolder(device,additionalData,rssi);

                    runOnUiThread(new DeviceAddTask(deviceHolder));


                }
            };

    class DeviceAddTask implements Runnable {
        DeviceHolder deviceHolder;

        public DeviceAddTask( DeviceHolder deviceHolder ) {
            this.deviceHolder = deviceHolder;
        }

        public void run() {
            String recoDevice = new String("RECO");
//            Log.d(LOG_TAG, "DeviceAddTask recoDevice " + deviceHolder.device.getName());

            if(deviceHolder.device.getName() != null) {
                if (deviceHolder.device.getName().equals(recoDevice)) {
                    Log.d(LOG_TAG, "DeviceAddTask deviceNameView setText " + deviceHolder.device.getName());
                    deviceNameView.setText("device name : " + deviceHolder.device.getName());
                    Log.d(LOG_TAG, "DeviceAddTask addressView setText " + deviceHolder.device.getAddress());
                    addressView.setText("address : " + deviceHolder.device.getAddress());
                    Log.d(LOG_TAG, "DeviceAddTask rssiView setText " + Integer.toString(deviceHolder.rssi));
                    rssiView.setText("rssi : " + Integer.toString(deviceHolder.rssi));
                }
            }
            mLeDeviceListAdapter.addDevice(deviceHolder);
            mLeDeviceListAdapter.notifyDataSetChanged();
        }
    }

    Button.OnClickListener mClickListener = new View.OnClickListener() {
        public void onClick(View v) {
            EditText hour = (EditText)findViewById(R.id.alarmHour);
            EditText minute = (EditText)findViewById(R.id.alarmMinute);
            switch (v.getId()) {
                case R.id.alarmSet:
                    String hourString = hour.getText().toString();
                    String minutesString = minute.getText().toString();
                    Toast.makeText(MainActivity.this, hourString, Toast.LENGTH_SHORT).show();
                    Toast.makeText(MainActivity.this, minutesString, Toast.LENGTH_SHORT).show();

                    Intent intent = new Intent(MainActivity.this,
                            AlarmReceive.class);
                    PendingIntent pender = PendingIntent.getBroadcast(
                            MainActivity.this, 0, intent, 0);

                    break;
            }
        }
    };

}
