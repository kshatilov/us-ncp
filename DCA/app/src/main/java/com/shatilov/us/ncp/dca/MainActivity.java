package com.shatilov.us.ncp.dca;

import android.content.Context;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;

import androidx.appcompat.app.AppCompatActivity;

import com.ncorti.myonnaise.Myo;
import com.ncorti.myonnaise.MyoStatus;
import com.ncorti.myonnaise.Myonnaise;
import com.shatilov.us.ncp.dca.utils.Utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.observers.DisposableSingleObserver;
import io.reactivex.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity {

    private static String TAG = "US_NCP_DCA";
    private static int MYO_POLLING_FREQUENCY = 200;

    Myo myo = null;
    List<Pair<float[], Integer>> records = new ArrayList<>();
    int sliderValue = 0;
    boolean hasStarted = false;

    Button connectButton = null;
    Button startButton = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initUI();
    }

    private void write() {
        Log.d(TAG, "WRITING " + records.size() + " values");
        String filename = "dca_emg_slider_values_" + System.currentTimeMillis() + ".dat";
        String dirName = "DCA_EMG";

        String path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + dirName;

        File dir = new File(path);
        if(!dir.exists()){
            dir.mkdir();
        }
        try {
            File file = new File(dir, filename);
            FileWriter writer = new FileWriter(file);
            for (Pair<float[], Integer> record : records) {
                String output = Arrays.toString(record.first) + ":" + record.second + "\n";
                writer.append(output);
            }
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void initUI() {
        connectButton = findViewById(R.id.connect_button);
        connectButton.setOnClickListener(button -> getMYO());
        SeekBar slider = findViewById(R.id.slider);
        slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                sliderValue = i;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        startButton = findViewById(R.id.start_button);
        Button stopButton = findViewById(R.id.stop_button);
        startButton.setOnClickListener(i -> {
            hasStarted = true;
            stopButton.setEnabled(true);
            startButton.setEnabled(false);
            records = new ArrayList<>();
        });
        stopButton.setOnClickListener(i -> {
            hasStarted = false;
            startButton.setEnabled(true);
            stopButton.setEnabled(false);
            write();
        });
        startButton.setEnabled(false);
        stopButton.setEnabled(false);
    }

    private void record(float[] emgValue) {
        if (hasStarted) {
            records.add(new Pair<>(emgValue, sliderValue));
        }
    }

    private void getMYO() {
        Log.d(TAG, "getMYO: start");
        Myonnaise myonnaise = new Myonnaise(this);
        String LEFT_MYO_ADDRESS = "CB:29:93:00:70:09";
        myonnaise.getMyo(LEFT_MYO_ADDRESS).subscribeWith(new DisposableSingleObserver<Myo>() {
            @Override
            public void onSuccess(Myo _myo) {
                Log.d(TAG, "getMYO: success");
                myo = _myo;
                myo.connect(getApplicationContext());

                myo.statusObservable()
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(it -> {
                            if (it == MyoStatus.READY) {
                                Handler handler = new Handler();
                                handler.postDelayed(() -> {
                                    myo.sendCommand(Utils.getStreamCmd());
                                    myo.setFrequency(MYO_POLLING_FREQUENCY);
                                }, 2000);
                                connectButton.setEnabled(false);
                                startButton.setEnabled(true);
                            }
                        });
                myo.dataFlowable()
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .onBackpressureDrop()
                        .subscribe(value -> record(value));

            }

            @Override
            public void onError(Throwable e) {
                Log.d(TAG, "onError: Failed to connect to myo");
            }
        });
    }
}
