package com.alexey.spyrecorder.source;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.File;
import java.util.Date;

/**
 * Created by Leha on 06-Apr-16.
 */
public class AudioEntity implements Parcelable, Comparable<AudioEntity>{
    private String fileName;
    private String filePath;
    private int id;
    private int spId;
    private Date lastModifiedDate;

    public AudioEntity(String audioFileName, String filePath, int id, Date date) {
        this.fileName = audioFileName;
        this.filePath = filePath;
        this.setId(id);
        this.setSpId(-1);
        this.lastModifiedDate = date;
        //readLastModifiedDate();
    }

    private AudioEntity(Parcel source) {
        fileName = source.readString();
        filePath = source.readString();
        id = source.readInt();
        setSpId(source.readInt());
        lastModifiedDate = (Date)source.readValue(ClassLoader.getSystemClassLoader()); //TEMPORALY
    }

    /*private void readLastModifiedDate(){
        File audioFile = new File(filePath + "/" + fileName);
        if (audioFile.exists()) {
            lastModifiedDate = new Date(audioFile.lastModified());
        } else {
            lastModifiedDate = null;
        }
    }*/

    public Date getLastModifiedDate(){
        return lastModifiedDate;
    }

    public void rename(String newFileName){
        File audioFile = new File(filePath + "/" + fileName);
        if (audioFile.exists()) {
            audioFile.renameTo(new File(filePath + "/" + newFileName));
        }
    }

    public String getFileName() {
        return fileName;
    }

    public String getFilePath() {
        return filePath;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(fileName);
        dest.writeString(filePath);
        dest.writeInt(id);
        dest.writeInt(getSpId());
        dest.writeValue(lastModifiedDate);
    }

    public static final Parcelable.Creator<AudioEntity> CREATOR = new Parcelable.Creator<AudioEntity>(){

        @Override
        public AudioEntity createFromParcel(Parcel source) {
            return new AudioEntity(source);
        }

        @Override
        public AudioEntity[] newArray(int size) {
            return new AudioEntity[size];
        }
    };

    public int getSpId() {
        return spId;
    }

    public void setSpId(int spId) {
        this.spId = spId;
    }

    @Override
    public int compareTo(AudioEntity another) {
        if (this.lastModifiedDate != null) {
            int dateCompare = this.lastModifiedDate.compareTo(another.lastModifiedDate);
            if (dateCompare != 0)
                return dateCompare;
        }

        int nameCompare = this.fileName.compareTo(another.fileName);
        if (nameCompare != 0)
            return nameCompare;

        int id = Integer.compare(this.id, another.id);
        if (id != 0)
            return id;

        return Integer.compare(this.spId, another.spId);
    }
}
