package com.alexbatalov.fallout2ce;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.documentfile.provider.DocumentFile;

import java.io.File;

public class ImportActivity extends Activity {
    private static final int IMPORT_REQUEST_CODE = 1;
    private static final int ACCESS_REQUEST_CODE = 2;

    /** Waiting for the player to allow access to the PortHub folder. */
    private boolean askedForAccess = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (PortHubFolder.hasGame(PortHubFolder.dir())) {
            Toast.makeText(this, R.string.porthub_access_message, Toast.LENGTH_LONG).show();
            askedForAccess = true;
            PortHubFolder.requestAccess(this, ACCESS_REQUEST_CODE);
            return;
        }
        openPicker();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (askedForAccess && PortHubFolder.canUse(this)) {
            askedForAccess = false;
            startMainActivity();
            finish();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == ACCESS_REQUEST_CODE && !PortHubFolder.canUse(this)) {
            askedForAccess = false;
            openPicker();
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        // Back from the settings screen without allowing access: offer the original folder picker.
        if (hasFocus && askedForAccess && !PortHubFolder.canUse(this)
                && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            askedForAccess = false;
            openPicker();
        }
    }

    private void openPicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        startActivityForResult(intent, IMPORT_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        if (requestCode == IMPORT_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                final Uri treeUri = resultData.getData();
                if (treeUri != null) {
                    final DocumentFile treeDocument = DocumentFile.fromTreeUri(this, treeUri);
                    if (treeDocument != null) {
                        copyFiles(treeDocument);
                        return;
                    }
                }
            }

            finish();
        } else {
            super.onActivityResult(requestCode, resultCode, resultData);
        }
    }

    private void copyFiles(DocumentFile treeDocument) {
        ProgressDialog dialog = createProgressDialog();
        dialog.show();

        new Thread(() -> {
            ContentResolver contentResolver = getContentResolver();
            File externalFilesDir = getExternalFilesDir(null);
            FileUtils.copyRecursively(contentResolver, treeDocument, externalFilesDir);

            startMainActivity();
            dialog.dismiss();
            finish();
        }).start();
    }

    private void startMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }

    private ProgressDialog createProgressDialog() {
        ProgressDialog progressDialog = new ProgressDialog(this,
            android.R.style.Theme_Material_Light_Dialog);
        progressDialog.setTitle(R.string.loading_dialog_title);
        progressDialog.setMessage(getString(R.string.loading_dialog_message));
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.setCancelable(false);

        return progressDialog;
    }
}
