
package me.pjq.musicplayer.utils;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;


import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

import me.pjq.musicplayer.MusicAlbumObject;
import me.pjq.musicplayer.MusicNotificationClassProvider;
import me.pjq.musicplayer.MusicPlayerConstants;
import me.pjq.musicplayer.MusicPlayerItem;
import me.pjq.musicplayer.R;


/**
 * 有声书相关工具类，包括与{@link me.pjq.musicplayer.MusicPlayerService}进行交互，购买，启动听书相关的方法
 *
 * @author pengjianqing
 */
public class PlayerUtils {
    private static final String TAG = PlayerUtils.class.getSimpleName();

    public static final String TEST_URL = "http://listen.tingbook.com/Android/AndroidPlayerUrl.aspx?bookid=3793&volumenum=1";

    public static final String LOCAL_FILE_TYPE = "file:///mnt/sdcard/mp3/1.mp3";

    public static final int TOTAL_TIME = (1 * 60 + 32) * 1000;

    public static void playTest(Context context) {
        // play(context, TEST_URL);

        if (true) {
            getMusicListByDirectory(context, "/sdcard/netease/cloudmusic/Music");
            return;
        }
    }

    public static ArrayList<MusicPlayerItem> playByDirectory(Context context) {
        ArrayList<MusicPlayerItem> list = getMusicListByDirectory(context, "/sdcard/netease/cloudmusic/Music");
//        appendList(context, list);

        return list;
    }

    public static ArrayList<MusicPlayerItem> getMusicListByDirectory(Context context, String path) {
        ArrayList<MusicPlayerItem> list = new ArrayList<MusicPlayerItem>();

        File file = new File(path);
        if (file.exists() && file.isDirectory()) {
            File[] files = file.listFiles();
            for (File item : files) {
                if (item.isDirectory()) {
                    File[] files2 = item.listFiles();
                    for (File item2 : files2) {
                        MusicPlayerItem musicPlayerItem = fileToPlayerItem(context, item2);
                        if (null != musicPlayerItem) {
                            list.add(musicPlayerItem);
                        }

                        MusicPlayerItem urlItem = new MusicPlayerItem();
                        urlItem.setUrl(TEST_URL);
                        urlItem.setDescription("unknown description");
                        urlItem.setName("Sunshine girl");
                        urlItem.setTime(TOTAL_TIME);
                        urlItem.setFileType(MusicPlayerConstants.TYPE_URL);
                        // appendItem(context, urlItem);
                    }
                } else {
                    MusicPlayerItem musicPlayerItem = fileToPlayerItem(context, item);
                    if (null != musicPlayerItem) {
                        list.add(musicPlayerItem);
                    }
                }
            }
        }

        return list;
    }

    public static void play(Context context, String url) {
        play(context, Uri.parse(url));
    }

    public static void play(Context context, Uri uri) {
        if (null == uri) {
            return;
        }

        Intent intent = new Intent();
        intent.setAction(MusicPlayerConstants.BIND_ACTION);
        Bundle bundle = new Bundle();
        bundle.putInt(MusicPlayerConstants.KEY_PLAYER_COMMAND, MusicPlayerConstants.COMMAND_PLAYER_START);
        MusicPlayerItem item = new MusicPlayerItem();
        item.setUrl(uri.toString());
        item.setDescription(uri.toString());
        item.setName(uri.toString());

        if (uri.getScheme().equalsIgnoreCase("file")) {
            item.setFileType(MusicPlayerConstants.TYPE_LOCAL);
        } else if (uri.getScheme().equalsIgnoreCase("http")) {
            item.setFileType(MusicPlayerConstants.TYPE_URL);
        }

        bundle.putParcelable(MusicPlayerConstants.KEY_ITEM, item);
        intent.putExtras(bundle);

        context.startService(intent);
    }

    public static void play(Context context, MusicPlayerItem item) {
        printTime("play");

        Intent intent = new Intent();
        intent.setAction(MusicPlayerConstants.BIND_ACTION);
        Bundle bundle = new Bundle();
        bundle.putInt(MusicPlayerConstants.KEY_PLAYER_COMMAND, MusicPlayerConstants.COMMAND_PLAYER_START);
        bundle.putParcelable(MusicPlayerConstants.KEY_ITEM, item);
        intent.putExtras(bundle);

        context.startService(intent);
    }

