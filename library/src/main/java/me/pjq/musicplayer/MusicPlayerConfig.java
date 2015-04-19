
package me.pjq.musicplayer;

import android.os.Parcel;
import android.os.Parcelable;

public class MusicPlayerConfig implements Parcelable {

    /**
     * 自动播放
     */
    int autoPlay;

    /**
     * 连续播放
     */
    int continuePlay;

    /**
     * 快捷关闭
     */
    int quickStop;

    /**
     * 设定关闭时间
     */
    int autoStopTime;

    /**
     * 设定关闭集数
     */
    int autoStopChapterCount;

    /**
     * 流量保护
     */
    int dataProtect;

    int shakeToNext;

    public MusicPlayerConfig() {
        autoPlay = MusicPlayerConstants.BOOLEAN_TRUE;
        continuePlay = MusicPlayerConstants.BOOLEAN_TRUE;
        quickStop = MusicPlayerConstants.BOOLEAN_FALSE;

        autoStopTime = -1;
        autoStopChapterCount = -1;
        dataProtect = MusicPlayerConstants.BOOLEAN_TRUE;
        shakeToNext = MusicPlayerConstants.BOOLEAN_TRUE;
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("autoPlay=" + isAutoPlay() + '\n');
        stringBuilder.append("continuePlay=" + isContinuePlay() + '\n');
        stringBuilder.append("quickStop=" + isQuickStop() + '\n');
        stringBuilder.append("autoStopTime=" + getAutoStopTime() + '\n');
        stringBuilder.append("autoStopChapterCount=" + getAutoStopChapterCount() + '\n');
        stringBuilder.append("dataProtect=" + isDataProtect() + '\n');
        stringBuilder.append("shakeToNext=" + isShakeToNext() + '\n');
        return stringBuilder.toString();
    }

    // @Override
    // public PlayerConfig clone() throws CloneNotSupportedException {
    // PlayerConfig item = new PlayerConfig();
    // item.autoPlay = autoPlay;
    // item.continuePlay = continuePlay;
    // item.quickStop = quickStop;
    // item.autoStopTime = autoStopTime;
    // item.autoStopChapterCount = autoStopChapterCount;
    // item.dataProtect = dataProtect;
    //
    // return item;
    // }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeInt(autoPlay);
        out.writeInt(continuePlay);
        out.writeInt(quickStop);
        out.writeInt(autoStopTime);
        out.writeInt(autoStopChapterCount);
        out.writeInt(dataProtect);
        out.writeInt(shakeToNext);
    }

    public static final Creator<MusicPlayerConfig> CREATOR = new Creator<MusicPlayerConfig>() {
        public MusicPlayerConfig createFromParcel(Parcel in) {
            return new MusicPlayerConfig(in);
        }

        public MusicPlayerConfig[] newArray(int size) {
            return new MusicPlayerConfig[size];
        }
    };

    private MusicPlayerConfig(Parcel in) {
        readFromParcel2(in);
    }

    /**
     * 暂时还不知道这个方法的用法，但必须要有
     *
     * @param in
     */
    private void readFromParcel2(Parcel in) {
        autoPlay = in.readInt();
        continuePlay = in.readInt();
        quickStop = in.readInt();
        autoStopTime = in.readInt();
        autoStopChapterCount = in.readInt();
        dataProtect = in.readInt();
        shakeToNext = in.readInt();
    }

    public void readFromParcel(Parcel in) {
    }

    @Override
    public int describeContents() {
        return 0;
    }

    private int getAutoPlay() {
        return autoPlay;
    }

    public void setAutoPlay(int autoPlay) {
        this.autoPlay = autoPlay;
    }

    private boolean isAutoPlay() {
        return MusicPlayerConstants.BOOLEAN_TRUE == autoPlay;
    }

    public int getContinuePlay() {
        return continuePlay;
    }

    public void setContinuePlay(int continuePlay) {
        this.continuePlay = continuePlay;
    }

    public boolean isContinuePlay() {
        return MusicPlayerConstants.BOOLEAN_TRUE == continuePlay;
    }

    public int getQuickStop() {
        return quickStop;
    }

    public void setQuickStop(int quickStop) {
        this.quickStop = quickStop;
    }

    public boolean isQuickStop() {
        return MusicPlayerConstants.BOOLEAN_TRUE == quickStop;
    }

    public void setShakeToNext(boolean shakeToNext) {
        if (shakeToNext) {
            this.shakeToNext = MusicPlayerConstants.BOOLEAN_TRUE;
        } else {
            this.shakeToNext = MusicPlayerConstants.BOOLEAN_FALSE;
        }
    }

    public boolean isShakeToNext() {
        return MusicPlayerConstants.BOOLEAN_TRUE == shakeToNext;
    }

    public int getAutoStopTime() {
        return autoStopTime;
    }

    public void setAutoStopTime(int autoStopTime) {
        this.autoStopTime = autoStopTime;
    }

    public int getAutoStopChapterCount() {
        return autoStopChapterCount;
    }

    public void setAutoStopChapterCount(int autoStopChapterCount) {
        this.autoStopChapterCount = autoStopChapterCount;
    }

    public int getDataProtect() {
        return dataProtect;
    }

    public void setDataProtect(int dataProtect) {
        this.dataProtect = dataProtect;
    }

    public boolean isDataProtect() {
        return MusicPlayerConstants.BOOLEAN_TRUE == dataProtect;
    }

}
