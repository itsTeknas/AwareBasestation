package com.blackcurrantapps.awarebasestation.activities;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.blackcurrantapps.awarebasestation.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class BaseStationActivity extends AppCompatActivity {


    @BindView(R.id.device_number)
    TextView deviceNumber;
    private BluetoothAdapter mBluetoothAdapter;
    private int REQUEST_ENABLE_BT = 1;
    private Handler mHandler;
    private static final long SCAN_PERIOD = 10000;
    private BluetoothLeScanner mLEScanner;
    private FirebaseDatabase mFirebaseDatabase;

    @BindView(R.id.textView2)
    TextView textView2;
    @BindView(R.id.devicesTextView)
    TextView devicesTextView;
    @BindView(R.id.imageView)
    ImageView imageView;
    @BindView(R.id.searchStatus)
    TextView searchStatus;
    @BindView(R.id.activity_base_station)
    RelativeLayout activityBaseStation;

    protected PowerManager.WakeLock mWakeLock;

    private String rideKey = "thunder";
    private String rideName = "Thunder";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_base_station);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        ButterKnife.bind(this);

        rideKey = getIntent().getStringExtra("RIDE_KEY");
        rideName = getIntent().getStringExtra("RIDE_NAME");

        if (savedInstanceState == null) {
            try {
                FirebaseDatabase.getInstance().setPersistenceEnabled(true);
                FirebaseDatabase.getInstance().getReference("rides").child(rideKey).keepSynced(true);
            } catch (Exception ignore) {
            }
        }

        getSupportActionBar().setTitle("Base Station : " + rideName);

        mHandler = new Handler();

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "BLE Not Supported",
                    Toast.LENGTH_SHORT).show();
            finish();
        }
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        final PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        this.mWakeLock = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK, "My Tag");
        this.mWakeLock.acquire();
    }

    @Override
    public void onDestroy() {
        this.mWakeLock.release();
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else {
            mLEScanner = mBluetoothAdapter.getBluetoothLeScanner();
            scanLeDevice(true);
        }

        mFirebaseDatabase = FirebaseDatabase.getInstance();

        mFirebaseDatabase.getReference().child("rides").child(rideKey).child("nearby_devices").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                mFirebaseDatabase.getReference().child("rides").child(rideKey).child("current_nearby_devices_count").setValue(dataSnapshot.getChildrenCount());
                deviceNumber.setText(String.valueOf(dataSnapshot.getChildrenCount()));
                Iterable<DataSnapshot> devices = dataSnapshot.getChildren();
                String names = "";
                for (DataSnapshot device : devices) {
                    names = names + "\n" + device.getKey();
                }
                devicesTextView.setText(names);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                forceCleanup();
            }
        },30000);
    }

    private void forceCleanup(){

        final DatabaseReference mref = mFirebaseDatabase.getReference().child("rides").child(rideKey);

        mref.child("nearby_devices").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Iterable<DataSnapshot> devices = dataSnapshot.getChildren();
                for (DataSnapshot device : devices) {
                    Long updateTime = (Long) device.getValue();
                    if (System.currentTimeMillis() - updateTime > 60000) {
                        mref.child("nearby_devices").child(device.getKey()).setValue(null);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                forceCleanup();
            }
        },30000);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mBluetoothAdapter != null && mBluetoothAdapter.isEnabled()) {
            scanLeDevice(false);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == Activity.RESULT_CANCELED) {
                //Bluetooth not enabled.
                finish();
                return;
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void scanLeDevice(final boolean enable) {
        if (enable) {
            mLEScanner.startScan(new ArrayList<ScanFilter>(),
                    new ScanSettings.Builder()
                            .setScanMode(ScanSettings.SCAN_MODE_BALANCED)
                            .build(), mScanCallback);

            searchStatus.setText("SCANNING");

        } else {
            mLEScanner.stopScan(mScanCallback);
        }
    }

    private void registerDevices(ArrayList<String> devices) {
        final DatabaseReference mref = mFirebaseDatabase.getReference().child("rides").child(rideKey);

        DatabaseReference macIdRef = mFirebaseDatabase.getReference().child("mac_id");

        for (String mac : devices) {
            mref.child("nearby_devices").child(mac).setValue(System.currentTimeMillis());
            macIdRef.child(mac).child("nearest_base_station_key").setValue(rideKey);
            macIdRef.child(mac).child("nearest_base_station_name").setValue(rideName);
            macIdRef.child(mac).child("update_timestamp").setValue(System.currentTimeMillis());
            macIdRef.child(mac).child("visits").child(rideKey).setValue(1);
        }

        mref.child("nearby_devices").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Iterable<DataSnapshot> devices = dataSnapshot.getChildren();
                for (DataSnapshot device : devices) {
                    Long updateTime = (Long) device.getValue();
                    if (System.currentTimeMillis() - updateTime > 60000) {
                        mref.child("nearby_devices").child(device.getKey()).setValue(null);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private ScanCallback mScanCallback = new ScanCallback() {

        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            Log.i("callbackType", String.valueOf(callbackType));
            Log.i("result", result.toString());
            BluetoothDevice btDevice = result.getDevice();
            Log.v("Found Device", btDevice.getAddress());
            ArrayList<String> uids = new ArrayList<>();
            uids.add(btDevice.getAddress());
            registerDevices(uids);
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            ArrayList<String> uids = new ArrayList<>();
            for (ScanResult sr : results) {
                BluetoothDevice btDevice = sr.getDevice();
                Log.v("Found Device", btDevice.getAddress());
                uids.add(btDevice.getAddress());
            }
            registerDevices(uids);
        }

        @Override
        public void onScanFailed(int errorCode) {
            Log.e("Scan Failed", "Error Code: " + errorCode);
        }

    };
}