    public static void play(Context context) {
        printTime("play");

        Intent intent = new Intent();
        intent.setAction(MusicPlayerConstants.BIND_ACTION);
        Bundle bundle = new Bundle();
        bundle.putInt(MusicPlayerConstants.KEY_PLAYER_COMMAND, MusicPlayerConstants.COMMAND_PLAYER_START);
        intent.putExtras(bundle);

        context.startService(intent);
    }

    public static void stop(Context context) {
        printTime("stop");

        Intent intent = new Intent();
        intent.setAction(MusicPlayerConstants.BIND_ACTION);
        Bundle bundle = new Bundle();
        bundle.putInt(MusicPlayerConstants.KEY_PLAYER_COMMAND, MusicPlayerConstants.COMMAND_PLAYER_STOP);
        intent.putExtras(bundle);

        context.startService(intent);
    }

    public static void pause(Context context) {
        printTime("pause");

        Intent intent = new Intent();
        intent.setAction(MusicPlayerConstants.BIND_ACTION);
        Bundle bundle = new Bundle();
        bundle.putInt(MusicPlayerConstants.KEY_PLAYER_COMMAND, MusicPlayerConstants.COMMAND_PLAYER_PAUSE);
        intent.putExtras(bundle);

        context.startService(intent);
    }

    public static void playSeekTo(Context context, int position) {
        printTime("playSeekTo");

        Intent intent = new Intent();
        intent.setAction(MusicPlayerConstants.BIND_ACTION);
        Bundle bundle = new Bundle();
        bundle.putInt(MusicPlayerConstants.KEY_PLAYER_COMMAND, MusicPlayerConstants.COMMAND_PLAYER_SEEK);
        bundle.putInt(MusicPlayerConstants.KEY_POSOTION, position);
        intent.putExtras(bundle);

        context.startService(intent);
    }

    /**
     * 添加一个item到播放列表
     *
     * @param context
     * @param item
     */
    public static void appendItem(Context context, MusicPlayerItem item) {
        printTime("appendItem");

        Intent intent = new Intent();
        intent.setAction(MusicPlayerConstants.BIND_ACTION);
        Bundle bundle = new Bundle();
        bundle.putInt(MusicPlayerConstants.KEY_PLAYER_COMMAND, MusicPlayerConstants.COMMAND_APPEND_ITEM);
        bundle.putParcelable(MusicPlayerConstants.KEY_ITEM, item);
        intent.putExtras(bundle);

        context.startService(intent);
    }

    public static void appendList(Context context, ArrayList<MusicPlayerItem> list) {
        if (null == list || 0 == list.size()) {
            return;
        }

        printTime("appendList");

        Intent intent = new Intent();
        intent.setAction(MusicPlayerConstants.BIND_ACTION);
        Bundle bundle = new Bundle();
        bundle.putInt(MusicPlayerConstants.KEY_PLAYER_COMMAND, MusicPlayerConstants.COMMAND_APPEND_LIST);
        bundle.putParcelableArrayList(MusicPlayerConstants.KEY_LIST, list);
        intent.putExtras(bundle);

        context.startService(intent);
    }

    public static void appendList2(Context context, ArrayList<MusicPlayerItem> list) {
        if (null == list || 0 == list.size()) {
            return;
        }

        printTime("appendList");

        Intent intent = new Intent();
        intent.setAction(MusicPlayerConstants.BIND_ACTION);
        Bundle bundle = new Bundle();
        bundle.putInt(MusicPlayerConstants.KEY_PLAYER_COMMAND, MusicPlayerConstants.COMMAND_APPEND_LIST);
        bundle.putParcelableArrayList(MusicPlayerConstants.KEY_LIST, (ArrayList<? extends android.os.Parcelable>) list);
        intent.putExtras(bundle);

        context.startService(intent);
    }

    /**
     * 逐一更新当前播放列表
     *
     * @param context
     * @param list
     */
    public static void updateList(Context context, ArrayList<MusicPlayerItem> list) {
        if (null == list || 0 == list.size()) {
            return;
        }

        printTime("appendList");

        Intent intent = new Intent();
        intent.setAction(MusicPlayerConstants.BIND_ACTION);
        Bundle bundle = new Bundle();
        bundle.putInt(MusicPlayerConstants.KEY_PLAYER_COMMAND, MusicPlayerConstants.COMMAND_UPDATE_LIST);
        bundle.putParcelableArrayList(MusicPlayerConstants.KEY_LIST, list);
        intent.putExtras(bundle);

        context.startService(intent);
    }

