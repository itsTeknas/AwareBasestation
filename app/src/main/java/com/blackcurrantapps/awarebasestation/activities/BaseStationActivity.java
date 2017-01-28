package com.blackcurrantapps.awarebasestation.activities;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.blackcurrantapps.awarebasestation.R;

import butterknife.BindView;
import butterknife.ButterKnife;

public class BaseStationActivity extends AppCompatActivity {

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

    private String rideName = "thunder";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_base_station);
        ButterKnife.bind(this);

        rideName = getIntent().getStringExtra("RIDE_NAME");
    }
}
