
package me.pjq.musicplayer;


import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

public class MusicPlayerItem implements Parcelable {
    String url;

    String name;

    String description;

    // millionsecciond
    int position;

    // 区分是本地还是服务器url
    int fileType;

    int time;

    int playingStatus;

    int isNeedOrder;

    int musicId;

    String musicAlbumId;

    long fileSize;

    public MusicPlayerItem() {
        url = "";
        name = "";
        time = 0;
        description = "";
        position = 0;
        fileType = MusicPlayerConstants.TYPE_URL;
        playingStatus = MusicPlayerConstants.STATUS_INVALID;
        isNeedOrder = MusicPlayerConstants.BOOLEAN_FALSE;
        musicId = 0;
        musicAlbumId = "";
        fileSize = 0;
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("url=" + url + '\n');
        stringBuilder.append("name=" + name + '\n');
        stringBuilder.append("time=" + time + '\n');
        stringBuilder.append("description=" + description + '\n');
        stringBuilder.append("position=" + position + '\n');
        stringBuilder.append("fileType=" + fileType + '\n');
        stringBuilder.append("playingStatus=" + playingStatus + '\n');
        stringBuilder.append("isNeedOrder=" + isNeedOrder + '\n');
        stringBuilder.append("musicId=" + musicId + '\n');
        stringBuilder.append("mRpidBookId=" + musicAlbumId + '\n');
        return stringBuilder.toString();
    }


    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeString(url);
        out.writeString(name);
        out.writeString(description);
        out.writeInt(position);
        out.writeInt(fileType);
        out.writeInt(time);
        out.writeInt(playingStatus);
        out.writeInt(isNeedOrder);
        out.writeInt(musicId);
        out.writeString(musicAlbumId);
        out.writeLong(fileSize);
    }

    public static final Creator<MusicPlayerItem> CREATOR = new Creator<MusicPlayerItem>() {
        public MusicPlayerItem createFromParcel(Parcel in) {
            return new MusicPlayerItem(in);
        }

        public MusicPlayerItem[] newArray(int size) {
            return new MusicPlayerItem[size];
        }
    };

    private MusicPlayerItem(Parcel in) {
        readFromParcel2(in);
    }

    /**
     * 暂时还不知道这个方法的用法，但必须要有
     *
     * @param in
     */
    private void readFromParcel2(Parcel in) {
        url = in.readString();
        name = in.readString();
        description = in.readString();
        position = in.readInt();
        fileType = in.readInt();
        time = in.readInt();
        playingStatus = in.readInt();
        isNeedOrder = in.readInt();
        musicId = in.readInt();
        musicAlbumId = in.readString();
        fileSize = in.readLong();
    }

    public void readFromParcel(Parcel in) {
        readFromParcel2(in);
        // url = in.readString();
        // name = in.readString();
        // description = in.readString();
        // position = in.readInt();
        // fileType = in.readInt();
        // time = in.readInt();
        // in.readTypedList(list, CREATOR);
    }

    // @Override
    // public boolean equals(Object o) {
    //
    // return true;
    // }
    @Override
    public int hashCode() {
        int result = 17;

        result = result + musicId * 31;

        // if (null != name) {
        // result = result + musicId * 31;
        // result = result + name.hashCode() * 31;
        // } else {
        // result = super.hashCode();
        // }

        return result;
    }

    public boolean equals(Object o) {
        if (null == o) {
            return false;
        }

        MusicPlayerItem other = (MusicPlayerItem) o;

//        if (musicId == other.getMusicId() && null != name && name.equalsIgnoreCase(other.name)) {
//        if (musicId == other.getMusicId()) {
        if (url.equalsIgnoreCase(other.getUrl())) {
            return true;
        } else {
            return false;
        }
    }

    public String getUrl() {
        if (null == url) {
            url = "";
        }
        // url = URLEncoder.encode(url);
        url = url.replace(" ", "%20");

        return url;
    }

    public void setUrl(String url) {
        if (!TextUtils.isEmpty(url)) {
            // url = URLEncoder.encode(url);
        }

        this.url = url;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getTime() {
        return time;
    }

    public String getTimeReadable() {
        int ss = time / 1000;
        int bb_1 = ss / 60;
        int bb_2 = ss % 60;
        String bb_11 = "";
        String bb_22 = "";

        if (bb_1 < 10) {
            bb_11 = "0" + bb_1;
        } else {
            bb_11 = "" + bb_1;
        }
        if (bb_2 < 10) {
            bb_22 = "0" + bb_2;
        } else {
            bb_22 = "" + bb_2;
        }
        String bofangshijian = bb_11 + ":" + bb_22;
        return bofangshijian;
    }

    public void setTime(int time) {
        this.time = time;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public int getFileType() {
        return fileType;
    }

    public void setFileType(int fileType) {
        this.fileType = fileType;
    }

    public boolean isLocalFileType() {
        return !isUrlType();
    }

    public boolean isUrlType() {
        if (!TextUtils.isEmpty(url)) {
            if (url.startsWith("file://")) {
                return false;
            } else if (url.startsWith("http://")) {
                return true;
            }
        }

        if (fileType == MusicPlayerConstants.TYPE_URL) {
            return true;
        } else {
            return false;
        }
    }

    public int getPlayingStatus() {
        return playingStatus;
    }

    public void setPlayingStatus(int playingStatus) {
        this.playingStatus = playingStatus;
    }

    public boolean isPlaying() {
        return MusicPlayerConstants.STATUS_STARTED == playingStatus;
    }

    public int getIsNeedOrder() {
        return isNeedOrder;
    }

    public void setIsNeedOrder(boolean isNeedOrder) {
        this.isNeedOrder = true == isNeedOrder ? MusicPlayerConstants.BOOLEAN_TRUE
                : MusicPlayerConstants.BOOLEAN_FALSE;
    }

    public boolean isNeedOrder() {
        return MusicPlayerConstants.BOOLEAN_TRUE == isNeedOrder;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public int getMusicId() {
        return musicId;
    }

    public void setMusicId(int musicId) {
        this.musicId = musicId;
    }

    public String getMusicAlbumId() {
        return musicAlbumId;
    }

    public void setMusicAlbumId(String musicAlbumId) {
        this.musicAlbumId = musicAlbumId;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }
}