    private static MusicPlayerItem fileToPlayerItem(Context context, File file) {
        if (!file.getName().endsWith("mp3")) {
            return null;
        }

        MusicPlayerItem item = new MusicPlayerItem();
        item.setDescription(file.getName());
        item.setUrl("file://" + file.getAbsolutePath());

        item.setName(file.getName());
        item.setFileType(MusicPlayerConstants.TYPE_LOCAL);

        return item;
    }

    /**
     * 添加一个item到播放列表
     *
     * @param context
     * @param file
     */
    public static void appendItem(Context context, File file) {
        if (null == file) {
            return;
        }

        if (!file.getName().endsWith("mp3")) {
            return;
        }

        MusicPlayerItem item = new MusicPlayerItem();
        item.setDescription(file.getName());
        item.setUrl("file://" + file.getAbsolutePath());
        item.setName(file.getName());
        item.setFileType(MusicPlayerConstants.TYPE_LOCAL);

        Intent intent = new Intent();
        intent.setAction(MusicPlayerConstants.BIND_ACTION);
        Bundle bundle = new Bundle();
        bundle.putInt(MusicPlayerConstants.KEY_PLAYER_COMMAND, MusicPlayerConstants.COMMAND_APPEND_ITEM);
        bundle.putParcelable(MusicPlayerConstants.KEY_ITEM, item);
        intent.putExtras(bundle);

        context.startService(intent);
    }

    /**
     * 从播放列表删除一个item
     *
     * @param context
     * @param item
     */
    public static void deleteItem(Context context, MusicPlayerItem item) {
        printTime("deleteItem");

        Intent intent = new Intent();
        intent.setAction(MusicPlayerConstants.BIND_ACTION);
        Bundle bundle = new Bundle();
        bundle.putInt(MusicPlayerConstants.KEY_PLAYER_COMMAND, MusicPlayerConstants.COMMAND_DELETE_ITEM);
        bundle.putParcelable(MusicPlayerConstants.KEY_ITEM, item);
        intent.putExtras(bundle);

        context.startService(intent);
    }

    /**
     * 清空播放列表
     *
     * @param context
     */
    public static void clearPlayList(Context context) {
        printTime("clearPlayList");

        Intent intent = new Intent();
        intent.setAction(MusicPlayerConstants.BIND_ACTION);
        Bundle bundle = new Bundle();
        bundle.putInt(MusicPlayerConstants.KEY_PLAYER_COMMAND, MusicPlayerConstants.COMMAND_CLEAR_PLAYLIST);
        intent.putExtras(bundle);

        context.startService(intent);
    }

    public static void playNext(Context context) {
        printTime("playNext");

        Intent intent = new Intent();
        intent.setAction(MusicPlayerConstants.BIND_ACTION);
        Bundle bundle = new Bundle();
        bundle.putInt(MusicPlayerConstants.KEY_PLAYER_COMMAND, MusicPlayerConstants.COMMAND_PLAYER_NEXT);
        intent.putExtras(bundle);

        context.startService(intent);
    }

    public static void playPrev(Context context) {
        printTime("playPrev");

        Intent intent = new Intent();
        intent.setAction(MusicPlayerConstants.BIND_ACTION);
        Bundle bundle = new Bundle();
        bundle.putInt(MusicPlayerConstants.KEY_PLAYER_COMMAND, MusicPlayerConstants.COMMAND_PLAYER_PREV);
        intent.putExtras(bundle);

        context.startService(intent);
    }

    public static void stopService(Context context) {
        printTime("stopService");

        Intent intent = new Intent();
        intent.setAction(MusicPlayerConstants.BIND_ACTION);

        context.stopService(intent);
    }

    /**
     * 关闭Service,但不保留播放状态
     *
     * @param context
     */
    public static void stopServiceWithouSave(Context context) {
        Intent intent = new Intent();
        intent.setAction(MusicPlayerConstants.BIND_ACTION);
        Bundle bundle = new Bundle();
        bundle.putInt(MusicPlayerConstants.KEY_PLAYER_COMMAND,
                MusicPlayerConstants.COMMAND_STOP_SERVICE_WITHOUT_SAVE);
        intent.putExtras(bundle);

        context.startService(intent);
    }

