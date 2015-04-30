package me.pjq.musicplayer;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Process;
import android.os.RemoteException;
import android.os.Vibrator;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Vector;

import me.pjq.musicplayer.utils.NotificationUtil;
import me.pjq.musicplayer.utils.PlayerUtils;
import me.pjq.musicplayer.utils.StatUtil;
import me.pjq.musicplayer.utils.Utils;


/**
 * 播放器主类,播放器跑在Service中,通过Intent,AIDL接口和UI进行数据交互. <br>
 * 播放器状态:
 * <li>{@link MusicPlayerConstants#STATUS_INVALID}</li>
 * <li>{@link MusicPlayerConstants#STATUS_INITIALIZED}</li>
 * <li>{@link MusicPlayerConstants#STATUS_IDLE}</li>
 * <li>{@link MusicPlayerConstants#STATUS_PREPARED}</li>
 * <li>{@link MusicPlayerConstants#STATUS_STARTED}</li>
 * <li>{@link MusicPlayerConstants#STATUS_STOPPED}</li>
 * <li>{@link MusicPlayerConstants#STATUS_PAUSED}</li>
 * <li>{@link MusicPlayerConstants#STATUS_PLACKBACK_COMPLETED}</li>
 * <li>{@link MusicPlayerConstants#STATUS_ERROR}</li>
 * </br></br>
 * <p/>
 * UI与播放器进行通信的命令,定义在 {@link MusicPlayerConstants}中以COMMAND_XXXX开头
 * </br></br>
 * CallBack处理全部统一放在{@link MusicPlayerListeners}中处理，当Service bind成功之后，UI调用{@link #mServiceBinder}与Service进行数据交互<br>
 * 所有实际上这里有两套方式能控制播放器，一个是通过Inten方式发送对应COMMAND_XXXX(可以参见{@link me.pjq.musicplayer.utils.PlayerUtils}中的对应实现,或者查看{@link #onStartCommand(android.content.Intent, int, int)}),一个是通过{@link #mServiceBinder}来进行同步控制,<b>这里有一个问题，就是同步控制容易发生ANR，
 * 后面可以考虑使用Intent的方式，把播放器放在同一进程中实现</b>
 * </br></br>
 * <p/>
 * 播放器有对各种异常处理，包括:
 * <li>来电中断{@link #mPhoneStateListener},
 * <li>屏幕翻转{@link #shakeEventManager},
 * <li>网络状态切换{@link #mNetWorkChangeReceiver},
 * <li>插拨耳机{@link #mAudioBecomingNoisyReceiver},
 * <li>其它播放器播放请求{@link #mAudioFocusHelper}
 * <p/>
 * </br></br>
 * 播放器Notification控制,由于涉及到后台和前台播放，在前台播放时，不需要在Titlebar显示Notification，
 * 所以在这里我实现的一种类似于引用计数的模式{@link #mNotificationControllerReferenceCount}，当前台UI需要播放时，
 * 通过{@link me.pjq.musicplayer.utils.PlayerUtils#requireNotificationController(android.content.Context)}来获得控制权限，
 * 当UI不需要时，调用{@link me.pjq.musicplayer.utils.PlayerUtils#returnNotificationController(android.content.Context)}归还控制权限，
 * 当计数为0时，就可以认为Notification控制权归Service所有.
 * <p/>
 * </br></br>
 * 播放器播放进度刷新见{@link #mUpdatePlayingProgressHandler}
 *
 * @author pengjianqing
 * @date 20121029
 * @since 2.5.2
 */
