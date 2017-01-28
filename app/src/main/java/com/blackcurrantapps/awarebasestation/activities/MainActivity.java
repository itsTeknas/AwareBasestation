package com.blackcurrantapps.awarebasestation.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
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


}
