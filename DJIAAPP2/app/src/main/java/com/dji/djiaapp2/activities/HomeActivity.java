package com.dji.djiaapp2.activities;

import static com.dji.djiaapp2.utils.AppConfiguration.CONTROLLER_IP_ADDRESS;
import static com.dji.djiaapp2.utils.AppConfiguration.RTMP_URL;
import static com.dji.djiaapp2.utils.AppConfiguration.RTSP_URL;
import static com.dji.djiaapp2.utils.AppConfiguration.MAX_SPEED;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.transition.Fade;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.dji.djiaapp2.R;
import com.dji.djiaapp2.utils.AppConfiguration;
import com.dji.djiaapp2.viewmodels.HomeViewModel;

/**
 * For uploading and loading waypoint xml file to drone
 * Parses waypoint xml file and loads into drone
 * Modify settings such as server addresses and amx speed of drone
 */
public class HomeActivity extends AppCompatActivity {

    private static final int IMPORT_FILE_CODE = 1234;

    private ImageView uploadBtn;
    private TextView filename;
    private Button startBtn;
    private Button settingsBtn;
    private ProgressDialog loadingBar;

    private HomeViewModel homeViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN);
        super.onCreate(savedInstanceState);
        homeViewModel = new ViewModelProvider(this).get(HomeViewModel.class);
        homeViewModel.init(getApplicationContext());
        supportRequestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getWindow().requestFeature(Window.FEATURE_ACTIVITY_TRANSITIONS);
            getWindow().setEnterTransition(new Fade());
        }

        setContentView(R.layout.activity_home);
        initUI();
        subscribeToViewModel();
    }

    private void initUI() {
        uploadBtn = findViewById(R.id.uploadBtn);
        uploadBtn.setOnClickListener(view -> openFile());
        filename = findViewById(R.id.waypointFilename);

        startBtn = findViewById(R.id.startBtn);
        startBtn.setOnClickListener(view -> startVideoActivity());

        settingsBtn = findViewById(R.id.settingsBtn);
        initSettings();

        loadingBar = new ProgressDialog(HomeActivity.this);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
    }

    // For file browsing on android phone
    private void openFile() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/xml");
        startActivityForResult(intent, IMPORT_FILE_CODE);
    }

    // Loading animation when uploading waypoint file
    private void initUploadingBar() {
        loadingBar.setTitle("Uploading Mission");
        loadingBar.setMessage("Please wait uploading waypoint mission...");
        loadingBar.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        loadingBar.setIndeterminate(true);
        loadingBar.show();
    }

    private void startVideoActivity() {
        Intent intent = new Intent(this, VideoActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        intent.putExtra("hasUploaded", homeViewModel.hasUploaded.getValue());
        startActivity(intent);
    }

    private void initSettings() {
        settingsBtn.setOnClickListener(view -> {
            final View settings = LayoutInflater.from(HomeActivity.this).inflate(R.layout.settings, null);
            AlertDialog.Builder alert = new AlertDialog.Builder(HomeActivity.this, R.style.AlertDialogCustom);
            alert.setCancelable(false);
            settings.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View view, MotionEvent motionEvent) {
                    InputMethodManager imm = (InputMethodManager) getBaseContext().getSystemService(Context
                            .INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(settings.getWindowToken(), 0);
                    return true;
                }
            });
            final EditText controllerIP = settings.findViewById(R.id.controllerIP);
            final EditText RTSPAddr = settings.findViewById(R.id.RTSPUrl);
            final EditText RTMPAddr = settings.findViewById(R.id.RTMPUrl);
            final EditText maxSpd = settings.findViewById(R.id.maxSpeed);

            controllerIP.setText(CONTROLLER_IP_ADDRESS);
            RTSPAddr.setText(RTSP_URL);
            RTMPAddr.setText(RTMP_URL);
            maxSpd.setText(Integer.toString(MAX_SPEED));

            alert.setTitle("Settings");
            alert.setView(settings);

            alert.setPositiveButton("OK", (dialog, whichButton) -> {
                AppConfiguration.setControllerIpAddress(controllerIP.getText().toString());
                AppConfiguration.setRtspUrl(RTSPAddr.getText().toString());
                AppConfiguration.setRtmpUrl(RTMPAddr.getText().toString());
                try{
                    AppConfiguration.setMaxSpeed(Integer.parseInt(maxSpd.getText().toString().trim()));
                } catch (NumberFormatException error) {
                    // ignore input
                }
            });

            alert.setNegativeButton("Cancel", (dialog, whichButton) -> {
                // do nothing, no update values
            });
            alert.show();
        });
    }

    private void subscribeToViewModel() {
        homeViewModel.selectedFile.observe(this, s -> {
            if (s != null) {
                filename.setText(s);
            } else {
                filename.setText("UPLOAD WAYPOINT FILE");
            }
        });

        homeViewModel.hasUploaded.observe(this, isTrue -> {
            loadingBar.dismiss();
        });
    }

    // For returning from file browser page after selecting waypoint file
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            // When file has been selected
            case IMPORT_FILE_CODE:
                if (resultCode == Activity.RESULT_OK ) {
                    if(data != null)  {
                        homeViewModel.uploadWaypointFile(data.getData(), getApplicationContext());
                        initUploadingBar();
                    }
                }
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        homeViewModel.cleanUp();
    }

    // Close application
    @Override
    public void onBackPressed() {
        this.finishAffinity();
    }

}