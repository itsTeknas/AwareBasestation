package com.blackcurrantapps.awarebasestation.activities;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;

import com.blackcurrantapps.awarebasestation.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;


public class MainActivity extends AppCompatActivity {

    private static final int PERMISSIONS_REQUEST = 7777;

    private static final String[] permissions = {
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.WAKE_LOCK
    };

    @BindView(R.id.activity_main)
    RelativeLayout activityMain;
    @BindView(R.id.listView)
    ListView listView;
    private FirebaseDatabase mFirebaseDatabase;

    ArrayList<String> names = new ArrayList<>();
    ArrayList<String> keys = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        getSupportActionBar().setTitle("Select Base Station");

        if (!areAllPermissionsGiven()) {
            new AlertDialog.Builder(this).setTitle("Permissions")
                    .setMessage("Please accept all permissions for the app to work properly")
                    .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            requestPermissions();
                        }
                    }).create().show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        FirebaseAuth.getInstance().signInAnonymously().addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                mFirebaseDatabase = FirebaseDatabase.getInstance();

                mFirebaseDatabase.getReference().child("ride_names").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        Iterable<DataSnapshot> rides = dataSnapshot.getChildren();

                        for (DataSnapshot ride:rides){
                            names.add((String) ride.child("name").getValue());
                            keys.add((String) ride.child("key").getValue());
                        }

                        listView.setAdapter(new ArrayAdapter<>(MainActivity.this, android.R.layout.simple_list_item_1,names));
                        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                            @Override
                            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                                Intent intent = new Intent(MainActivity.this,BaseStationActivity.class);
                                intent.putExtra("RIDE_KEY",keys.get(i));
                                intent.putExtra("RIDE_NAME",names.get(i));
                                startActivity(intent);
                                finish();
                            }
                        });
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
            }
        });
    }

    private void requestPermissions() {
        ActivityCompat.requestPermissions(MainActivity.this, permissions, PERMISSIONS_REQUEST);
    }

    private boolean areAllPermissionsGiven() {
        boolean denied = false;
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(MainActivity.this, permission) != PackageManager.PERMISSION_GRANTED) {
                denied = true;
            }
        }
        return !denied;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case PERMISSIONS_REQUEST: {
                if (!areAllPermissionsGiven()) {
                    requestPermissions();
                }
            }
        }
    }

}