    private static final boolean controlled = true;
    public static void requireNotificationController(Context context) {
        if (controlled) {
            return;
        }

        Utils.i(TAG, "requireNotificationController");
        Intent intent = new Intent();
        intent.setAction(MusicPlayerConstants.BIND_ACTION);
        Bundle bundle = new Bundle();
        bundle.putInt(MusicPlayerConstants.KEY_PLAYER_COMMAND,
                MusicPlayerConstants.COMMAND_REQUIRE_NOTIFICATION_CONTROLLER);
        intent.putExtras(bundle);

        context.startService(intent);
    }

    public static void returnNotificationController(Context context) {
        if (controlled) {
            return;
        }

        Utils.i(TAG, "returnNotificationController");
        Intent intent = new Intent();
        intent.setAction(MusicPlayerConstants.BIND_ACTION);
        Bundle bundle = new Bundle();
        bundle.putInt(MusicPlayerConstants.KEY_PLAYER_COMMAND,
                MusicPlayerConstants.COMMAND_RETURN_NOTIFICATION_CONTROLLER);
        intent.putExtras(bundle);

        context.startService(intent);
    }

    public static void initNotification(MusicNotificationClassProvider provider) {
        NotificationUtil.setNotificationClassProvider(provider);
    }

    public static void showNotification(Context context) {
        Intent intent = new Intent();
        intent.setAction(MusicPlayerConstants.BIND_ACTION);
        Bundle bundle = new Bundle();
        bundle.putInt(MusicPlayerConstants.KEY_PLAYER_COMMAND, MusicPlayerConstants.COMMAND_SHOW_NOTIFICATION);
        intent.putExtras(bundle);

        context.startService(intent);
    }

    public static void dismissNotification(Context context) {
        Intent intent = new Intent();
        intent.setAction(MusicPlayerConstants.BIND_ACTION);
        Bundle bundle = new Bundle();
        bundle.putInt(MusicPlayerConstants.KEY_PLAYER_COMMAND,
                MusicPlayerConstants.COMMAND_DISMISS_NOTIFICATION);
        intent.putExtras(bundle);

        context.startService(intent);
    }

    /**
     * 跳到某个item播放
     *
     * @param context
     * @param item
     */
    public static void jumpToItem(Context context, MusicPlayerItem item) {
        Intent intent = new Intent();
        intent.setAction(MusicPlayerConstants.BIND_ACTION);
        Bundle bundle = new Bundle();
        bundle.putInt(MusicPlayerConstants.KEY_PLAYER_COMMAND, MusicPlayerConstants.COMMAND_JUMP_TO_ITEM);
        bundle.putParcelable(MusicPlayerConstants.KEY_ITEM, item);
        intent.putExtras(bundle);

        context.startService(intent);
    }

    /**
     * 更新某个item，但不播放
     *
     * @param context
     * @param item
     */
    public static void updateToItem(Context context, MusicPlayerItem item) {
        Intent intent = new Intent();
        intent.setAction(MusicPlayerConstants.BIND_ACTION);
        Bundle bundle = new Bundle();
        bundle.putInt(MusicPlayerConstants.KEY_PLAYER_COMMAND, MusicPlayerConstants.COMMAND_UPDATE_TO_ITEM);
        bundle.putParcelable(MusicPlayerConstants.KEY_ITEM, item);
        intent.putExtras(bundle);

        context.startService(intent);
    }

    public static void jumpToItem(Context context, String url) {
        if (TextUtils.isEmpty(url)) {
            return;
        }

        Uri uri = Uri.parse(url);

        MusicPlayerItem item = new MusicPlayerItem();
        item.setUrl(uri.toString());
        item.setDescription(uri.toString());
        item.setName(uri.getPath());

        if (uri.getScheme().equalsIgnoreCase("file")) {
            item.setFileType(MusicPlayerConstants.TYPE_LOCAL);
        } else if (uri.getScheme().equalsIgnoreCase("http")) {
            item.setFileType(MusicPlayerConstants.TYPE_URL);
        }

        jumpToItem(context, item);
    }

    /**
     * 跳到某个item播放
     *
     * @param context
     * @param index
     */
    public static void jumpToItem(Context context, int index) {
        Intent intent = new Intent();
        intent.setAction(MusicPlayerConstants.BIND_ACTION);
        Bundle bundle = new Bundle();
        bundle.putInt(MusicPlayerConstants.KEY_PLAYER_COMMAND, MusicPlayerConstants.COMMAND_JUMP_TO_INDEX);
        bundle.putInt(MusicPlayerConstants.KEY_INDEX, index);
        intent.putExtras(bundle);

        context.startService(intent);
    }

