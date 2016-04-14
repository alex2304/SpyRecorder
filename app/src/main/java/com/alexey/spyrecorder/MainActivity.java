package com.alexey.spyrecorder;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Chronometer;
import android.widget.ImageButton;
import android.widget.NumberPicker;

import com.alexey.spyrecorder.source.AudioFileManager;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;


public class MainActivity extends Activity implements View.OnClickListener {

    private static final String TAG = "audioLog";
    //Параметры звукозаписи
    private static final int RECORDER_SAMPLERATE = 44100;                                //с какой частотой считывается звук, Гц
    private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_STEREO;           //кол-во каналов (1 или 2)
    private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;  //сколько памяти занимает одно считывание звука (бит)
    private static final int RECORDER_CHANNELS_COUNT = (RECORDER_CHANNELS == AudioFormat.CHANNEL_IN_STEREO) ? 2 : 1;
    private static final int RECORDER_BPP = (RECORDER_AUDIO_ENCODING == AudioFormat.ENCODING_PCM_16BIT) ? 16: 8;
    //Указатели на элементы интерфейса
    private AudioRecord audioRecorder;
    private ImageButton buttonRec, buttonStop, showHistoryButton;
    private Chronometer timer;
    private NumberPicker recordTimePicker;

    private long currFileSize;
    private long maxDiskSpace;
    private long remainingDiskSpace;
    private int recTime;
    private int myBufferSize;
    private boolean isReading, isRecording;
    private Thread recordingThread;
    private AudioFileManager audioFileManager;

    private SharedPreferences sp; //для настроек



    private void createAudioRecorder() {
        //Размер 1 секунды = RATE(Гц) * FORMAT(бит) *  КОЛ-ВО КАНАЛОВ (1 или 2)
        myBufferSize = 8192; //Размер буфера (в байтах) должен быть меньше RECORDER_SAMPLERATE*recTime/2
        int minInternalBufferSize = AudioRecord.getMinBufferSize(RECORDER_SAMPLERATE,
                RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING);
        if (minInternalBufferSize == AudioRecord.ERROR) {
            Log.d(TAG,"Can't recording: unable to query hardware");
            return;//throw exc
        } else if (minInternalBufferSize == AudioRecord.ERROR_BAD_VALUE){
            Log.d(TAG,"Can't recording: parameters don't supporting by hardware");
            return; //throw exc
        }
        int internalBufferSize = minInternalBufferSize * 4;
        Log.d(TAG, "minInternalBufferSize = " + minInternalBufferSize
                + ", internalBufferSize = " + internalBufferSize
                + ", myBufferSize = " + myBufferSize);

        audioRecorder = new AudioRecord(MediaRecorder.AudioSource.MIC, RECORDER_SAMPLERATE, RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING, internalBufferSize);
        audioRecorder.setPositionNotificationPeriod(RECORDER_SAMPLERATE * recTime);
        //audioRecorder.setNotificationMarkerPosition(10000);

        audioRecorder.setRecordPositionUpdateListener(new AudioRecord.OnRecordPositionUpdateListener() {
            public void onPeriodicNotification(AudioRecord recorder) {
                Log.d(TAG, "onPeriodicNotification");
                isReading = false; //установка флага, сообщающая потоку считывания, что надо начинать новый файл
            }

            public void onMarkerReached(AudioRecord recorder) {
                Log.d(TAG, "onMarkerReached");
            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        audioFileManager = new AudioFileManager();
        buttonRec = (ImageButton) findViewById(R.id.recButton);
        buttonStop = (ImageButton) findViewById(R.id.stopButton);
        showHistoryButton = (ImageButton) findViewById(R.id.showHistoryButton);
        buttonRec.setOnClickListener(this);
        buttonStop.setOnClickListener(this);
        showHistoryButton.setOnClickListener(this);
        timer = (Chronometer) findViewById(R.id.chronometer);
        timer.setText("00:00");
        recordTimePicker = (NumberPicker)findViewById(R.id.recordTimePicker);
        recordTimePicker.setMaxValue(60);
        recordTimePicker.setMinValue(10);
        recordTimePicker.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                /*if (!isRecording) {
                    recTime = newVal;
                } else {
                    picker.setValue(oldVal);
                }*/
            }
        });

        sp = PreferenceManager.getDefaultSharedPreferences(this);
        readPreferenceSettings();
        createAudioRecorder();
    }



