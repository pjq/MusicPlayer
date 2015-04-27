
package me.pjq.musicplayer;

/**
 * 播放器和UI交互接口
 * 
 * @author pengjianqing
 */
public interface MusicPlayerUIBridgeInterface {

    MusicPlayerConfig getPlayerConfig();

    void onShowMessage(MusicPlayerItem item, int type, String message);

    void onUpdateStepCount(int stepCount);

    void onUpdateStepReq(float freq);
}
