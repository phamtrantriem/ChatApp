package com.example.chatapp.Permission;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.chatapp.Service.Constaints;

public class Permissions {
    public boolean isEnableRecording(Context context) {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
    }
    public void requestRecordingPermission(Activity activity) {
        ActivityCompat.requestPermissions(activity, new String[] {Manifest.permission.RECORD_AUDIO}, Constaints.RECORDING_REQUEST_CODE);
    }
}
