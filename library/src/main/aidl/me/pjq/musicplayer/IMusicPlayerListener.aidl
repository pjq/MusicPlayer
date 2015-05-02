package  me.pjq.musicplayer;

import  me.pjq.musicplayer.MusicPlayerItem;
import  me.pjq.musicplayer.MusicPlayerConfig;

interface  IMusicPlayerListener{
   String getListenerTag();

   void onVideoSizeChanged(int width, int height);
   void onSeekComplete();
   boolean onInfo(int what, int extra);
   void onCompletion();
   void onPrepared();
   boolean onError(int what, int extra);
   void onBufferingUpdate(int percent);

    /**
     * 停止播放
     */
     void onStop();

    /**
     * 暂停播放
     */
     void onPause();

    /**
     * 开始播放
     */
     void onStart();

    /**
     * 快速跳转到
     * 
     * @param position
     */
     void onSeekTo(int position);


    /**
     * 播放前一个
     * 
     * @param object
     */
     void onPrev(inout MusicPlayerItem object,int itemIndex);

    /**
     * 播放下一个
     * 
     * @param object
     */
     void onNext(inout MusicPlayerItem object,int itemIndex);

    /**
     * 开始播放前
     */
     void onPrePlaying(inout MusicPlayerItem item,int itemIndex);

    /**
     * 开始播放
     */
     void onStartPlaying(inout MusicPlayerItem item);

     void onUpdatePlayingProgress(inout MusicPlayerItem item, int itemIndex, int position);

     void onUpdateStepCount(int stepCount);

     void onUpdateStepFrequency(float freq, float avgFreq);

    /**
     * 通知播放界面，给一些提示
     */
     void onShowMessage(inout MusicPlayerItem item, int type, String message);
     
     MusicPlayerConfig getPlayerConfig();
     
    /**
     * 播放列表改变
     */
     void onPlayerListChange();
     
    /**
     * 保存当前播放进度
     */
     void onSavePlayingProgress(inout MusicPlayerItem item);
}