package com.krondelivery.android.app;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Environment;
import android.os.Handler;
import android.os.ParcelFileDescriptor;
import android.text.format.Formatter;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.krondelivery.android.app.sdk.ReceiveTask;
import com.krondelivery.android.app.sdk.SendTask;
import com.krondelivery.android.app.sdk.Task;


import java.io.File;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CODE = 200;
    SendTask.Mode scnario01 = SendTask.Mode.SCENARIO01;
    SendTask.Mode scnario02 = SendTask.Mode.SCENARIO02;
    SendTask.Mode scnario03 = SendTask.Mode.SCENARIO03;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final String[] contry = {"- Select -", "Scenario 1", "Scenario 2", "Scenario 3"};

        Spinner spinner = findViewById(R.id.buttonCombobox);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, R.layout.spinner_item, contry);
        spinner.setAdapter(adapter);

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Toast.makeText(getApplicationContext(), "Select Scenario "+position, Toast.LENGTH_SHORT).show();
                if(position == 0) {
                    return;
                }
                else if(position == 1) {
                    Intent intent = new Intent(MainActivity.this, HybridActivity.class);
                    intent.putExtra("uploadMode", scnario01);
                    startActivityForResult(intent, REQUEST_CODE);
                }
                else if(position == 2){
                    Intent intent = new Intent(MainActivity.this, HybridActivity.class);
                    intent.putExtra("uploadMode", scnario02);
                    startActivityForResult(intent, REQUEST_CODE);
                }
                else if(position == 3) {
                    Intent intent = new Intent(MainActivity.this, HybridActivity.class);
                    intent.putExtra("uploadMode", scnario03);
                    startActivityForResult(intent, REQUEST_CODE);
                }
                else {
                    return;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (PackageManager.PERMISSION_GRANTED != ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
            }
        }

        Task.init("503d6430f3c124e0f239092e9c916b932a869dfe");
    }


    class Listener implements ReceiveTask.OnTaskListener {
        @Override
        public void onNotify(int state, int detailedState, Object obj) {
        }
    }

}
