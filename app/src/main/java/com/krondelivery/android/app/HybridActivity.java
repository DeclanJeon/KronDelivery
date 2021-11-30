package com.krondelivery.android.app;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.nfc.Tag;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.text.format.Formatter;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.krondelivery.android.app.sdk.ReceiveTask;
import com.krondelivery.android.app.sdk.SendTask;
import com.krondelivery.android.app.sdk.Task;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class HybridActivity extends AppCompatActivity {

    private static final int REQUEST_FILE = 200;

    Button sendButton, recvButton;
    TextView fileSizeView,
            progress_bar_text,
            fileName,
            downloadSpeed,
            remainingDownload,
            uploadSpeed,
            remainingUpload,
            up_fileSizeView,
            up_fileName,
            up_progressBarText;

    ProgressBar progressBar, up_progressBar;
    Handler handler = new Handler();

    private ListView logView;

    SendTask.Mode uploadMode;

    long startTime;
    long endTime;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hybrid);

        logView = ((ListView)findViewById(R.id.log));
        logView.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, new ArrayList<String>()));
        logView.setTranscriptMode(ListView.TRANSCRIPT_MODE_ALWAYS_SCROLL);

        sendButton = (Button) findViewById(R.id.send);
        sendButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                send();
            }
        });

        recvButton = (Button) findViewById(R.id.receive);
        recvButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                receive();
            }
        });

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (PackageManager.PERMISSION_GRANTED != ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
            }
        }
    }

    private void send() {

        if(PackageManager.PERMISSION_GRANTED != ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
            Toast.makeText(this, "External storage permission required", Toast.LENGTH_SHORT).show();
            return;
        }

        sendButton.setEnabled(false);

        Toast.makeText(HybridActivity.this, "File Picker Called", Toast.LENGTH_SHORT).show();

        Intent openFile = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        openFile.addCategory(Intent.CATEGORY_OPENABLE);
        openFile.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        openFile.setType("*/*");
        openFile = Intent.createChooser(openFile, "Choose a file");
        startActivityForResult(openFile, REQUEST_FILE);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        Uri returnUri = null;
        Uri[] returnUriArray = null;
        ArrayList<Uri> uriList = new ArrayList<>();

        if(resultCode != RESULT_OK) {
            return;
        }
        else if(requestCode == REQUEST_FILE) {
            if(data.getClipData() != null) {
                for(int i = 0; i < data.getClipData().getItemCount(); i++) {
                    returnUri = data.getClipData().getItemAt(i).getUri();
                    uriList.add(returnUri);
                }
                returnUriArray = uriList.toArray(new Uri[uriList.size()-1]);

                sendResult(returnUriArray);
            }
            else {
                returnUri = data.getData();
                uriList.add(returnUri);
                returnUriArray = uriList.toArray(new Uri[0]);
                sendResult(returnUriArray);
            }
        }
    }

    private void sendResult(Uri[] filePaths) {

        List list = new ArrayList();

        for(int i = 0; i < filePaths.length; i++) {
            SimpleFileInfo fileInfo = new SimpleFileInfo(this, filePaths[i]);
            list.add(fileInfo);
        }

        if (list.size() < 0 || list.size() == -1 || filePaths.length == 0) {
            print("ERROR: There is no list inside the list...");
            return;
        }

        Intent intent = getIntent();
        uploadMode = (SendTask.Mode) intent.getSerializableExtra("uploadMode");
        SendTask sendTask = new SendTask(this, list, uploadMode);

        sendTask.setOnTaskListener(new SendTask.OnTaskListener() {
            @Override
            public void onNotify(int state, int detailedState, Object obj) {

                Log.d("State", "state:::"+state);

                if(state == SendTask.State.PREPARING) {
                    if(detailedState == SendTask.DetailedState.PREPARING_UPDATED_KEY) {
                        String key = (String)obj;
                        if(key != null) {
                            ((TextView)findViewById(R.id.key)).setText(key);
                            print(String.format("Received key: %s", key));
                            print(String.format("Link URL: %s", sendTask.getValue(Task.Value.LINK_URL)));
                            print(String.format("Expires at: %s", new Timestamp((long)sendTask.getValue(Task.Value.EXPIRES_TIME) * 1000)));
                        }
                    }
                } else if(state == SendTask.State.TRANSFERRING) {

                    Task.FileState fileState = (Task.FileState)obj;

                    findViewResource();

                    String fileTotalSize = String.valueOf(fileState.getTotalSize());

                    Long lFileTotalSize = Long.parseLong(fileTotalSize);
                    String fileSize = Formatter.formatFileSize(HybridActivity.this, lFileTotalSize);
                    Long fileTransferSize = fileState.getTransferSize();
                    String fileTransferSizeFormat = Formatter.formatFileSize(HybridActivity.this, fileTransferSize);

                    //남은용량 표시
                    remainingUpload.setText(fileTransferSizeFormat+" / "+fileSize);

                    //백분율 계산
                    long transferPercentNum = fileTransferSize * 100 / lFileTotalSize;

                    int iTransferPercentNum = (int) transferPercentNum;

                    up_progressBar.setMax(100);
                    int progressMax = up_progressBar.getMax();

                    up_fileSizeView.setText(fileSize);
                    up_fileName.setText(fileState.getPathName());

                    if(fileState != null) {

                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                if(iTransferPercentNum <= progressMax) {
                                    up_progressBar.setProgress(iTransferPercentNum);
                                    handler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            up_progressBarText.setText("Uploading... "+String.valueOf(iTransferPercentNum)+"%");
                                        }
                                    });
                                }
                            }
                        }).start();


                        if(fileTransferSize == 0) {
                            startTime = System.currentTimeMillis();
                        }
                        Log.d("StartTime", "startTime : "+startTime);

                        endTime = System.currentTimeMillis();
                        Log.d("EndTime", "endTime : "+endTime);

                        double fileSizeCal = fileTransferSize;
                        double timeCal = endTime - startTime;
                        double timeCalDivision = timeCal / 1000;

                        double rate = (fileSizeCal / timeCalDivision) * 8;
                        double rateRound = Math.round( rate );
                        int iRate = (int) rateRound;
                        int mbpsRate = iRate / 1024 / 1024;
                        int kbpsRate = iRate / 1024;
                        String ratevalue = "";
                        if(iRate > 1024 * 1024){
                            ratevalue = String.valueOf(mbpsRate).concat(" Mbps");
                            uploadSpeed.setText(ratevalue);
                        }
                        else{
                            ratevalue = String.valueOf(kbpsRate).concat(" Kbps");
                            uploadSpeed.setText(ratevalue);
                        }

                         print(String.format("%s: %s/%s",
                         fileState.getFile().getLastPathSegment(),
                         fileState.getTransferSize(), fileState.getTotalSize()));

                    }
                } else if(state == SendTask.State.FINISHED) {

                    switch(detailedState) {
                        case SendTask.DetailedState.FINISHED_SUCCESS:
                            print("Transfer finished (success)");
                            Toast.makeText(HybridActivity.this, "Transfer finished (success)", Toast.LENGTH_SHORT).show();
                            break;
                        case SendTask.DetailedState.FINISHED_CANCEL:
                            print("Transfer finished (canceled)");
                            break;
                        case SendTask.DetailedState.FINISHED_ERROR:
                            print("Transfer finished (error!)");
                            break;
                    }

                    sendButton.setEnabled(true);

                } else if(state == SendTask.State.ERROR) {
                    switch(detailedState) {
                        case SendTask.DetailedState.ERROR_SERVER:
                            print("Network or Server Error!");
                            break;
                        case SendTask.DetailedState.ERROR_NO_REQUEST:
                            print("Timeout for waiting recipient");
                            break;
                    }

                    sendButton.setEnabled(true);

                }
            }
        });

        sendTask.start();
        //sendTask.await();
    }


    private void receive() {

        if(PackageManager.PERMISSION_GRANTED != ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            Toast.makeText(this, "External storage permission required", Toast.LENGTH_SHORT).show();
            return;
        }

        recvButton.setEnabled(false);

        String key = ((TextView)findViewById(R.id.key)).getText().toString();
        File destDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        ReceiveTask recvTask = new ReceiveTask(this, key, destDir);

        recvTask.setOnTaskListener(new ReceiveTask.OnTaskListener() {

            @Override
            public void onNotify(int state, int detailedState, Object obj) {

                if (state == ReceiveTask.State.PREPARING) {
                    if (detailedState == ReceiveTask.DetailedState.PREPARING_UPDATED_FILE_LIST) {
                        Task.FileState[] fileStateList = (Task.FileState[])obj;
                        for(Task.FileState file : fileStateList) {
                            /*print(String.format("%s: %d bytes",
                                    file.getPathName(), file.getTotalSize()));*/
                            Toast.makeText(HybridActivity.this, "Path Preparing : "+file.getPathName().toString(), Toast.LENGTH_SHORT).show();
                        }
                    }
                } else if (state == ReceiveTask.State.TRANSFERRING) {
                    Task.FileState fileState = (Task.FileState) obj;

                    findViewResource();

                    String fileTotalSize = String.valueOf(fileState.getTotalSize());

                    Long lFileTotalSize = Long.parseLong(fileTotalSize);
                    String fileSize = Formatter.formatFileSize(HybridActivity.this, lFileTotalSize);
                    Long fileTransferSize = fileState.getTransferSize();
                    String fileTransferSizeFormat = Formatter.formatFileSize(HybridActivity.this, fileTransferSize);

                    //남은용량 표시
                    remainingDownload.setText(fileTransferSizeFormat+" / "+fileSize);

                    //백분율 계산
                    long transferPercentNum = fileTransferSize * 100 / lFileTotalSize;

                    int iTransferPercentNum = (int) transferPercentNum;

                    progressBar.setMax(100);
                    int progressMax = progressBar.getMax();

                    fileSizeView.setText(fileSize);
                    fileName.setText(fileState.getPathName());

                    if (fileState != null) {

                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                if(iTransferPercentNum <= progressMax) {
                                    progressBar.setProgress(iTransferPercentNum);
                                    handler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            progress_bar_text.setText("Downloading... "+String.valueOf(iTransferPercentNum)+"%");
                                        }
                                    });
                                }
                            }
                        }).start();

                        if(fileTransferSize == 0) {
                            startTime = 0;
                            startTime = System.currentTimeMillis();
                        }
                        Log.d("StartTime", "startTime : " + startTime);

                        endTime = System.currentTimeMillis();
                        Log.d("EndTime", "endTime : "+endTime);

                        double fileSizeCal = fileTransferSize;
                        double timeCal = endTime - startTime;
                        double timeCalDivision = timeCal / 1000;

                        double rate = (fileSizeCal / timeCalDivision) * 8;
                        double rateRound = Math.round( rate );
                        int iRate = (int) rateRound;
                        int mbpsRate = iRate / 1024 / 1024;
                        int kbpsRate = iRate / 1024;
                        String ratevalue = "";
                        if(iRate > 1024 * 1024){
                            ratevalue = String.valueOf(mbpsRate).concat(" Mbps");
                            downloadSpeed.setText(ratevalue);
                        }
                        else{
                            ratevalue = String.valueOf(kbpsRate).concat(" Kbps");
                            downloadSpeed.setText(ratevalue);
                        }

                        print(String.format("%s => %s/%s",
                                fileState.getFile().getLastPathSegment(),
                                fileState.getTransferSize(), fileState.getTotalSize()));

                    }
                } else if (state == ReceiveTask.State.FINISHED) {
                    switch (detailedState) {
                        case ReceiveTask.DetailedState.FINISHED_SUCCESS:
                            print("Transfer finished (success)");
                            Toast.makeText(HybridActivity.this, "Transfer finished (success)", Toast.LENGTH_SHORT).show();
                            break;
                        case ReceiveTask.DetailedState.FINISHED_CANCEL:
                            print("Transfer finished (canceled)");
                            break;
                        case ReceiveTask.DetailedState.FINISHED_ERROR:
                            print("Transfer finished (error!)");
                            break;
                    }

                    recvButton.setEnabled(true);
                } else if (state == ReceiveTask.State.ERROR) {
                    switch (detailedState) {
                        case ReceiveTask.DetailedState.ERROR_SERVER:
                            print("Network or Server Error!");
                            break;
                        case ReceiveTask.DetailedState.ERROR_NO_EXIST_KEY:
                            print("Invalid Key!");
                            break;
                        case ReceiveTask.DetailedState.ERROR_FILE_NO_DOWNLOAD_PATH:
                            print("Invalid download path");
                            break;
                    }

                    recvButton.setEnabled(true);
                }

            }
        });
        recvTask.start();

    }


    private void findViewResource() {

        fileSizeView = (TextView)findViewById(R.id.filesize);
        progressBar = findViewById(R.id.progress_bar);
        progress_bar_text = findViewById(R.id.progress_bar_text);
        fileName = findViewById(R.id.file_name);
        downloadSpeed = findViewById(R.id.download_speed);
        remainingDownload = findViewById(R.id.remaining_download);

        up_fileSizeView = findViewById(R.id.up_filesize);
        up_fileName = findViewById(R.id.up_file_name);
        up_progressBar = findViewById(R.id.up_progress_bar);
        up_progressBarText = findViewById(R.id.up_progress_bar_text);
        uploadSpeed = findViewById(R.id.upload_speed);
        remainingUpload = findViewById(R.id.remaining_upload);
    }

    private void print(final String text) {
        ArrayAdapter<String> adapter = (ArrayAdapter<String>)logView.getAdapter();
        adapter.add(text);
        adapter.notifyDataSetChanged();
    }

}
