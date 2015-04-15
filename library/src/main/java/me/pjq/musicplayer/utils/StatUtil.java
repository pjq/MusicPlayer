
package me.pjq.musicplayer.utils;


import me.pjq.musicplayer.MusicPlayerItem;

/**
 * 数据统计对外提供统一接口
 *
 * @author pengjianqing
 */
public class StatUtil {


    /**
     * 听书行为表<br>
     */
    public static void onTingshuBehavior(String rpidBookid, String chapterid, String source) {
    }

    /**
     * 有声书播放错误统计<br>
     * rpid: bookid: chapterid: uri: detail: occur_ts:事件发生时间 upload_ts:记录上传时间
     */
    public static void onTingshuPlayError(MusicPlayerItem musicPlayerItem, String detail) {

    }
}
