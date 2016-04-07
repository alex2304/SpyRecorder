package com.alexey.spyrecorder.source;

import android.os.Environment;
import android.provider.MediaStore;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Created by Leha on 06-Apr-16.
 */
public class AudioFileManager {
    //Параметры работы с файлами
    private static final String AUDIO_RECORDER_FILE_EXT_WAV = ".wav";
    private static final String AUDIO_RECORDER_PATH = Environment.getExternalStorageDirectory().getPath();
    private static final String AUDIO_RECORDER_FOLDER = "AudioRecorder";
    private static final String AUDIO_RECORDER_TEMP_FILE = "record_temp.raw";

    private List<AudioEntity> recordedFiles;

    public AudioFileManager(){
        recordedFiles = new ArrayList<>();
    }

    public int getAudioSPId(int audioFileindex) throws IndexOutOfBoundsException{
        if (audioFileindex >= recordedFiles.size()) throw new IndexOutOfBoundsException("Index of audio file in list '" + Integer.toString(audioFileindex) + "' more than array of files size.");
        return recordedFiles.get(audioFileindex).getSpId();
    }

    public void setAudioSPId(int audioFileindex, int spId) throws IndexOutOfBoundsException{
        if (audioFileindex >= recordedFiles.size()) throw new IndexOutOfBoundsException("Index of audio file in list '" + Integer.toString(audioFileindex) + "' more than array of files size.");
        recordedFiles.get(audioFileindex).setSpId(spId);
    }

    public String getAudioFilePath(int audioFileindex) throws IndexOutOfBoundsException{
        if (audioFileindex >= recordedFiles.size()) throw new IndexOutOfBoundsException("Index of audio file in list '" + Integer.toString(audioFileindex) + "' more than array of files size.");
        AudioEntity entity = recordedFiles.get(audioFileindex);
        return entity.getFilePath() + "/" + entity.getFileName();
    }

    public List<String> getAudioNamesListFromFiles(Date dateFilter){
        getAudioListFromFiles(dateFilter);
        List<String> filesNamesList = new ArrayList<>();
        int counter = 0;
        for (AudioEntity e : recordedFiles){
            filesNamesList.add(e.getFileName());
        }
        return filesNamesList;
    }

    public List<AudioEntity> getAudioListFromFiles(Date dateFilter){
        recordedFiles.clear();
        String path = AUDIO_RECORDER_PATH + "/" + AUDIO_RECORDER_FOLDER;
        File folder = new File(path);
        if (folder.exists()){
            File[] filesList = new File(path).listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String filename) {
                    return (filename.toLowerCase().endsWith(AUDIO_RECORDER_FILE_EXT_WAV));
                }
            });
            int i = 0;
            Date fDate = new Date();
            for (File f : filesList){
                fDate.setTime(f.lastModified());
                if (dateFilter == null){ //без фильтра, если он не нужен
                    recordedFiles.add(new AudioEntity(f.getName(), path, i, fDate));
                    i++;
                    continue;
                }
                if (fDate.after(dateFilter)) { //применение фильтра
                    recordedFiles.add(new AudioEntity(f.getName(), path, i, fDate));
                    i++;
                }
            }
        }
        Collections.sort(recordedFiles);
        return recordedFiles;
    }

    public String resolveAudioFileName(){
        File file = new File(AUDIO_RECORDER_PATH, AUDIO_RECORDER_FOLDER);

        if (!file.exists()){
            file.mkdirs();
        }

        long date = System.currentTimeMillis();
        SimpleDateFormat dateFormat = new SimpleDateFormat("k-mm-ss dd.MM");

        return (file.getAbsolutePath() + "/" + dateFormat.format(date) + AUDIO_RECORDER_FILE_EXT_WAV);
    }

    public String getTempFilename(){
        String filepath = AUDIO_RECORDER_PATH;
        File file = new File(filepath, AUDIO_RECORDER_FOLDER);

        if(!file.exists()){
            file.mkdirs();
        }

        File tempFile = new File(filepath, AUDIO_RECORDER_TEMP_FILE);

        if(tempFile.exists())
            tempFile.delete();

        return (file.getAbsolutePath() + "/" + AUDIO_RECORDER_TEMP_FILE);
    }
}
