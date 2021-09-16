package com.tokbox.sample.sharedwhiteboard;

import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.opentok.android.Connection;
import com.opentok.android.OpentokError;
import com.opentok.android.Session;
import com.opentok.android.Stream;
import com.skydoves.colorpickerview.ColorEnvelope;
import com.skydoves.colorpickerview.ColorPickerDialog;
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener;

public class MainActivity extends AppCompatActivity implements Session.SignalListener {

    private static final String TAG = MainActivity.class.getSimpleName();

    private static final int PERMISSIONS_REQUEST_CODE = 124;

    private Session session;
    private DrawingView drawingView;
    private AlertDialog alertDialog;


    private Session.SessionListener sessionListener = new Session.SessionListener() {
        @Override
        public void onConnected(Session session) {
            Log.d(TAG, "onConnected: Connected to session " + session.getSessionId());
            alertDialog.dismiss();

        }

        @Override
        public void onDisconnected(Session session) {
            Log.d(TAG, "onDisconnected: disconnected from session " + session.getSessionId());

            MainActivity.this.session = null;
        }

        @Override
        public void onError(Session session, OpentokError opentokError) {
            finishWithMessage("Session error: " + opentokError.getMessage());
        }

        @Override
        public void onStreamReceived(Session session, Stream stream) {
            Log.d(TAG, "onStreamReceived: New stream " + stream.getStreamId() + " in session " + session.getSessionId());
        }

        @Override
        public void onStreamDropped(Session session, Stream stream) {
            Log.d(TAG, "onStreamDropped: Stream " + stream.getStreamId() + " dropped from session " + session.getSessionId());
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if(!OpenTokConfig.isValid()) {
            finishWithMessage("Invalid OpenTokConfig. " + OpenTokConfig.getDescription());
            return;
        }

       setProgressDialog();

        drawingView = findViewById(R.id.whiteboard);

        Button colorPickerBtn = (Button)findViewById(R.id.button);

        colorPickerBtn.setOnClickListener(view -> {

            new ColorPickerDialog.Builder(this)
                    .setTitle("ColorPicker Dialog")
                    .setPreferenceName("MyColorPickerDialog")
                    .setPositiveButton("Ok",
                            new ColorEnvelopeListener() {
                                @Override
                                public void onColorSelected(ColorEnvelope envelope, boolean fromUser) {
                                    //setLayoutColor(envelope);
                                    Log.d("MainActivity", "onColorSelected: " + envelope.getColor());
                                    drawingView.setColor(envelope.getColor());
                                }
                            })
                    .setNegativeButton("cancel",
                            new DialogInterface.OnClickListener(){
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    dialogInterface.dismiss();
                                }
                            })
                    .attachAlphaSlideBar(true) // the default value is true.
                    .attachBrightnessSlideBar(true)  // the default value is true.
                    .setBottomSpace(12).show();// set a bottom space between the last slidebar and buttons.

        });

        initializeSession();

    }

    private void setProgressDialog() {

            int llPadding = 30;
            LinearLayout ll = new LinearLayout(this);
            ll.setOrientation(LinearLayout.HORIZONTAL);
            ll.setPadding(llPadding, llPadding, llPadding, llPadding);
            ll.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams llParam = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            llParam.gravity = Gravity.CENTER;
            ll.setLayoutParams(llParam);

            ProgressBar progressBar = new ProgressBar(this);
            progressBar.setIndeterminate(true);
            progressBar.setPadding(0, 0, llPadding, 0);
            progressBar.setLayoutParams(llParam);

            llParam = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            llParam.gravity = Gravity.CENTER;
            TextView tvText = new TextView(this);
            tvText.setText("Initializing session ...");
            tvText.setTextColor(Color.parseColor("#000000"));
            tvText.setTextSize(20);
            tvText.setLayoutParams(llParam);

            ll.addView(progressBar);
            ll.addView(tvText);

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setCancelable(true);
            builder.setView(ll);

            alertDialog = builder.create();
            alertDialog.show();

            Window window = alertDialog.getWindow();
            if (window != null) {
                WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
                layoutParams.copyFrom(alertDialog.getWindow().getAttributes());
                layoutParams.width = LinearLayout.LayoutParams.WRAP_CONTENT;
                layoutParams.height = LinearLayout.LayoutParams.WRAP_CONTENT;
                alertDialog.getWindow().setAttributes(layoutParams);
            }
    }

    private void initializeSession() {

        session = new Session.Builder(this, OpenTokConfig.API_KEY, OpenTokConfig.SESSION_ID).sessionOptions(new Session.SessionOptions() {
            @Override
            public boolean useTextureViews() {
                return true;
            }
        }).build();
        session.setSessionListener(sessionListener);
        session.setSignalListener(this);
        session.connect(OpenTokConfig.TOKEN);
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (session == null) {
            return;
        }

        session.onPause();

        if (isFinishing()) {
            disconnectSession();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (session == null) {
            return;
        }

        session.onResume();
    }

    @Override
    protected void onDestroy() {
        disconnectSession();

        super.onDestroy();
    }

    private void disconnectSession() {
        if (session == null) {
            return;
        }

        session.disconnect();
    }

    private void finishWithMessage(String message) {
        Log.e(TAG, message);
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        this.finish();
    }

    @Override
    public void onSignalReceived(Session session, String s, String s1, Connection connection) {

        String myConnectionId = session.getConnection().getConnectionId();
        if (connection != null && !(connection.getConnectionId().equals(myConnectionId))) {
            // Signal received from another client
            Log.d("MainActivity", "onSignalReceived: " + s + s1);

            String[] xy = s1.split("_");

            Float x = Float.parseFloat(xy[0]);
            Float y = Float.parseFloat(xy[1]);
            int color = Integer.parseInt(xy[2]);

            if(s.equals("start")) {
                drawingView.touch_start("remote", x, y, color);
                drawingView.invalidate();
            }
            else if(s.equals("move")) {
                drawingView.touch_move("remote", x, y, color);
                drawingView.invalidate();
            } else {
                drawingView.touch_up("remote");
                drawingView.invalidate();
            }

        }

    }

    public void sendSignal(String type, float x, float y, int color) {
        session.sendSignal(type, Float.toString(x) + "_" + Float.toString(y) + "_"+ color);
    }
}
