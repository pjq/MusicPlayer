
package  me.pjq.musicplayer;

import  me.pjq.musicplayer.IMusicPlayerListener;
import  me.pjq.musicplayer.MusicPlayerItem;

interface IMusicPlayerService {

    boolean setPlayerListener(IMusicPlayerListener listener);
    
    boolean unSetPlayerListener(IMusicPlayerListener listener);

    /**
     * 播放下一个，如果无法播放或者到最后一个时,返回false
     * 
     * @return
     */
     boolean playNextItem();

    /**
     * 播放上一个，如果无法播放或者是第一个时，返回false
     * 
     * @return
     */
     boolean playPrevItem();

      List<MusicPlayerItem> getPlayerList();

      MusicPlayerItem getCurrentPlayingItem();

      int getCurrentPlayingIndex();

      int getCurrentPlayingTotalTime();
      
     /**
      * 获取当前播放列表数量
      */
      int getPlayerListCount();

    /**
     * 判断播放器是否准备好了,如果false,则无法响应播放/暂停
     * 
     * @return
     */
      boolean isPlayerPrepared();

    /**
     * 获得当前播放器的播放状态
     * 
     * @return
     */
      int getPlayerPlayingStatus();

      boolean start();

      boolean pause();

      boolean stop();

      boolean isPlaying();

    /**
     * 判断是否可以播放下一首
     * 
     * @return
     */
      boolean canForward();

    /**
     * 判断是否可以往前播放
     * 
     * @return
     */
      boolean canGoBack();
      
      boolean isMediaPlayerStatusInvalid();
      
      void clearPlayList();
      
      void seekTo(int position);
      
      void savePlayingStatus();
}
