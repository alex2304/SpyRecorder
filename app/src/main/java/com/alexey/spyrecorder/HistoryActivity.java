package com.alexey.spyrecorder;

import android.app.Activity;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.alexey.spyrecorder.source.AudioEntity;
import com.alexey.spyrecorder.source.AudioFileManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;


public class HistoryActivity extends Activity {

    private ListView historyList;
    private EditText inputDate, inputTime;
    private MediaPlayer mediaPlayer;
    private AudioFileManager audioFileManager;
    private ArrayAdapter<String> viewAdapter;

    private int playingThreadId;
    private List<String> filesNamesForTable;
    private Date filterDate;
    private int filterOffset;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        //Создание и настрока объекта SoundPool
        AudioAttributes attributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build();
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                return false;
            }
        });
        mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                mp.start();
            }
        });
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {

            }
        });

        //Создание с SoundPool
        /*soundPool = new SoundPool.Builder()
                .setAudioAttributes(attributes)
                .setMaxStreams(1)
                .build();
        soundPool.setOnLoadCompleteListener(new SoundPool.OnLoadCompleteListener() {
            @Override
            public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {

            }
        }); */

        //Получение списка файлов при помощи parcelable
        /*Intent mainActivityIntent = getIntent();
        int listSize = mainActivityIntent.getIntExtra("audioFilesCount", 0);
        for (int i = 0; i < listSize; i++){
            recordedFiles.add( (AudioEntity) mainActivityIntent.getParcelableExtra(AudioEntity.class.getCanonicalName()) );
        }*/

        //Создание менеджера аудиофайлов
        audioFileManager = new AudioFileManager();
        filesNamesForTable = audioFileManager.getAudioNamesListFromFiles(null);
        //loadAudioToSP(); //загрузка доступных файлов в SoundPool
        playingThreadId = -1;

        //Настройка списка
        historyList = (ListView)findViewById(R.id.historyView);
        historyList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                try {
                    mediaPlayer.reset();
                    mediaPlayer.setDataSource(audioFileManager.getAudioFilePath(position));
                    mediaPlayer.prepareAsync();
                    //playingThreadId = soundPool.play(, 1, 1, 0, 0, 1);
                } catch (IndexOutOfBoundsException e){
                    //notify that file not found
                } catch (IOException e){
                    //notify that file not found
                }
            }
        });
        viewAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, filesNamesForTable);
        historyList.setAdapter(viewAdapter);

        //Настройки ввода фильтра даты
        filterDate = null;
        inputTime = (EditText)findViewById(R.id.timeInput);
        inputDate = (EditText)findViewById(R.id.dateInput);
        inputDate.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                try {
                    String inputString = s.toString();
                    String[] strParts = inputString.split("\\.");
                    GregorianCalendar calendar = new GregorianCalendar(Integer.parseInt(strParts[2]), Integer.parseInt(strParts[1]) - 1, Integer.parseInt(strParts[0]));
                    filterDate = calendar.getTime();
                } catch (Exception e){
                    filterDate = null;
                }
                setDateFilter();
            }
        });
        inputTime.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
    }

    //Temporaly: Оптимизировать постоянную загрузку-выгрузку
    private void setDateFilter(){
        if (filterDate != null){
            //unloadAudioFromSP();
            filesNamesForTable.clear();
            filesNamesForTable.addAll(audioFileManager.getAudioNamesListFromFiles(filterDate));
            //loadAudioToSP();
            viewAdapter.notifyDataSetChanged();
        }
    }

    /*private void unloadAudioFromSP(){
        //Выгрузка старых (если есть)
        for (int i = 0; i < filesNamesForTable.size(); i++){
            if (audioFileManager.getAudioSPId(i) != -1)
                soundPool.unload(audioFileManager.getAudioSPId(i));
        }
    }*/

    /*
    private void loadAudioToSP(){
        //Загрузка новых
        for (int i = 0; i < filesNamesForTable.size(); i++){
            try {
                int spId = soundPool.load(audioFileManager.getAudioFilePath(i), 1);
                audioFileManager.setAudioSPId(i, spId);
            } catch (IndexOutOfBoundsException e){
                //remove file that not found
            }
        }
    }*/


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_history, menu);
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
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();

        if (mediaPlayer != null){
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }
}