    public void showHistory(){
        Intent historyIntent = new Intent(this, HistoryActivity.class);
        //Передача списка файлов при помощи parcelable
        /*List<AudioEntity> files = audioFileManager.getAudioListFromFiles();
        historyIntent.putExtra("audioFilesCount", files.size());
        for (AudioEntity audioFile : files){
            historyIntent.putExtra(AudioEntity.class.getCanonicalName(), audioFile);
        }*/
        startActivity(historyIntent);
    }

    @Override
    public void onClick(View v) {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        switch (v.getId()) {
            //Запись
            case R.id.recButton:
                startRecord();
                break;
            //Стоп запись
            case R.id.stopButton:
                stopRecord();
                break;
            case R.id.showHistoryButton:
                showHistory();
                break;
            default:
                break;
        }
    }

    private void stopRecord() {
        Log.d(TAG, "record stop");
        audioRecorder.stop();
        timer.stop();
        if (recordingThread != null){
            recordingThread.interrupt();
            recordingThread = null;
        }
        isRecording = false;
        isReading = false;
    }

    private void writeAudioDataToFile(){
        isReading = isRecording; //проверка, идёт ли запись
        byte data[] = new byte[myBufferSize];
        String tempFileName = audioFileManager.getTempFilename();
        FileOutputStream os = null;
        int totalCount = 0;

        try {
            os = new FileOutputStream(tempFileName);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        int read = 0;
        if(os != null){
            //timer.setBase(SystemClock.elapsedRealtime());
            timer.start();
            while(isReading){
                read = audioRecorder.read(data, 0, myBufferSize);

                if(read != AudioRecord.ERROR_INVALID_OPERATION){
                    try {
                        os.write(data);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                totalCount += read;
                Log.d(TAG, "readCount = " + read + ", totalCount = "
                        + totalCount);
            }
            //timer.stop();
            try {
                os.close();
                remainingDiskSpace = audioFileManager.getRemainingDiskSpace(maxDiskSpace);
                boolean allFilesRemovedFlag = false;
                while (currFileSize > remainingDiskSpace && !allFilesRemovedFlag){
                    allFilesRemovedFlag = !audioFileManager.removeFile(0); //всегда удаляется самый первый файл, т.к. они уже отсортированы по дате
                    remainingDiskSpace = audioFileManager.getRemainingDiskSpace(maxDiskSpace);
                }
                if (allFilesRemovedFlag && currFileSize > remainingDiskSpace){ //все файлы удалены, а места всё равно нет
                    //throw exc;
                }
                String audioFileName = audioFileManager.resolveAudioFileName();
                copyWaveFile(tempFileName, audioFileName);
                Log.d(TAG, "Temp file writed to " + audioFileName);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void startRecord() {
        if (audioRecorder.getState() != 1 || isRecording) {
            Log.d(TAG, "Recorder can't initialize!");
            return;
        }
        audioRecorder.startRecording();
        if (audioRecorder.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING){ //запись идёт
            Log.d(TAG, "Record started.");
            isRecording = true;
            timer.setBase(SystemClock.elapsedRealtime());
            timer.start();
            recordingThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    if (audioRecorder == null)
                        return;
                    while (isRecording)
                        writeAudioDataToFile(); //запись во временный файл
                }
            });
            recordingThread.start();
        } else {
            Log.d(TAG, "Record can't start.");
        }
    }

    private void copyWaveFile(String inFilename, String outFilename){
        FileInputStream in = null;
        FileOutputStream out = null;
        long totalAudioLen = 0;
        long totalDataLen = totalAudioLen + 36;
        long longSampleRate = RECORDER_SAMPLERATE;
        int channels = (RECORDER_CHANNELS == AudioFormat.CHANNEL_IN_MONO) ? 1: 2;
        long byteRate = RECORDER_BPP * RECORDER_SAMPLERATE * channels/8;

        byte[] data = new byte[myBufferSize];

        try {
            in = new FileInputStream(inFilename);
            out = new FileOutputStream(outFilename);
            totalAudioLen = in.getChannel().size();
            totalDataLen = totalAudioLen + 36;

            Log.d(TAG, Long.toString(totalDataLen));

            WriteWaveFileHeader(out, totalAudioLen, totalDataLen,
                    longSampleRate, channels, byteRate);

            while(in.read(data) != -1){
                out.write(data);
            }

            in.close();
            out.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void WriteWaveFileHeader(
            FileOutputStream out, long totalAudioLen,
            long totalDataLen, long longSampleRate, int channels,
            long byteRate) throws IOException {

        byte[] header = new byte[44];

        header[0] = 'R'; // RIFF/WAVE header
        header[1] = 'I';
        header[2] = 'F';
        header[3] = 'F';
        header[4] = (byte) (totalDataLen & 0xff);
        header[5] = (byte) ((totalDataLen >> 8) & 0xff);
        header[6] = (byte) ((totalDataLen >> 16) & 0xff);
        header[7] = (byte) ((totalDataLen >> 24) & 0xff);
        header[8] = 'W';
        header[9] = 'A';
        header[10] = 'V';
        header[11] = 'E';
        header[12] = 'f'; // 'fmt ' chunk
        header[13] = 'm';
        header[14] = 't';
        header[15] = ' ';
        header[16] = 16; // 4 bytes: size of 'fmt ' chunk
        header[17] = 0;
        header[18] = 0;
        header[19] = 0;
        header[20] = 1; // format = 1
        header[21] = 0;
        header[22] = (byte) channels;
        header[23] = 0;
        header[24] = (byte) (longSampleRate & 0xff);
        header[25] = (byte) ((longSampleRate >> 8) & 0xff);
        header[26] = (byte) ((longSampleRate >> 16) & 0xff);
        header[27] = (byte) ((longSampleRate >> 24) & 0xff);
        header[28] = (byte) (byteRate & 0xff);
        header[29] = (byte) ((byteRate >> 8) & 0xff);
        header[30] = (byte) ((byteRate >> 16) & 0xff);
        header[31] = (byte) ((byteRate >> 24) & 0xff);
        header[32] = (byte) (2 * 16 / 8); // block align
        header[33] = 0;
        header[34] = RECORDER_BPP; // bits per sample
        header[35] = 0;
        header[36] = 'd';
        header[37] = 'a';
        header[38] = 't';
        header[39] = 'a';
        header[40] = (byte) (totalAudioLen & 0xff);
        header[41] = (byte) ((totalAudioLen >> 8) & 0xff);
        header[42] = (byte) ((totalAudioLen >> 16) & 0xff);
        header[43] = (byte) ((totalAudioLen >> 24) & 0xff);

        out.write(header, 0, 44);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent settingsIntent = new Intent(this, SettingsActivity.class);
            startActivity(settingsIntent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();

        if (audioRecorder != null) {
            audioRecorder.release();
        }
    }

    @Override
    protected void onResume() {
        readPreferenceSettings();
        super.onResume();
    }

    private void readPreferenceSettings(){
        if (!isRecording) {
            maxDiskSpace = (long)Integer.parseInt(sp.getString("pref_disk_limit", "50"));
            maxDiskSpace *= (1024 * 1024);
            recTime = Integer.parseInt(sp.getString("pref_file_length", "20"));
            currFileSize = RECORDER_SAMPLERATE * RECORDER_BPP * RECORDER_CHANNELS_COUNT * recTime + 44; //44 байта занимает заголовок WAV-файла
        }
    }
}
