package sjnov11_equalizing.equalizer;

import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.AudioFormat;
import android.media.MediaRecorder;
import android.media.audiofx.Equalizer;
import android.os.Build;
import android.os.Handler;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.json.*;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {

    static final String RECORDED_FILE = "/sdcard/recorded.mp4";
    static final String DEVICE_DATA = "/sdcard/device_data.json";

    MediaRecorder recorder = null;          // To get max amplitude
    AudioTrack audioTrack = null;           // To play particular frequency audio

    private final int MEASURING = 7;
    private final int NOISE = 3;

    // Frequency Tone Generator variable
    private final int duration = 30; // seconds (not correct, so give enough time)
    private final int sampleRate = 48000;
    private final int numSamples = duration * sampleRate;
    private final double sample[] = new double[numSamples];
    private final byte generatedSnd[] = new byte[2 * numSamples];

    private Timer _timer;
    private TimerTask _timerTask;
    private Runnable _startRecord;
    private Runnable _stopRecord;
    private Handler _handler;

    private Equalizer _equalizer;

    private final ArrayList<Integer> amplitudes = new ArrayList<Integer>();

    private final int []averages = {0, 0, 0, 0};
    private int sum_amplitude = 0;
    private int freqOfTone;
    private int cnt = 0;

    //Handler handler = new Handler();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        _equalizer = new Equalizer(0, 0);


        /*
        // Measured Amplitude
        TextView tv60hzAmp = (TextView)findViewById(R.id.tv60hzAmp);
        TextView tv230hzAmp = (TextView)findViewById(R.id.tv230hzAmp);
        TextView tv910hzAmp = (TextView)findViewById(R.id.tv910hzAmp);
        TextView tv3600hzAmp = (TextView)findViewById(R.id.tv3600hzAmp);

        // Average Amplitude
        TextView tv60hzAverage = (TextView)findViewById(R.id.tv60hzAverage);
        TextView tv230hzAverage = (TextView)findViewById(R.id.tv230hzAverage);
        TextView tv910hzAverage = (TextView)findViewById(R.id.tv910hzAverage);
        TextView tv3600hzAverage = (TextView)findViewById(R.id.tv3600hzAverage);

        // Buttons
        Button btn60hz = (Button)findViewById(R.id.btn60hz);
        Button btn230hz = (Button)findViewById(R.id.btn230hz);
        Button btn910hz = (Button)findViewById(R.id.btn910hz);
        Button btn3600hz = (Button)findViewById(R.id.btn3600hz);
        */

    }


    protected void onDestroy() {
        // killMediaPlayer();
        killMediaRecorder();
        super.onDestroy();
    }

    protected void onPause() {
        killMediaRecorder();
        // killMediaPlayer();
        super.onPause();
    }

    private void killMediaRecorder() {
        if(recorder != null) {
            try {
                recorder.release();
            } catch (Exception e){
                e.printStackTrace();
            }
        }
    }


    public void onClick(View v) {
        cnt = 0;
        switch (v.getId()){
            case R.id.btn60hz:
                record(60);
                break;
            case R.id.btn230hz:
                record(230);
                break;
            case R.id.btn910hz:
                record(910);
                break;
            case R.id.btn3600hz:
                record(3600);
                break;

            case R.id.btnSave:
                saveDeviceData();
                break;
        }

    }

    public void saveDeviceData() {
        EditText editText = (EditText)findViewById(R.id.etDevice);
        String device = editText.getText().toString();
        JSONObject obj = new JSONObject();
        JSONArray list = new JSONArray();
        try {
            obj.put("device", device);
            list.put(averages[0]);
            list.put(averages[1]);
            list.put(averages[2]);
            list.put(averages[3]);
            obj.put("decibel", list);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        Log.e("Result", obj.toString());
        try {
            FileWriter file = new FileWriter(DEVICE_DATA);
            file.write(obj.toString());
            file.flush();
            file.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void initBandLevel() {
        _equalizer.setBandLevel((short)0, (short)(0));
        _equalizer.setBandLevel((short)1, (short)(0));
        _equalizer.setBandLevel((short)2, (short)(0));
        _equalizer.setBandLevel((short)3, (short)(0));
        _equalizer.setBandLevel((short)4, (short)(0));
    }


    // Make TimerTask which evaluates max amplitudes for MEASURE seconds.
    public void setTimerTask (int freq) {

        _timerTask = new TimerTask() {
            @Override
            public void run() {
                MainActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        int current_amplitude = recorder.getMaxAmplitude();

                        switch (freqOfTone){
                            case 60:
                                ((TextView) findViewById(R.id.tv60hzAmp)).setText(String.valueOf(current_amplitude));
                                amplitudes.add(current_amplitude);
                                break;
                            case 230:
                                ((TextView) findViewById(R.id.tv230hzAmp)).setText(String.valueOf(current_amplitude));
                                amplitudes.add(current_amplitude);
                                break;
                            case 910:
                                ((TextView) findViewById(R.id.tv910hzAmp)).setText(String.valueOf(current_amplitude));
                                amplitudes.add(current_amplitude);
                                break;
                            case 3600:
                                ((TextView) findViewById(R.id.tv3600hzAmp)).setText(String.valueOf(current_amplitude));
                                amplitudes.add(current_amplitude);
                                break;
                        }

                    }
                });
            }
        };
    }

    // Record amplitude of frequency
    public void record(int freq) {
        if(recorder != null) {
            recorder.stop();
            recorder.release();
            recorder = null;
        }

        recorder = new MediaRecorder();
        String manufacturer = Build.MANUFACTURER;
        if (manufacturer.toLowerCase().contains("samsung")) {
            recorder.setAudioSource(MediaRecorder.AudioSource.VOICE_COMMUNICATION);
        } else {
            recorder.setAudioSource(MediaRecorder.AudioSource.VOICE_CALL);
        }
//        recorder.setAudioSource(MediaRecorder.AudioSource.VOICE_CALL);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);
        recorder.setOutputFile(RECORDED_FILE);

        freqOfTone = freq;


        try {
            recorder.prepare();
            recorder.start();

            // Record max amplitudes per second
            _startRecord = new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(),
                            "Start Recording", Toast.LENGTH_SHORT).show();

                    if(_timer != null) {
                        _timer.cancel();
                        _timer = null;
                    }
                    cnt ++;
                    setTimerTask(freqOfTone);
                    _timer = new Timer();
                    _timer.schedule(_timerTask, 1000, 1000);

                    initBandLevel();
                    switch (freqOfTone){
                        case 60:
                            if(cnt == 1)
                                _equalizer.setBandLevel((short) 0, (short) (-1500));
                            else if(cnt == 2)
                                _equalizer.setBandLevel((short) 0, (short) (0));
                            else if(cnt == 3)
                                _equalizer.setBandLevel((short) 0, (short) (1500));
                            playFreq(60);
                            break;
                        case 230:
                            if(cnt == 1)
                                _equalizer.setBandLevel((short) 1, (short) (-1500));
                            else if(cnt == 2)
                                _equalizer.setBandLevel((short) 1, (short) (0));
                            else if(cnt == 3)
                                _equalizer.setBandLevel((short) 1, (short) (1500));
                            playFreq(230);
                            break;
                        case 910:
                            if(cnt == 1)
                                _equalizer.setBandLevel((short) 2, (short) (-1500));
                            else if(cnt == 2)
                                _equalizer.setBandLevel((short) 2, (short) (0));
                            else if(cnt == 3)
                                _equalizer.setBandLevel((short) 2, (short) (1500));
                            playFreq(910);
                            break;
                        case 3600:
                            if(cnt == 1)
                                _equalizer.setBandLevel((short) 3, (short) (-1500));
                            else if(cnt == 2)
                                _equalizer.setBandLevel((short) 3, (short) (0));
                            else if(cnt == 3)
                                _equalizer.setBandLevel((short) 3, (short) (1500));
                            playFreq(3600);
                            break;
                    }

                }
            };

            _stopRecord = new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(),
                            "Stop Recording", Toast.LENGTH_SHORT).show();

                    if(_timer != null) {
                        _timer.cancel();
                        _timer = null;
                    }

                    stopFreq();
                    /*
                    if(recorder != null) {
                        recorder.stop();
                        recorder.release();
                        recorder = null;
                    }
                    */

                    for(int i = 0; i < amplitudes.size(); i ++){
                        Log.d("full arraylist", "value (" + i + ") :" + amplitudes.get(i));
                    }

                    // Average Amplitudes
                    // Except index 0, 1 noising part for accuracy
                    for(int i = NOISE; i < amplitudes.size(); i ++){
                        Log.d("arraylist", "result : " + amplitudes.get(i));
                        sum_amplitude += amplitudes.get(i);
                    }

                    int average;
                    switch (freqOfTone){
                        case 60:
                            average = sum_amplitude / (amplitudes.size() - NOISE);
                            averages[0] = average;
                            if(cnt == 1)
                                ((TextView) findViewById(R.id.tv60hzAverage1)).setText(String.valueOf(average));
                            else if(cnt == 2)
                                ((TextView) findViewById(R.id.tv60hzAverage2)).setText(String.valueOf(average));
                            else if(cnt == 3)
                                ((TextView) findViewById(R.id.tv60hzAverage3)).setText(String.valueOf(average));
                            sum_amplitude = 0;
                            amplitudes.clear();
                            break;
                        case 230:
                            average = sum_amplitude / (amplitudes.size() - NOISE);
                            averages[1] = average;
                            if(cnt == 1)
                                ((TextView) findViewById(R.id.tv230hzAverage1)).setText(String.valueOf(average));
                            else if(cnt == 2)
                                ((TextView) findViewById(R.id.tv230hzAverage2)).setText(String.valueOf(average));
                            else if(cnt == 3)
                                ((TextView) findViewById(R.id.tv230hzAverage3)).setText(String.valueOf(average));
                            sum_amplitude = 0;
                            amplitudes.clear();
                            break;
                        case 910:
                            average = sum_amplitude / (amplitudes.size() - NOISE);
                            averages[2] = average;
                            if(cnt == 1)
                                ((TextView) findViewById(R.id.tv910hzAverage1)).setText(String.valueOf(average));
                            else if(cnt == 2)
                                ((TextView) findViewById(R.id.tv910hzAverage2)).setText(String.valueOf(average));
                            else if(cnt == 3)
                                ((TextView) findViewById(R.id.tv910hzAverage3)).setText(String.valueOf(average));
                            sum_amplitude = 0;
                            amplitudes.clear();
                            break;
                        case 3600:
                            average = sum_amplitude / (amplitudes.size() - NOISE);
                            averages[3] = average;
                            if(cnt == 1)
                                ((TextView) findViewById(R.id.tv3600hzAverage1)).setText(String.valueOf(average));
                            else if(cnt == 2)
                                ((TextView) findViewById(R.id.tv3600hzAverage2)).setText(String.valueOf(average));
                            else if(cnt == 3)
                                ((TextView) findViewById(R.id.tv3600hzAverage3)).setText(String.valueOf(average));
                            sum_amplitude = 0;
                            amplitudes.clear();
                            break;
                    }
                }
            };