    /**
     * 更新播放进度到item
     *
     * @param context
     * @param item
     */
    public static void updatePlayingPostionToItem(Context context, MusicPlayerItem item) {
        Intent intent = new Intent();
        intent.setAction(MusicPlayerConstants.BIND_ACTION);
        Bundle bundle = new Bundle();
        bundle.putInt(MusicPlayerConstants.KEY_PLAYER_COMMAND,
                MusicPlayerConstants.COMMAND_UPDATE_POSITION_TO_ITEM);
        bundle.putParcelable(MusicPlayerConstants.KEY_ITEM, item);
        intent.putExtras(bundle);

        context.startService(intent);
    }

    /**
     * 更新当前的book到Service
     *
     * @param context
     * @param musicAlbumObject
     */
    public static void updateMusicListObject(Context context, MusicAlbumObject musicAlbumObject) {
        Intent intent = new Intent();
        intent.setAction(MusicPlayerConstants.BIND_ACTION);
        Bundle bundle = new Bundle();
        bundle.putInt(MusicPlayerConstants.KEY_PLAYER_COMMAND, MusicPlayerConstants.COMMAND_UPDATE_BOOK);
        bundle.putSerializable(MusicPlayerConstants.KEY_BOOK, musicAlbumObject);
        intent.putExtras(bundle);

        context.startService(intent);
    }

    public static void updateStartPlayerSource(Context context, int startSourceFrom) {
        Intent intent = new Intent();
        intent.setAction(MusicPlayerConstants.BIND_ACTION);
        Bundle bundle = new Bundle();
        bundle.putInt(MusicPlayerConstants.KEY_PLAYER_COMMAND,
                MusicPlayerConstants.COMMAND_UPDATE_START_PLAYER_SOURCE);
        bundle.putInt(MusicPlayerConstants.KEY_START_SOURCE_FROM, startSourceFrom);

        intent.putExtras(bundle);

        context.startService(intent);
    }