public class MusicPlayerService extends Service implements
        MediaPlayer.OnBufferingUpdateListener, MediaPlayer.OnErrorListener,
        MediaPlayer.OnPreparedListener, MediaPlayer.OnCompletionListener,
        MediaPlayer.OnInfoListener, MediaPlayer.OnSeekCompleteListener,
        MediaPlayer.OnVideoSizeChangedListener {
    private static String TAG = MusicPlayerService.class.getSimpleName();

    private static final boolean DEBUG_LOG = true;

    private MediaPlayer mCloudaryMediaPlayer;

    private Context mContext;

    private Vibrator vibrator;
    private ShakeEventManager shakeEventManager;
    private StepCountEventManager stepCountEventManager;
    private int stepInitCount = -1;
    private int stepCount = -1;
    private int stepPrevCount = -1;

    /**
     * 保存播放器prepare状态
     */
    private int mMediaPlayerPreparingStatus = MusicPlayerConstants.PREPARING_STATUS_PRE;

    /**
     * 保存播放器播放状态
     */
    private int mMediaPlayerPlayingStatus = MusicPlayerConstants.STATUS_INVALID;

    private Vector<MusicPlayerItem> mPlayList;

    private MusicPlayerItem mCurrentPlayingItem;

    private int mCurrentPlayingIndex = 0;

    /**
     * 当拨出耳机时需要停止播放
     */
    private BroadcastReceiver mAudioBecomingNoisyReceiver;

    private BroadcastReceiver mNetWorkChangeReceiver;

    /**
     * 用来保存Notification引用计数,当Activity发送require请求时，Service放弃Notification权利.
     * 由Activity控制是否显示Notification.
     */
    private int mNotificationControllerReferenceCount = 0;


    private MusicPlayerListeners mPlayerListener;

    private MusicPlayerConfig mMusicPlayerConfig;

    // 用来计算自动播放帐节数目,达到数量需要暂停播放
    private int mAutoPlayChapterCount = 0;

    // 用来计算自动播放时长,根据设备达到时长需要暂停播放
    private int mAutoPlayTime = 0;

    private MusicAlbumObject musicAlbumObject;

    private int mStartPlayerSourceFrom = 0;

    private int mCommand;

    /**
     * 标示是否在seek状态，如果是在seek状态，就不需要更新播放进度，有时会出现来回跳转的情况
     */
    private boolean mInSeekMode = false;

    /**
     * 记录跳转位置
     */
    private int mSeekPosition = 0;

    /**
     * 记录跳转之前的位置
     */
    private int mBeforeSeekPostion = 0;

    /**
     * 记录跳转时间，如果跳转时间和刷新播放进度时间相差1S,并且跳转的值和进度相关太大，可以认为当前刷新进度无效，用来防止跳转播放进度前后抖动
     */
    private long mSeekTimestamp = 0;

    private int mPreviewPlayingProgress = 0;

    private Handler mUpdatePlayingProgressHandler = new Handler() {
        public void dispatchMessage(android.os.Message msg) {

            int what = msg.what;

            if (MusicPlayerConstants.MESSAGE_UPDATE_PLAYING_PROGRESS == what) {
                keepAutoPlayStaus(mAutoPlayChapterCount, mAutoPlayTime
                        + MusicPlayerConstants.PROGRESS_REFRESH_INTERVAL_SECOND);

                int autoStopTime = getPlayerConfig().getAutoStopTime();
                if (autoStopTime > 0) {
                    if (mAutoPlayTime >= autoStopTime) {
                        pausePlayer();
                        log("mAutoPlayTime=" + mAutoPlayTime + ",goto sleep mode,pausePlayer");

                        keepAutoPlayStaus(0, 0);
                        return;
                    }
                }

                if (null != mPlayerListener) {
                    if (null != mCloudaryMediaPlayer) {
                        int position = getCurrentPositionImpl();
                        keepCurrentPosition(position);
                        // log("playing position=" + position);

                        // 如果当前位置小于1S，则无须刷新界面，目前遇到在线播放的时候，得到的值小于1000ms,导致进度跳转
                        if (position < MusicPlayerConstants.PROGRESS_REFRESH_INTERVAL_SECOND) {

                        } else {
                            // 如果是在跳转模式下，就不需要更新当前播放进度
                            if (mInSeekMode) {

                            } else {

                                if (!checkNeedUpdatePlayingProgress(position)) {

                                } else {
                                    mPlayerListener.onUpdatePlayingProgress(
                                            getCurrentPlayingItemImpl(), mCurrentPlayingIndex,
                                            position);
                                }
                            }

                        }

                        if (position >= mCurrentPlayingItem.getTime()) {
                            // 播放完成时调用onComplete
                            stopPlayer();
                            onCompletion(mCloudaryMediaPlayer);

                        }

                        mPreviewPlayingProgress = position;
                    }
                }

            } else if (MusicPlayerConstants.MESSAGE_UPDATE_STEP_FREQUENCY == what) {
                float freq = stepCount - stepPrevCount;
                freq = freq / ((float) MusicPlayerConstants.STEP_FREQUENCY_REFRESH_INTERVAL / (float) 60000);

                mPlayerListener.onUpdateStepFreq(freq);

                stepPrevCount = stepCount;
            }
        }

        ;

        /**
         * 检查是否需要更新播放进度
         *
         * @param currentPosition
         * @return true if need
         */
        private boolean checkNeedUpdatePlayingProgress(int currentPosition) {
            long current = System.currentTimeMillis();
            long duration = current - mSeekTimestamp;

            log("duration=" + duration);
            // 防止播放进度抖动处理
            if (duration > 0 && duration < MusicPlayerConstants.PROGRESS_SHAKE_DURATION_MS) {
                int skipDuration = Math.abs(currentPosition - mSeekPosition);

                log("skipDuration=" + skipDuration);
                if (skipDuration > MusicPlayerConstants.PROGRESS_SKIP_DURATION) {
                    return false;
                }
            }

            // 如果前后进度一样，可以不需要进行刷新
            log("mPreviewPlayingProgress=" + mPreviewPlayingProgress + ",position="
                    + currentPosition);
            if (mPreviewPlayingProgress != 0 && mPreviewPlayingProgress == currentPosition) {
                return false;
            }

            return true;
        }
    };

    private boolean mPlayingProgressUpdateThreadFlag = true;

    /**
     * 用来刷新播放进度，只要Service启动就一直处于状态
     */
    private Thread mUpdatePlayingProgressThread = new Thread(new Runnable() {

        @Override
        public void run() {
            while (mPlayingProgressUpdateThreadFlag) {
                // 先要用isPlayerPreparingImpl判断是否准备完毕，否则会报exception
                if (null != mCloudaryMediaPlayer && isPlayerPreparedImpl() && isPlaying()) {
                    mUpdatePlayingProgressHandler
                            .sendEmptyMessage(MusicPlayerConstants.MESSAGE_UPDATE_PLAYING_PROGRESS);
                }

                try {
                    Thread.sleep(MusicPlayerConstants.PROGRESS_REFRESH_INTERVAL);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    });

    private boolean mStepFrequencyUpdateThreadFlag = true;

    /**
     * 用来刷新播放进度，只要Service启动就一直处于状态
     */
    private Thread mStepFrequencyUpdateThread = new Thread(new Runnable() {

        @Override
        public void run() {
            while (mStepFrequencyUpdateThreadFlag) {
                mUpdatePlayingProgressHandler
                        .sendEmptyMessage(MusicPlayerConstants.MESSAGE_UPDATE_STEP_FREQUENCY);
                try {
                    Thread.sleep(MusicPlayerConstants.STEP_FREQUENCY_REFRESH_INTERVAL);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    });

    IMusicPlayerService.Stub mServiceBinder = new IMusicPlayerService.Stub() {
        @Override
        public boolean setPlayerListener(IMusicPlayerListener listener) {
            return mPlayerListener.setPlayerListener(listener);
        }

        @Override
        public boolean unSetPlayerListener(IMusicPlayerListener listener) {
            return mPlayerListener.unSetPlayerListener(listener);
        }

        @Override
        public boolean playNextItem() {
            return playNext();
        }

        @Override
        public boolean playPrevItem() {
            return playPrev();
        }

        @Override
        public Vector<MusicPlayerItem> getPlayerList() {
            return mPlayList;
        }

        @Override
        public MusicPlayerItem getCurrentPlayingItem() {
            return getCurrentPlayingItemImpl();
        }

        @Override
        public int getCurrentPlayingIndex() {
            return mCurrentPlayingIndex;
        }

        @Override
        public int getCurrentPlayingTotalTime() {
            int total = getCurrentPlayingTotalTimeImpl();
            // log("getCurrentPlayingTotalTime,total=" + total);
            return total;
        }

        @Override
        public boolean isPlayerPrepared() {
            return isPlayerPreparedImpl();
        }

        @Override
        public int getPlayerPlayingStatus() {
            return mMediaPlayerPlayingStatus;
        }

        @Override
        public boolean start() {
            log("mServiceBinder,start()");
            return startPlayer();
        }

        @Override
        public boolean pause() {
            log("mServiceBinder,pausePlayer()");
            return pausePlayer();
        }

        @Override
        public boolean stop() {
            return stopPlayer();
        }

        @Override
        public boolean isPlaying() {
            if (null != mCloudaryMediaPlayer) {
                return MusicPlayerConstants.STATUS_STARTED == mMediaPlayerPlayingStatus;
            }

            return false;
        }

        @Override
        public boolean canForward() {
            return canForwardImpl();
        }

        @Override
        public boolean canGoBack() {
            return canGoBackImpl();
        }

        @Override
        public boolean isMediaPlayerStatusInvalid() throws RemoteException {
            return MusicPlayerConstants.STATUS_INVALID == mMediaPlayerPlayingStatus
                    || null == mCloudaryMediaPlayer;
        }

        @Override
        public void clearPlayList() throws RemoteException {
            clearPlayListImpl();
        }

        @Override
        public void seekTo(int position) throws RemoteException {
            seekToImpl(position);
        }

        @Override
        public int getPlayerListCount() throws RemoteException {
            return mPlayList.size();
        }

        @Override
        public void savePlayingStatus() throws RemoteException {
            MusicPlayerService.this.savePlayingStatus();
            // if (null != mPlayerListener) {
            // mPlayerListener.savePlayingStatus(mContext, mCurrentPlayingItem,
            // mBook,
            // getPlayerListCount(), mCurrentPlayingIndex);
            // }

        }
    };

    private PhoneStateListener mPhoneStateListener = new PhoneStateListener() {
        private boolean mConsumed = true;

        private void log(String tag, String msg) {
            Utils.i(tag, msg);
        }

        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            log(TAG, "onCallStateChanged,state=" + state + ",incomingNumber=" + incomingNumber);

            switch (state) {
                case TelephonyManager.CALL_STATE_IDLE:
                    log(TAG, "CALL_STATE_IDLE");
                    if (!mConsumed) {
                        startPlayer();
                        mConsumed = true;
                    }

                    break;

                case TelephonyManager.CALL_STATE_RINGING: {
                    log(TAG, "CALL_STATE_RINGING");
                    if (isPlaying()) {
                        mConsumed = false;
                        pausePlayer();
                    }

                    break;
                }

                default:
                    break;
            }

            super.onCallStateChanged(state, incomingNumber);
        }
    };

    private void log(String message) {
        if (!DEBUG_LOG) {
            return;
        }

        if (null != message) {
            Log.i(TAG, message);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();

        log("onCreate");

        mContext = getApplicationContext();

        mPlayList = new Vector<MusicPlayerItem>();
        mPlayerListener = new MusicPlayerListeners();

        mAudioBecomingNoisyReceiver = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {
                log("AUDIO_BECOMING_NOISY,pausePlayer()");
                pausePlayer();
            }
        };

        IntentFilter intentFilter = new IntentFilter("android.media.AUDIO_BECOMING_NOISY");
        registerReceiver(mAudioBecomingNoisyReceiver, intentFilter);

        // 当网络切换的时候，需要判断是否是非wifi,如果切到非wifi情况下需要停止播放
        mNetWorkChangeReceiver = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {
                // boolean isNetworkAvailable =
                // PlayerUtils.isNetworkAvailable(context);

                if (isNeedStopPlayerWhenNotWifi()) {
                    // pausePlayer();
                    log("isNeedStopPlayerWhenNotWifi,stopPlayer");

                    stopPlayer();
                }

            }
        };

        registerReceiver(mNetWorkChangeReceiver, new IntentFilter(
                ConnectivityManager.CONNECTIVITY_ACTION));

        TelephonyManager tm = (TelephonyManager) getSystemService(Service.TELEPHONY_SERVICE);
        tm.listen(mPhoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);

        mUpdatePlayingProgressThread.start();

        vibrator = (Vibrator) getSystemService(Service.VIBRATOR_SERVICE);
        shakeEventManager = new ShakeEventManager();
        shakeEventManager.setListener(new ShakeEventManager.ShakeListener() {
            @Override
            public void onShake() {
                boolean isShakeToNext = getPlayerConfig().isShakeToNext();
                if (isShakeToNext) {
                    playNext();
                }
            }

            @Override
            public void onFlipBack() {
                boolean isQuickStopEnable = getPlayerConfig().isQuickStop();
                if (isQuickStopEnable) {
                    log("back the phone,pausePlayer()");
                    pausePlayer();
                }
            }
        });

        shakeEventManager.init(this);

        stepCountEventManager = new StepCountEventManager();
        stepCountEventManager.setListener(new StepCountEventManager.StepCountListener() {
            @Override
            public void onStepCount(int count) {
                stepCount = count;
                if (-1 == stepPrevCount) {
                    stepPrevCount = count;
                }

                if (-1 == stepInitCount) {
                    stepInitCount = count;
                }

                int totalCount = count - stepInitCount;
                if (totalCount > 0) {
                    mPlayerListener.onUpdateStepCount(totalCount);
                }
            }

            @Override
            public void onNotSupportStepSensor() {

            }
        });

        stepCountEventManager.init(this);
        mStepFrequencyUpdateThread.start();
    }

    /**
     * Interface definition for a callback to be invoked when the audio focus of
     * the system is updated.
     */
    public interface OnAudioFocusChangeListener {
        /**
         * Called on the listener to notify it the audio focus for this listener
         * has been changed. The focusChange addEvent indicates whether the focus
         * was gained, whether the focus was lost, and whether that loss is
         * transient, or whether the new focus holder will hold it for an
         * unknown amount of time. When losing focus, listeners can use the
         * focus change information to decide what behavior to adopt when losing
         * focus. A music player could for instance elect to lower the volume of
         * its music stream (duck) for transient focus losses, and pause
         * otherwise.
         *
         * @param focusChange the type of focus change, one of
         *                    {@link android.media.AudioManager#AUDIOFOCUS_GAIN},
         *                    {@link android.media.AudioManager#AUDIOFOCUS_LOSS},
         *                    {@link android.media.AudioManager#AUDIOFOCUS_LOSS_TRANSIENT} and
         *                    {@link android.media.AudioManager#AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK}.
         */
        public void onAudioFocusChange(int focusChange);
    }

    OnAudioFocusChangeListener mOnAudioFocusChangeListener = new OnAudioFocusChangeListener() {

        @Override
        public void onAudioFocusChange(int focusChange) {
            switch (focusChange) {
                case AudioManager.AUDIOFOCUS_GAIN:
                    // resume playback
                    log("AUDIOFOCUS_GAIN,startPlayer()");
                    startPlayer();
                    if (null != mCloudaryMediaPlayer) {
                        mCloudaryMediaPlayer.setVolume(1.0f, 1.0f);
                    }

                    break;

                case AudioManager.AUDIOFOCUS_LOSS:
                    // Lost focus for an unbounded amount of time: stop playback
                    // and release media player
                    // stopPlayer();
                    log("AUDIOFOCUS_LOSS,pausePlayer()");
                    pausePlayer();

                    break;

                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                    // Lost focus for a short time, but we have to stop
                    // playback. We don't release the media player because
                    // playback
                    log("AUDIOFOCUS_LOSS_TRANSIENT,pausePlayer()");
                    pausePlayer();

                    break;

                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                    // Lost focus for a short time, but it's ok to keep playing
                    // at an attenuated level
                    // pausePlayer();
                    log("AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK");

                    if (null != mCloudaryMediaPlayer && isPlaying()) {
                        // mCloudaryMediaPlayer.setVolume(0.1f, 0.1f);
                    }

                    break;
            }
        }
    };

    AudioFocusHelper mAudioFocusHelper;

    /**
     * 开始播放时需要调用
     */
    private void requestAudioFocus() {
        if (android.os.Build.VERSION.SDK_INT < 8) {
            // if (null == mAudioFocusHelper) {
            // mAudioFocusHelper = new AudioFocusHelper(getApplicationContext(),
            // mOnAudioFocusChangeListener);
            // }
            //
            // mAudioFocusHelper.requestFocus();
            mAudioFocusHelper = null;

        } else {
            if (null == mAudioFocusHelper) {
                // try {
                // mAudioFocusHelper = (AudioFocusHelper)
                // Class.forName("AudioFocusHelper");
                // mAudioFocusHelper.requestFocus();
                // } catch (ClassNotFoundException e) {
                // e.printStackTrace();
                // }
                mAudioFocusHelper = new AudioFocusHelper(getApplicationContext(),
                        mOnAudioFocusChangeListener);
            }

            mAudioFocusHelper.requestFocus();

            // AudioManager audioManager = (AudioManager)
            // getSystemService(Context.AUDIO_SERVICE);
            // int result =
            // audioManager.requestAudioFocus(mOnAudioFocusChangeListener,
            // AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
            //
            // if (result != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            //
            // }
        }

    }

    /**
     * 暂停、停止播放时需要调用
     */
    private void removeAudioFocus() {
        if (android.os.Build.VERSION.SDK_INT < 8) {
            // if (null != mAudioFocusHelper) {
            // mAudioFocusHelper.abandonFocus();
            // }
        } else {
            // AudioManager audioManager = (AudioManager)
            // getSystemService(Context.AUDIO_SERVICE);
            // audioManager.abandonAudioFocus(mOnAudioFocusChangeListener);

            if (null != mAudioFocusHelper) {
                mAudioFocusHelper.abandonFocus();
            }
        }
    }

    // @Override
    // @Deprecated
    // public void onStart(Intent intent, int startId) {
    // super.onStart(intent, startId);
    //
    // log("onStart,intent=" + intent + ",startId=" + startId);
    // }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        log("onStartCommand,intent=" + intent + ",flags=" + flags + ",startId=" + startId);

        int command = getPlayerCommand(intent);
        mCommand = command;
        if (MusicPlayerConstants.COMMAND_PLAYER_INVALID != command) {
            switch (command) {
                case MusicPlayerConstants.COMMAND_PLAYER_PAUSE: {
                    log("onStartCommand,pausePlayer()");
                    PlayerUtils.printTime("COMMAND_PLAYER_PAUSE");
                    pausePlayer();

                    break;
                }

                case MusicPlayerConstants.COMMAND_PLAYER_START: {
                    PlayerUtils.printTime("COMMAND_PLAYER_START");
                    Bundle bundle = intent.getExtras();
                    if (null != bundle) {
                        // 如果有地址，表示开始播放新的资源
                        MusicPlayerItem item = (MusicPlayerItem) bundle
                                .getParcelable(MusicPlayerConstants.KEY_ITEM);
                        if (null != item) {
                            insertItemToPlayList(item);
                            playItem(item);
                        } else {
                            startPlayer();
                        }

                    } else {
                        startPlayer();
                    }

                    break;
                }

                case MusicPlayerConstants.COMMAND_PLAYER_STOP: {
                    PlayerUtils.printTime("COMMAND_PLAYER_STOP");
                    stopPlayer();

                    break;
                }

                case MusicPlayerConstants.COMMAND_PLAYER_SEEK: {
                    PlayerUtils.printTime("COMMAND_PLAYER_SEEK");
                    Bundle bundle = intent.getExtras();
                    if (null != bundle) {
                        int position = bundle.getInt(MusicPlayerConstants.KEY_POSOTION);
                        seekToImpl(position);
                    }

                    break;
                }

                case MusicPlayerConstants.COMMAND_DESTROY_SERVICE: {
                    PlayerUtils.printTime("COMMAND_DESTROY_SERVICE");
                    log("COMMAND_DESTROY_SERVICE");
                    stopPlayer();

                    stopSelf();
                    break;
                }

                case MusicPlayerConstants.COMMAND_STOP_SERVICE_WITHOUT_SAVE: {
                    PlayerUtils.printTime("COMMAND_STOP_SERVICE_WITHOUT_SAVE");
                    log("COMMAND_STOP_SERVICE_WITHOUT_SAVE");
                    stopPlayer();

                    stopSelf();
                    break;
                }

                case MusicPlayerConstants.COMMAND_PLAYER_NEXT: {
                    PlayerUtils.printTime("COMMAND_PLAYER_NEXT");
                    playNext();

                    break;
                }

                case MusicPlayerConstants.COMMAND_PLAYER_PREV: {
                    PlayerUtils.printTime("COMMAND_PLAYER_PREV");
                    playPrev();

                    break;
                }

                case MusicPlayerConstants.COMMAND_APPEND_ITEM: {
                    PlayerUtils.printTime("COMMAND_APPEND_ITEM");
                    Bundle bundle = intent.getExtras();
                    if (null != bundle) {
                        MusicPlayerItem item = (MusicPlayerItem) bundle
                                .getParcelable(MusicPlayerConstants.KEY_ITEM);
                        appendItemToPlayList(item);
                    }

                    break;
                }

                case MusicPlayerConstants.COMMAND_APPEND_LIST: {
                    PlayerUtils.printTime("COMMAND_APPEND_LIST");
                    Bundle bundle = intent.getExtras();
                    if (null != bundle) {
                        ArrayList<MusicPlayerItem> list = bundle
                                .getParcelableArrayList(MusicPlayerConstants.KEY_LIST);
                        appendListToPlayList(list);
                    }

                    break;
                }

                case MusicPlayerConstants.COMMAND_UPDATE_BOOK: {

                    Bundle bundle = intent.getExtras();
                    if (null != bundle) {
                        musicAlbumObject = (MusicAlbumObject) bundle.getSerializable(MusicPlayerConstants.KEY_BOOK);
                    }

                    PlayerUtils.printTime("COMMAND_UPDATE_BOOK,mBook=" + musicAlbumObject);

                    break;
                }

                case MusicPlayerConstants.COMMAND_UPDATE_AUTHED_LIST: {
                    PlayerUtils.printTime("COMMAND_UPDATE_AUTHED_LIST");
                    Bundle bundle = intent.getExtras();
                    if (null != bundle) {
                        ArrayList<MusicPlayerItem> list = bundle
                                .getParcelableArrayList(MusicPlayerConstants.KEY_LIST);
                        updateAuthedListToPlayList(list);
                    }

                    break;
                }

                case MusicPlayerConstants.COMMAND_UPDATE_AUTHED_LIST_AND_START_PLAYER: {
                    PlayerUtils.printTime("COMMAND_UPDATE_AUTHED_LIST_AND_START_PLAYER");
                    Bundle bundle = intent.getExtras();
                    if (null != bundle) {
                        ArrayList<MusicPlayerItem> list = bundle
                                .getParcelableArrayList(MusicPlayerConstants.KEY_LIST);
                        updateAuthedListToPlayList(list);

                        startPlayer();
                    }

                    break;
                }

                case MusicPlayerConstants.COMMAND_DELETE_ITEM: {
                    PlayerUtils.printTime("COMMAND_DELETE_ITEM");
                    Bundle bundle = intent.getExtras();
                    if (null != bundle) {
                        MusicPlayerItem item = (MusicPlayerItem) bundle
                                .getParcelable(MusicPlayerConstants.KEY_ITEM);
                        deleteItemFromPlayList(item);
                    }

                    break;
                }

                case MusicPlayerConstants.COMMAND_REQUIRE_NOTIFICATION_CONTROLLER: {
                    mNotificationControllerReferenceCount++;

                    showNotification(false);

                    break;
                }

                case MusicPlayerConstants.COMMAND_RETURN_NOTIFICATION_CONTROLLER: {
                    mNotificationControllerReferenceCount--;

                    // showNotification(false);

                    break;
                }

                case MusicPlayerConstants.COMMAND_SHOW_NOTIFICATION: {
                    showNotification(true);

                    break;
                }

                case MusicPlayerConstants.COMMAND_DISMISS_NOTIFICATION: {
                    dismissNotification();

                    break;

                }

                case MusicPlayerConstants.COMMAND_CLEAR_PLAYLIST: {
                    PlayerUtils.printTime("COMMAND_CLEAR_PLAYLIST");
                    clearPlayListImpl();

                    break;
                }

                case MusicPlayerConstants.COMMAND_JUMP_TO_ITEM: {
                    PlayerUtils.printTime("COMMAND_JUMP_TO_ITEM");
                    Bundle bundle = intent.getExtras();
                    if (null != bundle) {
                        MusicPlayerItem item = (MusicPlayerItem) bundle
                                .getParcelable(MusicPlayerConstants.KEY_ITEM);
                        jumpToItem(item);
                    }

                    break;
                }

                case MusicPlayerConstants.COMMAND_UPDATE_TO_ITEM: {
                    PlayerUtils.printTime("COMMAND_UPDATE_TO_ITEM");
                    Bundle bundle = intent.getExtras();
                    if (null != bundle) {
                        MusicPlayerItem item = (MusicPlayerItem) bundle
                                .getParcelable(MusicPlayerConstants.KEY_ITEM);
                        updateToItem(item);
                    }

                    break;
                }

                case MusicPlayerConstants.COMMAND_JUMP_TO_INDEX: {
                    PlayerUtils.printTime("COMMAND_JUMP_TO_INDEX");
                    Bundle bundle = intent.getExtras();
                    if (null != bundle) {
                        int index = bundle.getInt(MusicPlayerConstants.KEY_INDEX);
                        MusicPlayerItem item = getPlayerItem(index);
                        jumpToItem(item);
                    }

                    break;
                }

                case MusicPlayerConstants.COMMAND_UPDATE_POSITION_TO_ITEM: {
                    PlayerUtils.printTime("COMMAND_UPDATE_POSITION_TO_ITEM");
                    Bundle bundle = intent.getExtras();
                    if (null != bundle) {
                        MusicPlayerItem item = (MusicPlayerItem) bundle
                                .getParcelable(MusicPlayerConstants.KEY_ITEM);
                        updateItemPositionToPlayerList(item);
                    }

                    break;
                }

                case MusicPlayerConstants.COMMAND_AUTO_PLAYING_STATUS_INTERRUPTED: {
                    // 自动播放状态被打断，也用来判断全部播完状态，如果有跑到这里表示播放完成状态被打乱
                    log("COMMAND_AUTO_PLAYING_STATUS_INTERRUPTED");
                    isPlayerListComplete = false;
                    break;
                }

                case MusicPlayerConstants.COMMAND_UPDATE_START_PLAYER_SOURCE: {

                    Bundle bundle = intent.getExtras();
                    if (null != bundle) {
                        mStartPlayerSourceFrom = bundle
                                .getInt(MusicPlayerConstants.KEY_START_SOURCE_FROM);
                    }

                    PlayerUtils
                            .printTime("COMMAND_UPDATE_START_PLAYER_SOURCE,mStartPlayerSourceFrom="
                                    + mStartPlayerSourceFrom);

                    break;
                }

                default:
                    break;
            }
        }

        return super.onStartCommand(intent, flags, startId);
    }

    /**
     * 解析出command.
     *
     * @param intent
     * @return
     */
    private int getPlayerCommand(Intent intent) {
        if (null == intent) {
            return MusicPlayerConstants.COMMAND_PLAYER_INVALID;
        }

        Bundle extra = intent.getExtras();
        if (null == extra) {
            return MusicPlayerConstants.COMMAND_PLAYER_INVALID;
        }

        return extra.getInt(MusicPlayerConstants.KEY_PLAYER_COMMAND);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        log("onDestroy");

        if (MusicPlayerConstants.COMMAND_STOP_SERVICE_WITHOUT_SAVE == mCommand) {
            // 不保存状态,在logout时会用到
            log("onDestroy,COMMAND_STOP_SERVICE_WITHOUT_SAVE");

        } else {
            // 保存状态前，先保存播放进度
            if (isPlayerPreparedImpl()) {
                keepCurrentPosition(null);
                savePlayingStatus();
            }
        }
        removeAudioFocus();
        NotificationUtil.dismissNotification(mContext);

        keepCurrentPosition(null);
        setCurrentPlayerItem(null);

        releaseMediaPlayer();
        mPlayList.clear();
        mPlayList = null;
        mPlayerListener.clear();
        mPlayerListener = null;
        musicAlbumObject = null;

        mPlayingProgressUpdateThreadFlag = false;
        mStepFrequencyUpdateThreadFlag = false;
        // mUpdatePlayingProgressThread.interrupt();

        if (null != mAudioBecomingNoisyReceiver) {
            unregisterReceiver(mAudioBecomingNoisyReceiver);
            mAudioBecomingNoisyReceiver = null;
        }

        if (null != mNetWorkChangeReceiver) {
            unregisterReceiver(mNetWorkChangeReceiver);
            mNetWorkChangeReceiver = null;
        }

        shakeEventManager.deregister();
        stepCountEventManager.deregister();
        removeAudioFocus();

        TelephonyManager tm = (TelephonyManager) getSystemService(Service.TELEPHONY_SERVICE);
        tm.listen(mPhoneStateListener, PhoneStateListener.LISTEN_NONE);

        Process.killProcess(Process.myPid());
    }

    @Override
    public void onRebind(Intent intent) {
        super.onRebind(intent);

        log("onRebind");
    }

    @Override
    public boolean onUnbind(Intent intent) {
        log("onUnbind,intent=" + intent);

        return super.onUnbind(intent);
    }

    private boolean isNotificationControlledByService() {
        log("isNotificationControlledByService,mNotificationControllerReferenceCount="
                + mNotificationControllerReferenceCount);

        if (0 == mNotificationControllerReferenceCount) {
            return true;
        } else {
            return false;
        }
    }

    private void showNotification() {
        showNotification(false);
    }

    private void showNotification(boolean force) {
        log("showNotification,force=" + force + ",mNotificationControllerReferenceCount="
                + mNotificationControllerReferenceCount);

        if (!force) {
            if (!isNotificationControlledByService()) {
                NotificationUtil.dismissNotification(mContext);

                return;
            }
        }

        String prefixMessage = "";
        switch (mMediaPlayerPlayingStatus) {
            case MusicPlayerConstants.STATUS_STOPPED:
                prefixMessage = getString(R.string.it_is_stop_now) + " "
                        + getString(R.string.click_to_see_detail);
                break;

            case MusicPlayerConstants.STATUS_PAUSED:
                prefixMessage = getString(R.string.it_is_pause_now) + " "
                        + getString(R.string.click_to_see_detail);
                break;

            case MusicPlayerConstants.STATUS_STARTED:
                prefixMessage = getString(R.string.it_is_playing_now);
                break;

            case MusicPlayerConstants.STATUS_INITIALIZED:
                prefixMessage = getString(R.string.it_is_preparing_now);
                break;

            default:
                prefixMessage = getString(R.string.it_is_invalid_now) + " "
                        + getString(R.string.click_to_see_detail);
                break;
        }

        if (null != mCurrentPlayingItem) {
            String message = prefixMessage + " " + mCurrentPlayingItem.getName();
            if (null == musicAlbumObject) {
                // mBook =
                // PlayerUtils.queryBookInfoFromDatabase(mCurrentPlayingItem.getMusicAlbumId());
            }

            NotificationUtil.showNotification(getApplicationContext(), message, musicAlbumObject);
        } else {
            // String message = "The playlist is empty.";
            // NotificationUtil.showNotification(getApplicationContext(),
            // message, mBook);
        }
    }

    /**
     * 关闭Notification,但如果是Notification是由Service控制，則禁止關閉，否則會找不到入口.
     */
    private void dismissNotification() {
        if (isNotificationControlledByService()) {
            return;
        }

        NotificationUtil.dismissNotification(getApplicationContext());
    }

    @Override
    public IBinder onBind(Intent intent) {
        log("onBind,intent=" + intent);

        return mServiceBinder;
    }

    // @Override
    // public void onTaskRemoved(Intent rootIntent) {
    // super.onTaskRemoved(rootIntent);
    // }

    /**
     * 创建一个新的MediaPlayer
     *
     * @return CloudaryMediaPlayer
     */
    private MediaPlayer createMediaPlayer() {
        log("createMediaPlayer");
        MediaPlayer player = null;
        if (null != mCloudaryMediaPlayer) {
            // releaseMediaPlayer();
            player = mCloudaryMediaPlayer;

            // Issues refer
            // to:https://groups.google.com/forum/?fromgroups=#!topic/android-developers/GHmI0YRfEz4
            // after reset, then setDataSource maybe throw exception.
            // player.reset();
            releaseMediaPlayer();

            player = new MediaPlayer();

            player = setMediaPlayerListener(player);
            player.setScreenOnWhilePlaying(false);

        } else {
            player = new MediaPlayer();

            player = setMediaPlayerListener(player);
            player.setScreenOnWhilePlaying(false);
        }

        setPlayerPlayingStatus(MusicPlayerConstants.STATUS_IDLE);

        return player;
    }

    private MediaPlayer createMediaPlayerAsync(String url) {
        log("createMediaPlayerAsync");


        // 这种方法会阻塞UI线程
        // MediaPlayer player = MediaPlayer.create(getApplicationContext(),
        // Uri.parse(url));
        // url =
        // "http://upupyoyoyo.net/COFFdD0xMzYxNzg1OTU1Jmk9MTE0LjgwLjEzMy43JnU9U29uZ3MvdjIvZmFpbnRRQy80NC9lNS81YTNhY2Q4ZmE3MmMwMjAxNDU4OGEwMDU4ZTk5ZTU0NC5tcDMmbT03YTQzZmI0ZmQxMGVlMTNlMTAxMTgwZjlhZGJlMjhiZSZ2PWRvd24mbj212sj9zOwmcz3Qu9Pq0MAmcD1z.mp3";

        MediaPlayer player = createMediaPlayer();
        try {
            mInSeekMode = false;
            player.setDataSource(getApplicationContext(), Uri.parse(url));
            player.prepareAsync();

            setPlayerPlayingStatus(MusicPlayerConstants.STATUS_INITIALIZED);

            setPrepareStatus(MusicPlayerConstants.PREPARING_STATUS_PRE);

            showNotification(false);

            if (null != mServiceBinder) {
                if (null != mPlayerListener) {
                    log("createMediaPlayer,onPrePlaying");
                    try {
                        mPlayerListener.onPrePlaying(getCurrentPlayingItemImpl(),
                                mCurrentPlayingIndex);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
            }

            showNotification();

        } catch (IllegalArgumentException e) {
            e.printStackTrace();

            setPlayerPlayingStatus(MusicPlayerConstants.STATUS_INVALID);
        } catch (SecurityException e) {
            e.printStackTrace();
            setPlayerPlayingStatus(MusicPlayerConstants.STATUS_INVALID);
        } catch (IllegalStateException e) {
            e.printStackTrace();
            setPlayerPlayingStatus(MusicPlayerConstants.STATUS_INVALID);
        } catch (IOException e) {
            e.printStackTrace();
            setPlayerPlayingStatus(MusicPlayerConstants.STATUS_INVALID);
        }

        // player = setMediaPlayerListener(player);

        log("createMediaPlayer,done,player=" + player);
        return player;
    }

    private MediaPlayer setMediaPlayerListener(MediaPlayer player) {
        if (null == player) {
            return player;
        }

        player.setAudioStreamType(AudioManager.STREAM_MUSIC);

        // 添加各类Listener
        player.setOnBufferingUpdateListener(this);
        player.setOnCompletionListener(this);
        player.setOnErrorListener(this);
        player.setOnInfoListener(this);
        player.setOnPreparedListener(this);
        player.setOnSeekCompleteListener(this);
        // player.setOnTimedTextListener(this);
        player.setOnVideoSizeChangedListener(this);
        return player;
    }

    private void releaseMediaPlayer() {
        log("releaseMediaPlayer");

        if (null != mCloudaryMediaPlayer) {
            mCloudaryMediaPlayer.release();
            mCloudaryMediaPlayer = null;
        }
    }

    @Override
    public void onVideoSizeChanged(MediaPlayer mp, int width, int height) {
        log("onVideoSizeChanged,mp=" + mp + ",width=" + width + ",height=" + height);

        if (null != mServiceBinder) {
            if (null != mPlayerListener) {
                try {
                    mPlayerListener.onVideoSizeChanged(width, height);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void onSeekComplete(MediaPlayer mp) {
        log("onSeekComplete,mp=" + mp);

        mInSeekMode = false;

        if (null != mServiceBinder) {
            if (null != mPlayerListener) {
                try {
                    mPlayerListener.onSeekComplete();
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public boolean onInfo(MediaPlayer mp, int what, int extra) {
        log("onInfo,mp=" + mp + ",what=" + what + ",extra=" + extra);

        if (null != mServiceBinder) {
            if (null != mPlayerListener) {
                try {
                    return mPlayerListener.onInfo(what, extra);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }

        return false;
    }

    // 表示全部列表播放完成,当中间播放状态打断时，置为false
    private static boolean isPlayerListComplete = false;

    @Override
    public void onCompletion(MediaPlayer mp) {
        log("onCompletion,mp=" + mp);

        // 自动播放章节数目
        keepAutoPlayStaus(mAutoPlayChapterCount + 1, mAutoPlayTime);

        setPlayerPlayingStatus(MusicPlayerConstants.STATUS_PLACKBACK_COMPLETED);

        if (null != mServiceBinder) {
            if (null != mPlayerListener) {
                try {
                    mPlayerListener.onCompletion();
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }

        keepCurrentPosition(0);

        log("mCurrentPlayingIndex=" + mCurrentPlayingIndex + ",list count=" + getPlayerListCount());

        if (mCurrentPlayingIndex + 1 == getPlayerListCount()) {
            isPlayerListComplete = true;
            onPlayerListComplete();
        } else {
            isPlayerListComplete = false;
        }

        // 完毕后自动播放下一个,并清0进度.
        if (!getPlayerConfig().isContinuePlay()) {
            log("is not continue play,pausePlayer");
            pausePlayer();

        } else if (getPlayerConfig().getAutoStopChapterCount() > 0) {
            // 如果设置了自动播放章节数，需要判断
            if (mAutoPlayChapterCount >= getPlayerConfig().getAutoStopChapterCount()) {
                pausePlayer();
                keepAutoPlayStaus(0, 0);
                log("mAutoPlayChapterCount=" + mAutoPlayChapterCount
                        + ",goto sleep mode,pausePlayer");
            } else {

                playNext(true);
            }

        } else {

            playNext(true);
        }

    }

    /**
     * 播放列表全部播完了
     */
    private void onPlayerListComplete() {
        // 已经播放完成
        if (mCurrentPlayingIndex + 1 == getPlayerListCount()) {
            if (null != mPlayerListener) {
                // 数据库未读数需要保存为-1
                mPlayerListener.savePlayingStatus(mContext, getCurrentPlayingItemImpl(), musicAlbumObject, 0,
                        1);
            }
        }
    }

    // @Override
    // public void onTimedText(MediaPlayer mp, TimedText text) {
    // log("onTimedText,mp=" + mp + ",text=" + text);
    //
    // if (null != mServiceBinder) {
    // if (null != mServiceBinder.mOnTimedTextListener) {
    // mServiceBinder.mOnTimedTextListener.onTimedText(mp, text);
    // }
    // }
    // }

    @Override
    public void onPrepared(MediaPlayer mp) {
        log("onPrepared,mp=" + mp);

        setPlayerPlayingStatus(MusicPlayerConstants.STATUS_PREPARED);
        setPrepareStatus(MusicPlayerConstants.PREPARING_STATUS_DONE);

        if (null != mServiceBinder) {
            if (null != mPlayerListener) {
                try {
                    mPlayerListener.onPrepared();
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }

        if (null != mCurrentPlayingItem) {
            mCurrentPlayingItem.setTime(getCurrentPlayingTotalTimeImpl());
            if (mCurrentPlayingItem.isLocalFileType()) {
                seekToImpl(mCurrentPlayingItem.getPosition());
            } else if (mCurrentPlayingItem.isUrlType()) {
                seekToImpl(mCurrentPlayingItem.getPosition());
            }
        }

        // 准备完毕开始播放
        startPlayer();
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        log("onError,mp=" + mp + ",what=" + what + ",extra=" + extra);
        // error code:http://univasity.iteye.com/blog/898613
        // http://zhhx.blog.sohu.com/189762970.html
        // I/CloudaryMediaPlayerService(13881):
        // onError,mp=android.media.MediaPlayer@41f374f8,what=1,extra=-1004
        if (null != mCurrentPlayingItem) {
            StatUtil.onTingshuPlayError(mCurrentPlayingItem, "what="
                    + what + ",extra=" + extra);
        }

        // boolean isHandled = false;
        if (null != mServiceBinder) {
            if (null != mPlayerListener) {
                try {
                    mPlayerListener.onError(what, extra);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }

        if (what == 1 && extra == -1004) {
            stopPlayer();

            // 当发生错误，把状态重置到INVALID.
            setPlayerPlayingStatus(MusicPlayerConstants.STATUS_INVALID);
        } else if (what == 1 && extra == -2147483648) {
            // what=1,extra=-2147483648
            stopPlayer();

            // 当发生错误，把状态重置到INVALID.
            setPlayerPlayingStatus(MusicPlayerConstants.STATUS_INVALID);
        }

        return true;
    }

    @Override
    public void onBufferingUpdate(MediaPlayer mp, int percent) {
        log("onBufferingUpdate,mp=" + mp + ",percent=" + percent);

        if (null != mServiceBinder) {
            if (null != mPlayerListener) {
                try {
                    mPlayerListener.onBufferingUpdate(percent);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }

            // if (null != mServiceBinder.mPlayerListener) {
            // int position = mp.getCurrentPosition();
            // mServiceBinder.mPlayerListener.onUpdatePlayingProgress(mCurrentPlayingItem,
            // mCurrentPlayingIndex, position);
            // }
        }

        // keepCurrentPosition(mp);
    }

    // private boolean canStopPlayer() {
    // if (PlayerConstants.STATUS_PREPARED == mMediaPlayerPlayingStatus
    // || PlayerConstants.STATUS_STARTED == mMediaPlayerPlayingStatus
    // || PlayerConstants.STATUS_STOPPED == mMediaPlayerPlayingStatus
    // || PlayerConstants.STATUS_PAUSED == mMediaPlayerPlayingStatus
    // || PlayerConstants.STATUS_INITIALIZED == mMediaPlayerPlayingStatus
    // || PlayerConstants.STATUS_PLACKBACK_COMPLETED ==
    // mMediaPlayerPlayingStatus) {
    // return true;
    // } else {
    // return false;
    // }
    // }

    /**
     * 停止Player,需要销毁资源
     *
     * @throws android.os.RemoteException
     */
    private boolean stopPlayer() {
        keepAutoPlayStaus(0, 0);

        keepCurrentPosition(null);

        savePlayingStatus();

        // if (null != mCloudaryMediaPlayer && canStopPlayer()) {
        // 由于stop播放器需要销毁MediaPlayer,所以直接releaseMediaPlayer。
        if (null != mCloudaryMediaPlayer && true) {
            try {
                removeAudioFocus();
                // mCloudaryMediaPlayer.stop();
                releaseMediaPlayer();
                setPlayerPlayingStatus(MusicPlayerConstants.STATUS_STOPPED);
                showNotification(false);
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }

            if (null != mServiceBinder) {
                if (null != mPlayerListener) {
                    try {
                        mPlayerListener.onStop();
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
            }

            return true;
        } else if (null == mCloudaryMediaPlayer) {
            if (null != mServiceBinder) {
                if (null != mPlayerListener) {
                    try {
                        mPlayerListener.onStop();
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
            }

            return true;
        } else {
            return false;
        }
    }

    /**
     * 保存播放器的播放状态
     *
     * @see MusicPlayerConstants#STATUS_PAUSED
     * //     * @see PlayerConstants#STATUS_PLAYING
     * @see MusicPlayerConstants#STATUS_STOPPED
     */
    private void setPlayerPlayingStatus(int status) {
        mMediaPlayerPlayingStatus = status;
    }

    private boolean canPausePlayer() {
        if (MusicPlayerConstants.STATUS_STARTED == mMediaPlayerPlayingStatus
                || MusicPlayerConstants.STATUS_PAUSED == mMediaPlayerPlayingStatus) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * 暂停
     */
    private boolean pausePlayer() {
        if (null != mCloudaryMediaPlayer && canPausePlayer()) {
            keepCurrentPosition(null);

            savePlayingStatus();

            try {
                removeAudioFocus();
                mCloudaryMediaPlayer.pause();
                setPlayerPlayingStatus(MusicPlayerConstants.STATUS_PAUSED);
                showNotification(false);
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }

            if (null != mServiceBinder) {
                if (null != mPlayerListener) {
                    try {
                        mPlayerListener.onPause();
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
            }

            return true;

        } else {
            return false;
        }

    }

    private boolean canStartPlayer() {
        if (MusicPlayerConstants.STATUS_PREPARED == mMediaPlayerPlayingStatus
                || MusicPlayerConstants.STATUS_STARTED == mMediaPlayerPlayingStatus
                || MusicPlayerConstants.STATUS_PAUSED == mMediaPlayerPlayingStatus
                || MusicPlayerConstants.STATUS_PLACKBACK_COMPLETED == mMediaPlayerPlayingStatus) {
            return true;
        } else {
            return false;
        }
    }

    private synchronized boolean startPlayer() {
        if (null != mCloudaryMediaPlayer && canStartPlayer()) {
            try {
                requestAudioFocus();
                mCloudaryMediaPlayer.start();
                setPlayerPlayingStatus(MusicPlayerConstants.STATUS_STARTED);
                showNotification(false);
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }

            // if (!isPlaying()) {
            // FIXME:之前为什么要在没有播放的情况下调用呢
            if (true) {
                if (null != mServiceBinder) {
                    if (null != mPlayerListener) {
                        try {
                            mPlayerListener.onStart();
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                    }

                    if (null != mPlayerListener) {
                        try {
                            mPlayerListener.onStartPlaying(getCurrentPlayingItemImpl());
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }

            return true;
        } else {
            // 如果这时候已经跑过stop过,mCloudaryMediaPlayer为空，默认重新播放当前的item,但缓冲进度可能不在了.
            return startPlayer(mCurrentPlayingItem);
        }

    }

    private boolean startPlayer(MusicPlayerItem item) {
        log("*********");
        log("startPlayer:");
        log("" + item);
        log("*********");

        if (null != item) {
            StatUtil.onTingshuBehavior(item.getMusicAlbumId(), "" + item.getMusicId(), ""
                    + mStartPlayerSourceFrom);
        }

        if (null == item || TextUtils.isEmpty(item.getUrl())) {
            if (null != mPlayerListener) {
                try {
                    stopPlayer();
                    mPlayerListener.onShowMessage(getCurrentPlayingItemImpl(),
                            MusicPlayerConstants.MESSAGE_URI_IS_EMPTY, "uri is empty");
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }

            return false;
        }

        if (item.isLocalFileType()) {
            if (!PlayerUtils.isFileExist(item)) {
                if (null != mPlayerListener) {
                    try {
                        mPlayerListener.onShowMessage(getCurrentPlayingItemImpl(),
                                MusicPlayerConstants.MESSAGE_FILE_NOT_EXIST, "file not exist.");
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }

                return false;
            }
        }

        String url = item.getUrl();

        // if (null != mCloudaryMediaPlayer) {
        // releaseMediaPlayer();
        // }

        try {
            mCloudaryMediaPlayer = createMediaPlayerAsync(url);
            // 如果是url调用seekTo会触发onCompletion,需要在onPrepared调用seekTo,否则会报错误，致使seekTo失败.
            // if (item.isLocalFileType()) {
            // seekToImpl(item.getPosition());
            // mCloudaryMediaPlayer.seekTo(item.getPosition() * 1000);
            // }

            // seekTo(10000);

            if (null == mCloudaryMediaPlayer) {
                return false;
            }
            // mCloudaryMediaPlayer.seekTo(60000);
            // mCloudaryMediaPlayer.prepareAsync();

        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return false;
        } catch (SecurityException e) {
            e.printStackTrace();
            return false;
        } catch (IllegalStateException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    // {Prepared, Started, Paused, PlaybackCompleted}
    private boolean canSeekTo() {
        if (MusicPlayerConstants.STATUS_PREPARED == mMediaPlayerPlayingStatus
                || MusicPlayerConstants.STATUS_STARTED == mMediaPlayerPlayingStatus
                || MusicPlayerConstants.STATUS_PAUSED == mMediaPlayerPlayingStatus
                || MusicPlayerConstants.STATUS_PLACKBACK_COMPLETED == mMediaPlayerPlayingStatus) {
            return true;
        } else {
            return false;
        }
    }

    private boolean seekToImpl(int position) {
        if (position < 0) {
            return false;
        }

        log("seekTo,position=" + position + ",total=" + getCurrentPlayingTotalTimeImpl());
        if (null != mCloudaryMediaPlayer && canSeekTo()) {
            try {
                if (mCurrentPlayingItem.isNeedOrder()) {
                    // 如果需要购买，则不需要跳转
                    // mCloudaryMediaPlayer.seekTo(position * 1000);
                } else {
                    mInSeekMode = true;
                    mBeforeSeekPostion = getCurrentPositionImpl();
                    mSeekPosition = position;
                    mSeekTimestamp = System.currentTimeMillis();

                    mCloudaryMediaPlayer.seekTo(position * 1000);
                }

            } catch (Exception e) {
                e.printStackTrace();

                return false;
            }

            if (null != mServiceBinder) {
                if (null != mPlayerListener) {
                    try {
                        mPlayerListener.onSeekTo(position);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
            }

            return true;
        } else {
            return false;
        }

    }

    /**
     * 获得对应index的item,如果超出范围，返回null。
     *
     * @param index
     * @return
     */
    private MusicPlayerItem getPlayerItem(int index) {

        int size = mPlayList.size();
        if (size == 0) {
            return null;
        }

        if (index >= size) {
            return mPlayList.get(size - 1);
        }

        if (index < 0) {
            return mPlayList.get(0);
        }

        MusicPlayerItem item = mPlayList.get(index);
        // try {
        // item = item.clone();
        // } catch (CloneNotSupportedException e) {
        // e.printStackTrace();
        // }

        return item;
    }

    /**
     * 获得对应index的item,如果超出范围，返回null。
     *
     * @param item
     * @return
     */
    private MusicPlayerItem getPlayerItem(MusicPlayerItem item) {

        int size = mPlayList.size();
        if (size == 0) {
            return null;
        }

        for (int i = 0; i < size; i++) {
            MusicPlayerItem musicPlayerItem = mPlayList.get(i);
            if (item.equals(musicPlayerItem)) {
                return musicPlayerItem;
            }
        }

        return null;
    }

    private int getPlayerListSize() {
        int size = mPlayList.size();

        return size;
    }

    private boolean playNext() {
        // 自动播放章节数目
        keepAutoPlayStaus(0, 0);

        return playNext(false);
    }

    /**
     * 是否需要清除播放进度，如果是从onCompletion进入，就需要清0.
     *
     * @param clearCurrentPosition
     * @return
     */
    private boolean playNext(boolean clearCurrentPosition) {

        log("playNext");

        mCurrentPlayingIndex++;

        if (clearCurrentPosition) {
            keepCurrentPosition(0);
        } else {
            keepCurrentPosition(null);
        }

        // 已经到了最后一个
        if (getPlayerListSize() == mCurrentPlayingIndex) {
            mCurrentPlayingIndex = getPlayerListSize() - 1;
            return false;
        }

        MusicPlayerItem nextItem = getPlayerItem(mCurrentPlayingIndex);

        // 如果没有下一个，需要重置index到最后一个位置
        if (null == nextItem) {
            mCurrentPlayingIndex = getPlayerListSize() - 1;
        }

        boolean handled = playItemPreHandle(nextItem);

        // 不需要恢复到之前的播放进度
        if (!MusicPlayerConstants.REMEMBER_EVERY_CHAPTER_PLAYING_POSITION) {
            if (null != nextItem) {
                nextItem.position = 0;
            }
        }

        if (null != mServiceBinder) {
            if (null != mPlayerListener) {
                try {
                    mPlayerListener.onNext(nextItem, mCurrentPlayingIndex);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }

        if (handled) {
            setCurrentPlayerItem(nextItem);
            return false;
        }

        return playItem(nextItem);
    }

    /**
     * 播放前预处理，
     *
     * @param item
     * @return true 如果已经处理掉了
     */
    private boolean playItemPreHandle(MusicPlayerItem item) {
        // 如果需要购买
        if (item.isNeedOrder()) {
            stopPlayer();

            if (null != mServiceBinder) {
                if (null != mPlayerListener) {
                    try {
                        mPlayerListener.onShowMessage(item, MusicPlayerConstants.MESSAGE_NEED_ORDER,
                                "This chapter need order,please order first,thanks.");
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
            }

            return true;
        } else
            // 如果是网络无效，并且是url,停止播放
            if (isNetworkUnAvailableAndUrlPlay(item)) {

                stopPlayer();

                if (null != mServiceBinder) {
                    if (null != mPlayerListener) {
                        try {
                            mPlayerListener.onShowMessage(item,
                                    MusicPlayerConstants.MESSAGE_NO_NETWORK_AVAILABLE,
                                    "It is no network available");
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                    }
                }

                return true;
            } else
                // 如果是非wifi环境下，并且是url,停止播放
                if (!isWifiAndUrlAndAutoPlay(item)) {

                    stopPlayer();

                    if (null != mServiceBinder) {
                        if (null != mPlayerListener) {
                            try {
                                mPlayerListener
                                        .onShowMessage(item, MusicPlayerConstants.MESSAGE_NOT_WIFI,
                                                "It is not wifi,it will auto stop the player to protect your network data usage.");
                            } catch (RemoteException e) {
                                e.printStackTrace();
                            }
                        }
                    }

                    return true;
                } else if (isPlayingLocalFile(item) && !PlayerUtils.isFileExist(item)) {
                    stopPlayer();

                    if (null != mServiceBinder) {
                        if (null != mPlayerListener) {
                            try {
                                mPlayerListener.onShowMessage(item, MusicPlayerConstants.MESSAGE_FILE_NOT_EXIST,
                                        "file not exist.");
                            } catch (RemoteException e) {
                                e.printStackTrace();
                            }
                        }
                    }

                    return true;
                } else if (isPlayingUrl(item) && TextUtils.isEmpty(item.getUrl())) {
                    stopPlayer();

                    if (null != mServiceBinder) {
                        if (null != mPlayerListener) {
                            try {
                                mPlayerListener.onShowMessage(item, MusicPlayerConstants.MESSAGE_URI_IS_EMPTY,
                                        "url is empty");
                            } catch (RemoteException e) {
                                e.printStackTrace();
                            }
                        }
                    }

                    return true;
                }

        return false;
    }

    /**
     * 判断是否继续播放，如果是url并且非wifi情况下，则停止播放.
     *
     * @return
     */
    private boolean isWifiAndUrlAndAutoPlay(MusicPlayerItem item) {
        boolean isWifi = PlayerUtils.isWifiActive(getApplicationContext());
        boolean isUrl = isPlayingUrl(item);

        if (!isWifi && isUrl) {
            // 如果启用了流量保护设置,则停止播放
            if (getPlayerConfig().isDataProtect()) {
                return false;
            } else {
                return true;
            }
        } else {
            return true;
        }
    }

    /**
     * 判断是否继续播放，如果是url并且非wifi情况下，则停止播放.
     *
     * @return
     */
    private boolean isNetworkUnAvailableAndUrlPlay(MusicPlayerItem item) {
        boolean isNetworkAvaiable = PlayerUtils.isNetworkAvailable(mContext);
        boolean isUrl = isPlayingUrl(item);

        if (!isNetworkAvaiable && isUrl) {
            return true;
        } else {
            return false;
        }
    }

    private boolean playPrev() {
        log("playPrev");

        mAutoPlayChapterCount = 0;
        mAutoPlayTime = 0;

        mCurrentPlayingIndex--;

        keepCurrentPosition(null);

        if (mCurrentPlayingIndex < 0) {
            mCurrentPlayingIndex = 0;
            return false;
        }

        // TODO:当为空的时候，当前的item是否需要替换为空
        MusicPlayerItem prevItem = getPlayerItem(mCurrentPlayingIndex);

        // 已经第一个了，不能再往前了
        if (null == prevItem) {
            mCurrentPlayingIndex = 0;
        }

        boolean handled = playItemPreHandle(prevItem);

        // 不需要恢复到之前的播放进度
        if (!MusicPlayerConstants.REMEMBER_EVERY_CHAPTER_PLAYING_POSITION) {
            if (null != prevItem) {
                prevItem.position = 0;
            }
        }

        if (null != mServiceBinder) {
            if (null != mPlayerListener) {
                try {
                    mPlayerListener.onPrev(prevItem, mCurrentPlayingIndex);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }

        if (handled) {
            setCurrentPlayerItem(prevItem);

            return false;
        }

        return playItem(prevItem);
    }

    private boolean playItem(MusicPlayerItem item) {
        setCurrentPlayerItem(item);

        setPlayerPlayingStatus(MusicPlayerConstants.STATUS_INVALID);

        if (null == item) {
            return false;
        }

        // String url = item.getUrl();
        return startPlayer(item);
    }

    private void appendItemToPlayList(MusicPlayerItem item) {
        if (null == item) {
            return;
        }

        // log("appendItemToPlayList:\n" + item.toString());
        // 如果當前播放列表沒有正在播放的，取最初的作為當前的播放item.
        if (null == mCurrentPlayingItem) {
            setCurrentPlayerItem(item);
        }

        mPlayList.add(item);

        if (null != mPlayerListener) {
            mPlayerListener.onPlayerListChange();
        }
    }

    private void appendListToPlayList(ArrayList<MusicPlayerItem> list) {
        if (null == list || 0 == list.size()) {
            return;
        }

        if (null == mCurrentPlayingItem) {
            log("appendListToPlayList,setCurrentPlayerItem=" + list.get(0));
            setCurrentPlayerItem(list.get(0));
            mCurrentPlayingIndex = 0;
        }

        mPlayList.addAll(list);

        if (null != mPlayerListener) {
            mPlayerListener.onPlayerListChange();
        }
    }

    /**
     * 更新播放url和购买状态到播放列表,主要用于在购买成功之后更新播放列表状态
     *
     * @param updateAuthedList
     */
    private void updateAuthedListToPlayList(ArrayList<MusicPlayerItem> updateAuthedList) {
        if (null == updateAuthedList || 0 == updateAuthedList.size()) {
            return;
        }

        int size = getPlayerListSize();
        for (int i = 0; i < size; i++) {
            MusicPlayerItem item = mPlayList.get(i);
            updateAuthedItem(item, updateAuthedList);
        }

        if (null != mPlayerListener) {
            mPlayerListener.onPlayerListChange();
        }
    }

    /**
     * 更新是否需要购买和播放url.
     *
     * @param item
     * @param updateAuthedList
     */
    private void updateAuthedItem(MusicPlayerItem item, ArrayList<MusicPlayerItem> updateAuthedList) {
        if (null == item || null == updateAuthedList || updateAuthedList.size() == 0) {
            return;
        }

        int size = updateAuthedList.size();
        for (int i = 0; i < size; i++) {
            MusicPlayerItem authedplayerItemMusic = updateAuthedList.get(i);
            if (item.equals(authedplayerItemMusic)) {
                log("updateAuthedItem,before,item=" + item);
                log("updateAuthedItem,before,authedplayerItem=" + authedplayerItemMusic);

                item.setIsNeedOrder(authedplayerItemMusic.isNeedOrder());
                // 如果是本地文件并且已经存在，则不需要更新
                if (item.isLocalFileType() && PlayerUtils.isFileExist(item)) {

                } else if (item.isLocalFileType() && !PlayerUtils.isFileExist(item)) {
                    item.setUrl(authedplayerItemMusic.getUrl());
                    item.setFileType(PlayerUtils.getFileType(authedplayerItemMusic.getUrl()));
                } else {
                    if (authedplayerItemMusic.isLocalFileType()
                            && !PlayerUtils.isFileExist(authedplayerItemMusic)) {
                        // 如果需要更新的播放链接为本地的，但文件又不存在，则不做任何事情
                        log("updateAuthedItem,is local file,but not exist,do nothing");
                    } else {
                        item.setUrl(authedplayerItemMusic.getUrl());
                        item.setFileType(PlayerUtils.getFileType(authedplayerItemMusic.getUrl()));
                    }
                }

                log("updateAuthedItem,after,item=" + item);
            }

            // 同步当前的播放item.
            if (authedplayerItemMusic.equals(mCurrentPlayingItem)) {
                // setCurrentPlayerItem(item);
            }
        }
    }

    /**
     * 在当前播放的位置插入
     *
     * @param item
     */
    private void insertItemToPlayList(MusicPlayerItem item) {
        if (null == item) {
            return;
        }

        // log("insertItemToPlayList:\n" + item.toString());
        // 如果當前播放列表沒有正在播放的，取最初的作為當前的播放item.
        if (null == mCurrentPlayingItem) {
            setCurrentPlayerItem(item);
        }

        if (mCurrentPlayingIndex - 1 > getPlayerListSize()) {
            mPlayList.insertElementAt(item, getPlayerListSize() - 1);
        } else {
            mPlayList.insertElementAt(item, mCurrentPlayingIndex);
        }

        if (null != mPlayerListener) {
            mPlayerListener.onPlayerListChange();
        }
    }

    /**
     * 从播放列表删除一项
     *
     * @param item
     */
    private void deleteItemFromPlayList(MusicPlayerItem item) {
        // 如果是当前播放的,则停止播放，并播放下一个
        if (item.equals(mCurrentPlayingItem)) {
            mPlayList.remove(mCurrentPlayingIndex);
            stopPlayer();
            playNext();

            if (null != mPlayerListener) {
                mPlayerListener.onPlayerListChange();
            }

            return;
        }

        int size = mPlayList.size();
        for (int i = 0; i < size; i++) {
            MusicPlayerItem item2 = mPlayList.get(i);
            if (item2.equals(item)) {
                mPlayList.remove(i);

                if (null != mPlayerListener) {
                    mPlayerListener.onPlayerListChange();
                }

                return;
            }
        }

    }

    private void clearPlayListImpl() {
        log("clearPlayListImpl");

        stopPlayer();
        mPlayList.clear();
        mCurrentPlayingIndex = 0;
        setCurrentPlayerItem(null);

        if (null != mPlayerListener) {
            mPlayerListener.onPlayerListChange();
        }

        // 清空播放列表时，取消notification.
        dismissNotification();
    }

    private int isInPlayList(MusicPlayerItem item) {
        if (null == item) {
            return -1;
        }

        int size = mPlayList.size();

        for (int i = 0; i < size; i++) {
            MusicPlayerItem object = mPlayList.get(i);
            if (item.equals(object)) {
                return i;
            }
        }

        return -1;
    }

    /**
     * 跳转播放
     */
    private void jumpToItem(MusicPlayerItem item) {
        log("jumpToItem,item=" + item);

        // 如果为当前item则不需要保存进度，否则会造成当前进度重置为0
        if (!item.equals(mCurrentPlayingItem)) {
            keepCurrentPosition(getCurrentPositionImpl());
        }

        int index = isInPlayList(item);
        log("jumpToItem,isInPlayList,index=" + index);
        if (-1 == index) {
            // 如果不在播放列表
            // mCurrentPlayingItem = item;
            insertItemToPlayList(item);
            playItem(item);

        } else {
            // 如果是当前播放的item,直接播放
            if (item.equals(mCurrentPlayingItem)) {

                if (!isPlaying()) {
                    startPlayer();
                }

            } else {
                mCurrentPlayingIndex = index;
                playItem(getPlayerItem(index));
            }
        }

        if (null != mPlayerListener) {
            mPlayerListener.onPlayerListChange();
        }
    }

    /**
     * 更新播放到item,但不播放
     */
    private void updateToItem(MusicPlayerItem item) {
        log("updateToItem,item=" + item);
        log("updateToItem,mCurrentPlayingItem=" + mCurrentPlayingItem);

        // 如果为当前item则不需要保存进度，否则会造成当前进度重置为0
        if (!item.equals(mCurrentPlayingItem)) {
            keepCurrentPosition(getCurrentPositionImpl());
        }

        int index = isInPlayList(item);

        if (-1 == index) {
            // 如果不在播放列表
            // mCurrentPlayingItem = item;
            insertItemToPlayList(item);
            // playItem(item);

        } else {
            // 如果是当前播放的item,直接播放
            if (item.equals(mCurrentPlayingItem)) {

                // 同步播放进度
                // mCurrentPlayingItem.position = item.getPosition();

                if (!isPlaying()) {
                    // startPlayer();
                }

            } else {
                mCurrentPlayingIndex = index;
                // playItem(getPlayerItem(index));
                setCurrentPlayerItem(getPlayerItem(item));
            }
        }

        if (null != mPlayerListener) {
            mPlayerListener.onPlayerListChange();
        }
    }

    private void updateItemPositionToPlayerList(MusicPlayerItem item) {
        if (null == item) {
            return;
        }

        int size = mPlayList.size();

        for (int i = 0; i < size; i++) {
            MusicPlayerItem object = mPlayList.get(i);
            if (item.equals(object)) {
                object.setPosition(item.getPosition());
            }
        }

    }

    /**
     * 保留当前播放进度
     *
     * @param mediaPlayer
     */
    private void keepCurrentPosition(MediaPlayer mediaPlayer) {
        int position = getCurrentPositionImpl();

        keepCurrentPosition(position);
    }

    /**
     * 保存当前播放进度，如果播放进度等于总长，重置为0.
     *
     * @param position
     */
    private void keepCurrentPosition(int position) {

        if (getCurrentPlayingTotalTimeImpl() == position) {
            position = 0;
        }

        if (null != mCurrentPlayingItem) {
            log("keepCurrentPosition,musicId=" + mCurrentPlayingItem.musicId + ".position="
                    + position + ",total=" + getCurrentPlayingTotalTimeImpl());
            if (canGetDuration() && !mCurrentPlayingItem.isNeedOrder()) {
                // 当前为不需要购买的，才保存
                mCurrentPlayingItem.setPosition(position);
            } else if (mCurrentPlayingItem.isNeedOrder()) {
                mCurrentPlayingItem.setPosition(0);
            } else {
                mCurrentPlayingItem.setPosition(position);
            }

            // 当前每个item播放状态，保存无效，要判断当前播放状态，应该使用isPlaying()来进行判断
            // mCurrentPlayingItem.setPlayingStatus(mMediaPlayerPlayingStatus);
        }
    }

    private void keepAutoPlayStaus(int chapterCount, int time) {
        mAutoPlayChapterCount = chapterCount;
        mAutoPlayTime = time;
    }

    // {Prepared, Started, Paused, Stopped, PlaybackCompleted}
    private boolean canGetDuration() {
        if (MusicPlayerConstants.STATUS_PREPARED == mMediaPlayerPlayingStatus
                || MusicPlayerConstants.STATUS_STARTED == mMediaPlayerPlayingStatus
                || MusicPlayerConstants.STATUS_PAUSED == mMediaPlayerPlayingStatus
                || MusicPlayerConstants.STATUS_STOPPED == mMediaPlayerPlayingStatus
                || MusicPlayerConstants.STATUS_PLACKBACK_COMPLETED == mMediaPlayerPlayingStatus) {
            return true;
        } else {
            return false;
        }
    }

    public int getCurrentPlayingTotalTimeImpl() {
        // 如果是本的直接取本地时间
        if (null != mCloudaryMediaPlayer && null != mCurrentPlayingItem && canGetDuration()
                && mCurrentPlayingItem.isLocalFileType()) {
            return mCloudaryMediaPlayer.getDuration() / 1000;
//            return mCurrentPlayingItem.getTime();
        } else if (null != mCurrentPlayingItem && mCurrentPlayingItem.isUrlType()) {
            // url
            return mCurrentPlayingItem.getTime();
        } else {
            return MusicPlayerConstants.ENVALID_TIME_DURATION;
        }
    }

    // {Idle, Initialized, Prepared, Started, Paused, Stopped,
    // PlaybackCompleted}
    private boolean canGetCurrentPosition() {
        if (MusicPlayerConstants.STATUS_PREPARED == mMediaPlayerPlayingStatus
                || MusicPlayerConstants.STATUS_IDLE == mMediaPlayerPlayingStatus
                || MusicPlayerConstants.STATUS_INITIALIZED == mMediaPlayerPlayingStatus
                || MusicPlayerConstants.STATUS_STARTED == mMediaPlayerPlayingStatus
                || MusicPlayerConstants.STATUS_PAUSED == mMediaPlayerPlayingStatus
                || MusicPlayerConstants.STATUS_STOPPED == mMediaPlayerPlayingStatus
                || MusicPlayerConstants.STATUS_PLACKBACK_COMPLETED == mMediaPlayerPlayingStatus) {
            return true;
        } else {
            return false;
        }

    }

    public int getCurrentPositionImpl() {
        try {
            if (null != mCloudaryMediaPlayer && canGetCurrentPosition()) {
                return mCloudaryMediaPlayer.getCurrentPosition() / 1000;
            } else {
                return 0;
            }
        } catch (Exception e) {
            e.printStackTrace();

            return 0;
        }
    }

    public int getPlayerListCount() {
        if (null == mPlayList) {
            return 0;
        }

        return mPlayList.size();
    }

    private void setPrepareStatus(int prepareStatus) {
        mMediaPlayerPreparingStatus = prepareStatus;
    }

    /**
     * 判断播放器是否处于准备状态
     *
     * @return
     */
    private boolean isPlayerPreparedImpl() {
        if (MusicPlayerConstants.PREPARING_STATUS_DONE == mMediaPlayerPreparingStatus
                && MusicPlayerConstants.STATUS_STOPPED != mMediaPlayerPlayingStatus) {
            return true;
        } else {
            return false;
        }
    }

    private boolean isPlaying() {
        if (MusicPlayerConstants.STATUS_STARTED == mMediaPlayerPlayingStatus) {
            return true;
        } else {
            return false;
        }

        // mCloudaryMediaPlayer.isPlaying();
    }

    /**
     * 判断当前是否是在播放url.
     *
     * @return
     */
    private boolean isPlayingUrl() {
        if (null != mCurrentPlayingItem && mCurrentPlayingItem.isUrlType()) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * 判断当前是否是在播放url.
     *
     * @return
     */
    private boolean isPlayingUrl(MusicPlayerItem item) {
        return item.isUrlType();
    }

    /**
     * 判断当前是否是在播放本地文件
     *
     * @return
     */
    private boolean isPlayingLocalFile(MusicPlayerItem item) {
        return !item.isUrlType();
    }

    /**
     * 是否可以往前播放
     *
     * @return
     */
    private boolean canForwardImpl() {
        if (mCurrentPlayingIndex + 1 >= getPlayerListSize()) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * 是否可以往后播放
     *
     * @return
     */
    private boolean canGoBackImpl() {
        if (mCurrentPlayingIndex - 1 < 0) {
            return false;
        } else {
            return true;
        }
    }

    private MusicPlayerConfig getPlayerConfig() {
        MusicPlayerConfig config = null;
        if (null != mPlayerListener) {
            config = mPlayerListener.getPlayerConfig();
            if (null == config) {
                // 如果没有返回，直接使用上次的保留备份
                config = mMusicPlayerConfig;
            } else {
                // 同时保存一份配置
                mMusicPlayerConfig = config;
            }
        }

        if (null == config) {
            config = new MusicPlayerConfig();
        }

        return config;
    }

    /**
     * 在回调返回item时，传回当前的播放状态
     *
     * @return
     */
    private MusicPlayerItem getCurrentPlayingItemImpl() {
        if (null != mCurrentPlayingItem) {
            mCurrentPlayingItem.setPlayingStatus(mMediaPlayerPlayingStatus);
            return mCurrentPlayingItem;
        } else {
            return null;
        }
    }

    /**
     * 判断非wifi情况下是否需要停止播放
     *
     * @return
     */
    private boolean isNeedStopPlayerWhenNotWifi() {
        boolean isWifiActive = PlayerUtils.isWifiActive(getApplicationContext());
        boolean isUrlType = isPlayingUrl();
        // 当非wifi网络，并且是播放url时，需要停止播放
        if (PlayerUtils.isNetworkAvailable(mContext) && !isWifiActive && isUrlType) {
            return true;
        } else {
            return false;
        }
    }

    private void setCurrentPlayerItem(MusicPlayerItem item) {
        log("setCurrentPlayerItem,item=" + item);
        mCurrentPlayingItem = item;

        if (null != item) {
            savePlayingStatus();
        }

    }

    /**
     * 保存当前播放状态
     */
    private void savePlayingStatus() {
        if (null != mPlayerListener) {
            // mPlayerListener.onSavePlayingProgress(getApplicationContext(),
            // mCurrentPlayingItem,
            // mBook, getPlayerListCount(), mCurrentPlayingIndex);
            if (isPlayerListComplete) {
                onPlayerListComplete();
            } else {
                mPlayerListener.savePlayingStatus(getApplicationContext(), mCurrentPlayingItem,
                        musicAlbumObject, getPlayerListCount(), mCurrentPlayingIndex);
            }

        }
    }
}