/*
            // Record max amplitudes per second
            _timer = new Timer();
            _timer.schedule(_timerTask,1000,1000);
*/


            _handler = new Handler();
            _handler.post(_startRecord);
            _handler.postDelayed(_stopRecord, MEASURING * 1000);        // 1000ms : 1sec

            /*
            _handler.postDelayed(_startRecord, (MEASURING + 1) * 1000);
            _handler.postDelayed(_stopRecord, (MEASURING * 2 + 1) * 1000);
            _handler.postDelayed(_startRecord, (MEASURING * 2 + 2) * 1000);
            _handler.postDelayed(_stopRecord, (MEASURING * 3 + 2) * 1000);
            */

/*
            switch (freqOfTone){
                case 60:
                    playFreq(60);
                    break;
                case 230:
                    playFreq(230);
                    break;
                case 910:
                    playFreq(910);
                    break;
                case 3600:
                    playFreq(3600);
                    break;
            }
*/

        } catch (Exception e) {
            Log.e("SampleAudioRecorder", "Exception : ", e);
        }
    }


    void genTone(int freq){
        // fill out the array
        for (int i = 0; i < numSamples; ++i) {
            sample[i] = Math.sin(2 * Math.PI * i / (sampleRate/freq));
        }

        // convert to 16 bit pcm sound array
        // assumes the sample buffer is normalised.
        int idx = 0;
        for (final double dVal : sample) {
            // scale to maximum amplitude
            final short val = (short) ((dVal * 32767));
            // in 16 bit wav PCM, first byte is the low order byte
            generatedSnd[idx++] = (byte) (val & 0x00ff);
            generatedSnd[idx++] = (byte) ((val & 0xff00) >>> 8);

        }
    }

    void playFreq(int freq){
        genTone(freq);

        if(audioTrack != null) {
            audioTrack.stop();
            audioTrack.release();
            audioTrack = null;
        }

        audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                sampleRate, AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT, numSamples,
                AudioTrack.MODE_STATIC);

        audioTrack.write(generatedSnd, 0, generatedSnd.length);
        audioTrack.play();
    }

    void stopFreq() {
        audioTrack.stop();
    }


}