    /**
     * Check whether the network is available.
     *
     * @param context
     * @return true if the network is available.
     */
    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivity = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivity == null) {
            return false;
        } else {
            NetworkInfo[] info = connectivity.getAllNetworkInfo();
            if (info != null) {
                for (int i = 0; i < info.length; i++) {
                    if (info[i].isConnected()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public static boolean isWifiActive(Context icontext) {
        Context context = icontext.getApplicationContext();
        ConnectivityManager connectivity = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo[] info;
        if (connectivity != null) {
            info = connectivity.getAllNetworkInfo();
            if (info != null) {
                for (int i = 0; i < info.length; i++) {
                    if ((info[i].getTypeName().equalsIgnoreCase("WIFI") || info[i].getTypeName()
                            .equalsIgnoreCase("WI FI")) && info[i].isConnected()) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    public static boolean isFileExist(MusicPlayerItem item) {
        if (null == item) {
            return false;
        }

        if (item.isLocalFileType()) {
            String path = item.getUrl();
            Uri uri = Uri.parse(path);
            path = uri.getPath();
            path = path.replace("file://", "");
            File file = new File(path);
            if (file.exists()) {
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    public static MusicAlbumObject getBookFromBundle(Bundle bundle) {
        MusicAlbumObject musicList = null;
        if (null != bundle) {
            musicList = (MusicAlbumObject) bundle.getSerializable(MusicPlayerConstants.KEY_BOOK);
        }

        return musicList;
    }

    public static int getStartTingshuSourceFromBundle(Bundle bundle) {
        int startSourceFrom = -1;
        if (null != bundle) {
            startSourceFrom = bundle.getInt(MusicPlayerConstants.KEY_START_SOURCE_FROM);
        }

        return startSourceFrom;
    }


    public static int getFileType(String uri) {
        if (TextUtils.isEmpty(uri)) {
            return MusicPlayerConstants.TYPE_URL;
        }

        if (uri.startsWith("http")) {
            return MusicPlayerConstants.TYPE_URL;
        } else {
            return MusicPlayerConstants.TYPE_LOCAL;
        }
    }

    public static String getFileSizeReadableString(long fileSize) {
        float sizeK = (float) fileSize / 1024;

        // float sizeK = (float) fileSize;
        String string = "";
        if (sizeK < 1024) {
            string = floatAccurate(sizeK, 2) + "K";

            return string;
        }

        float sizeM = sizeK / 1024;
        if (sizeM < 1024) {
            string = floatAccurate(sizeM, 2) + "M";
            return string;
        }

        float sizeG = sizeM / 1024;
        string = floatAccurate(sizeG, 2) + "G";

        return string;
    }

    public static String floatAccurate(float value, int accurate) {
        String string = "" + value;
        int dotIndex = string.indexOf(".");
        int allLength = string.length();
        int end = dotIndex + accurate + 1;
        if (end > allLength) {
            end = allLength;
        }

        string = string.substring(0, end);

        return string;
    }

    public static String getTimeReadable(int durationSecond) {
        int ss = durationSecond;

        // get hour
        int hour = durationSecond / 3600;
        String hourString = "";
        if (hour > 0) {
            ss = durationSecond % 3600;

            if (hour < 10) {
                hourString = "0" + hour;
            } else {
                hourString = "" + hour;
            }
        }

        int minute = ss / 60;
        int second = ss % 60;
        String minuteString = "";
        String sencodString = "";

        if (minute < 10) {
            minuteString = "0" + minute;
        } else {
            minuteString = "" + minute;
        }
        if (second < 10) {
            sencodString = "0" + second;
        } else {
            sencodString = "" + second;
        }

        String readableTime = minuteString + ":" + sencodString;
        if (hour > 0) {
            readableTime = hourString + ":" + readableTime;
        }

        return readableTime;
    }

    /**
     * 获得如 1小时20分钟
     *
     * @param context
     * @param durationSecond
     * @return
     */
    public static String getTimeReadableString(Context context, long durationSecond) {
        long ss = durationSecond;

        // get hour
        long hour = durationSecond / 3600;
        String hourString = "";
        if (hour > 0) {
            ss = durationSecond % 3600;

            if (hour < 10) {
                hourString = "0" + hour;
            } else {
                hourString = "" + hour;
            }
        }

        long minute = ss / 60;
        long second = ss % 60;
        String minuteString = "";
        String sencodString = "";

        if (minute < 10) {
            minuteString = "0" + minute;
        } else {
            minuteString = "" + minute;
        }
        if (second < 10) {
            sencodString = "0" + second;
        } else {
            sencodString = "" + second;
        }

        // String readableTime = minuteString + ":" + sencodString;
        // if (hour > 0) {
        // readableTime = hourString + ":" + readableTime;
        // }

        String readableTime = "";
        String hString = context.getString(R.string.hour);
        String mString = context.getString(R.string.minute);
        String sString = context.getString(R.string.second);
        if (hour > 0) {
            readableTime = hourString + hString;
        }

        if (minute > 0) {
            readableTime = readableTime + minuteString + mString;
        }

        if (second > 0) {
            readableTime = readableTime + sencodString + sString;
        }

        return readableTime;
    }

    public static void printTime(String message) {
        long currentTime = System.currentTimeMillis();
        Utils.i(TAG, message + ":" + currentTime + " ms");
    }

    private static final SimpleDateFormat sDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    /**
     * 转换2012-08-15格式
     *
     * @param time
     * @return
     */
    private static long dateConvert(String time) {
        if (TextUtils.isEmpty(time)) {
            return 0;
        }

        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try {
            return format.parse(time).getTime();
        } catch (ParseException e) {
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * 转换2013032015格式
     *
     * @param time
     * @return
     */
    private static long dateConvertForYuncheng(String time) {
        if (TextUtils.isEmpty(time)) {
            return 0;
        }

        SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHH");
        try {
            return format.parse(time).getTime();
        } catch (ParseException e) {
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * 判断当前时间是否处于有效时间内
     *
     * @param start
     * @param end
     * @param nowTime
     * @return
     */
    public static boolean isTimeValid(String start, String end, long nowTime) {

        try {
            if (TextUtils.isEmpty(start) || TextUtils.isEmpty(end)) {
                return false;
            }

            long startTime = dateConvert(start);
            long endTime = dateConvert(end);

            if (nowTime >= startTime && nowTime <= endTime) {
                return true;
            } else {
                return false;
            }

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean isTimeValidForYuncheng(String end, long nowTime) {

        try {
            if (TextUtils.isEmpty(end)) {
                return false;
            }

            long endTime = dateConvertForYuncheng(end);

            if (nowTime <= endTime) {
                return true;
            } else {
                return false;
            }

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static void download(String rpidBookid, int chapterId, String url, long fileSize) {

    }
}
