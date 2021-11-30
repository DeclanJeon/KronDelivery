package com.krondelivery.android.app;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import androidx.annotation.NonNull;

import com.krondelivery.android.app.sdk.SendTask;

import java.io.File;
import java.util.Arrays;
import java.util.List;

public class SimpleFileInfo implements SendTask.FileInfo {
    private Uri file;
    private Uri[] files;
    private String fileName;
    private long length;
    private long lastModified;

    public SimpleFileInfo(File file) {
        set(file, null);
    }

    public SimpleFileInfo(Context context, Uri uri) {
        set(context, uri, null);
    }

    public SimpleFileInfo(File file, String fileName) {
        set(file, fileName);
    }

    public SimpleFileInfo(Context context, Uri uri, String fileName) {
        set(context, uri, fileName);
    }

    public SimpleFileInfo(Context context, Uri[] uris) {
        set(context, uris, null);
    }

    synchronized void set(File file, String fileName) {
        if (file.exists()) {
            this.file = Uri.fromFile(file);
            this.fileName = fileName != null ? fileName : this.file.getLastPathSegment();
            this.length = file.length();
            this.lastModified = file.lastModified() / 1000;
        }
    }

    synchronized void set(Context context, Uri uri, String fileName) {
        if (uri.getScheme().equals("file")) {
            File file = new File(uri.getPath());
            set(file, fileName);
        } else if (uri.getScheme().equals("content")) {
            ContentResolver cr = context.getContentResolver();
            Cursor cs = cr.query(uri, null, null, null, null);
            if (cs != null) {
                if (cs.moveToFirst()) {
                    if (fileName != null) {
                        this.fileName = fileName;
                    } else {
                        int col = cs.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                        if (col >= 0) {
                            this.fileName = cs.getString(col);
                        }
                    }
                    if (this.fileName != null) {
                        int col = cs.getColumnIndex(OpenableColumns.SIZE);
                        if (col >= 0) {
                            this.length = cs.getLong(col);
                            this.file = uri;
                            this.lastModified = System.currentTimeMillis() / 1000L;
                        }
                    }
                }
                cs.close();
            }
        }
    }

    synchronized void set(Context context, Uri[] uris, String fileName) {

        int uriSize = uris.length;

        this.files = new Uri[uriSize+1];

        if (uris[uriSize].getScheme().equals("file")) {
            File file = new File(uris[uriSize].getPath());
            set(file, fileName);
        } else if (uris[uriSize].getScheme().equals("content")) {
            for(int i=0; i< uris.length; i++) {
                ContentResolver cr = context.getContentResolver();
                Cursor cs = cr.query(uris[i], null, null, null, null);
                if (cs != null) {
                    if (cs.moveToFirst()) {
                        if (fileName != null) {
                            this.fileName = fileName;
                        } else {
                            int col = cs.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                            if (col >= 0) {
                                this.fileName = cs.getString(col);
                            }
                        }
                        if (this.fileName != null) {
                            int col = cs.getColumnIndex(OpenableColumns.SIZE);
                            if (col >= 0) {
                                this.length = cs.getLong(col);
                                this.files[i] = uris[i];
                                this.lastModified = System.currentTimeMillis() / 1000L;
                            }
                        }
                    }
                    cs.close();
                }
            }
        }
    }


    public boolean isValid() {
        return file != null;
    }

    @NonNull
    @Override
    public Uri getUri() {
        return file;
    }

    @Override
    public Uri[] getUris() {
        return files;
    }

    @NonNull
    @Override
    public String getFileName() {
        return fileName;
    }

    @Override
    public long getLength() {
        return length;
    }

    @Override
    public long getLastModified() {
        return lastModified;
    }

}