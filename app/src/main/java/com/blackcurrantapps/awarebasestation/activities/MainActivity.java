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
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import com.blackcurrantapps.awarebasestation.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;


public class MainActivity extends AppCompatActivity {
    private BluetoothAdapter mBluetoothAdapter;
    private int REQUEST_ENABLE_BT = 1;
    private Handler mHandler;
    private static final long SCAN_PERIOD = 10000;
    private BluetoothLeScanner mLEScanner;
    private FirebaseDatabase mFirebaseDatabase;
    private Boolean firebaseInit = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mHandler = new Handler();
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "BLE Not Supported",
                    Toast.LENGTH_SHORT).show();
            finish();
        }
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        if (savedInstanceState == null) {
            try {
                FirebaseDatabase.getInstance().setPersistenceEnabled(true);
                FirebaseDatabase.getInstance().getReference("rides").child(rideName).keepSynced(true);
            } catch (Exception ignore) {
            }
        }
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

        FirebaseAuth.getInstance().signInAnonymously().addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                mFirebaseDatabase = FirebaseDatabase.getInstance();
                firebaseInit = true;
            }
        });
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
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mLEScanner.stopScan(mScanCallback);
                    Log.v("SCAN_STATUS","STOPPED SCAN");
                }
            }, SCAN_PERIOD);

            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mLEScanner.startScan(mScanCallback);
                    Log.v("SCAN_STATUS","STARTED SCAN");
                }
            }, SCAN_PERIOD + 5000);

            mLEScanner.startScan(new ArrayList<ScanFilter>() ,
                    new ScanSettings.Builder()
                        .setScanMode(ScanSettings.SCAN_MODE_BALANCED)
                        .build() , mScanCallback);

            Log.v("SCAN_STATUS","STARTED SCAN");

        } else {
            mLEScanner.stopScan(mScanCallback);
        }
    }

    private final String rideName = "thunder";

    private void registerDevices(ArrayList<String> devices){
        if (firebaseInit){
            final DatabaseReference mref = mFirebaseDatabase.getReference().child("rides").child(rideName);

            for (String mac : devices){
                mref.child("nearby_devices").child(mac).setValue(System.currentTimeMillis());
            }

            mref.child("nearby_devices").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    Iterable<DataSnapshot> devices = dataSnapshot.getChildren();
                    for (DataSnapshot device : devices){
                        Long updateTime = (Long) device.getValue();
                        if (System.currentTimeMillis()-updateTime < 60000){
                            mref.child("nearby_devices").child(device.getKey()).setValue(null);
                        }
                    }
                    mref.child("nearby_devices").addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            mref.child("current_nearby_devices_count").setValue(dataSnapshot.getChildrenCount());
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {

                        }
                    });
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });

        }
    }

    private ScanCallback mScanCallback = new ScanCallback() {

        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            Log.i("callbackType", String.valueOf(callbackType));
            Log.i("result", result.toString());
            BluetoothDevice btDevice = result.getDevice();
            Log.v("Found Device",btDevice.getAddress());
            ArrayList<String> uids = new ArrayList<>();
            uids.add(btDevice.getAddress());
            registerDevices(uids);
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            ArrayList<String> uids = new ArrayList<>();
            for (ScanResult sr : results) {
                BluetoothDevice btDevice = sr.getDevice();
                Log.v("Found Device",btDevice.getAddress());
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
