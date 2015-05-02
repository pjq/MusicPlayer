package me.pjq.musicplayer;

import android.animation.ObjectAnimator;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import me.pjq.musicplayer.utils.PlayerUtils;
import me.pjq.musicplayer.utils.ToastUtil;
import me.pjq.musicplayer.utils.Utils;

/**
 * 播放器自定义View.
 *
 * @author pengjianqing
 */
public class MusicPlayerMainView extends LinearLayout implements OnClickListener, MusicPlayerConstants {

    private static String TAG = MusicPlayerMainView.class.getSimpleName();

    private static final boolean DEBUG_LOG = true;

    private IMusicPlayerService mIMusicPlayerService;

    private Context mContext;

    private View mPlayPauseLinLayout;

    private LinearLayout mMp3upLinLayout;

    private View mMp3downLinLayout;

    private ImageView mPlayorpauseIv;

    private TextView mPlayingTimeTv;

    private TextView mPlayzongTimeTv;

    private SeekBar mSeekBar;


    private ProgressBar mProBarlarge;

    private View mView;

    private MusicPlayerUIBridgeInterface mPlayerUIInterface;

    boolean isSimpleView = true;

    /**
     * 是否处于进度条托动状态
     */
    private boolean mIsInSeekMode = false;


    public MusicPlayerMainView(Context context) {
        super(context);
        mContext = context;
        init();
    }

    public MusicPlayerMainView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        init();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        mSetListenerCount = 0;
    }

    public void setPlayerUIInterface(MusicPlayerUIBridgeInterface playerUIInterface) {
        mPlayerUIInterface = playerUIInterface;
    }

    public void setMusicPlayerView(int resId) {
        mPlayPauseLinLayout.setBackgroundResource(resId);
    }

    ObjectAnimator animator;

    private void initAnimation(boolean isPlaying) {
        if (isSimpleView) {
            if (null == animator) {
                animator = ObjectAnimator.ofFloat(mPlayPauseLinLayout, "rotation", 0f, 360f);
                animator.setDuration(5000);
                animator.setRepeatCount(ObjectAnimator.INFINITE);
            }

            if (isPlaying) {
                animator.start();
            } else {
                animator.pause();
            }
        }

    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        log("onDetachedFromWindow");

        PlayerUtils.returnNotificationController(mContext);

        if (null != mIMusicPlayerService) {
            try {
                mIMusicPlayerService.savePlayingStatus();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        try {
            // 如果是处于播放状态，则退出时需要弹notification.
            if (musicAlbumObject.isOnline()) {
                // 如果不在播放，直接停止,清空状态
                PlayerUtils.stopService(mContext);
            } else {
                if (mIMusicPlayerService.isPlaying()) {
                    PlayerUtils.showNotification(mContext);
                } else {
                    // 如果不在播放，直接停止,清空状态
                    PlayerUtils.dismissNotification(mContext);
                    PlayerUtils.stopService(mContext);
                }
            }

        } catch (RemoteException e1) {
            e1.printStackTrace();
        }

        if (true) {
            if (null != mIMusicPlayerService) {
                try {
                    mIMusicPlayerService.unSetPlayerListener(mPlayerListener);
                    mPlayerListener = null;
                    mSetListenerCount = 0;
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }

        if (null != mServiceConnection) {
            mContext.unbindService(mServiceConnection);
            mServiceConnection = null;
        }

    }

    private void init() {
        log("init");

        bindPlayerService();
        PlayerUtils.requireNotificationController(mContext);

        PlayerUtils.dismissNotification(mContext);

        View view = onCreateView(LayoutInflater.from(mContext));
        addView(view);
    }

    private View onCreateView(LayoutInflater inflater) {
        log("onCreateView,mView=" + mView);
        if (isSimpleView) {
            mView = inflater.inflate(R.layout.musicplayer_controller_view_simple, null);
        } else {
            mView = inflater.inflate(R.layout.musicplayer_controller_view, null);
            mView.setBackgroundResource(R.drawable.musicplayer_player_bg);
        }
        ensureUi(mView);

        return mView;
    }

    public void onNewIntent(Context context, Bundle bundle) {
        if (null == bundle) {
            return;
        }

        String action = bundle.getString(KEY_ACTION);
        String uri = bundle.getString(KEY_URI);
        if (!TextUtils.isEmpty(action) && action.equalsIgnoreCase(Intent.ACTION_VIEW)) {
            PlayerUtils.jumpToItem(context, uri);
        }
    }

    private void ensureUi(View view) {
        mPlayPauseLinLayout = (View) view.findViewById(R.id.playorpause);
        mMp3upLinLayout = (LinearLayout) view.findViewById(R.id.mp3up);
        mMp3downLinLayout = (View) view.findViewById(R.id.mp3down);
        mPlayorpauseIv = (ImageView) view.findViewById(R.id.playorpauseIv);
        mPlayingTimeTv = (TextView) view.findViewById(R.id.playingtime);
        mPlayzongTimeTv = (TextView) view.findViewById(R.id.playzongtime);
        mSeekBar = (SeekBar) view.findViewById(R.id.seekb);
        mProBarlarge = (ProgressBar) view.findViewById(R.id.ProgressBar04);

        mPlayPauseLinLayout.setOnClickListener(this);
        mMp3upLinLayout.setOnClickListener(this);
        mMp3downLinLayout.setOnClickListener(this);

        mSeekBar.setOnTouchListener(new OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                mIsInSeekMode = true;
                int position = mSeekBar.getProgress();

                updatePlayingProgressTime(position);

                return false;
            }
        });

        mSeekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

            }

            public void onStartTrackingTouch(SeekBar seekBar) {
                mIsInSeekMode = true;
                int position = seekBar.getProgress();

                updatePlayingProgressTime(position);
            }

            public void onStopTrackingTouch(SeekBar seekBar) {
                mIsInSeekMode = false;

                int position = seekBar.getProgress();
                log("onStopTrackingTouch,position=" + position);
                // PlayerUtils.playSeekTo(mContext, position);

                if (null != mIMusicPlayerService) {
                    try {
                        mIMusicPlayerService.seekTo(position);

                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

    }

    private IMusicPlayerListener mPlayerListener = new IMusicPlayerListener.Stub() {

        @Override
        public void onStop() {
            log("onStop");

            mLocalHandler.sendEmptyMessage(MESSAGE_ON_STOP);
        }

        @Override
        public void onStart() {
            log("onStart");

            mLocalHandler.sendEmptyMessage(MESSAGE_ON_START);
        }

        @Override
        public void onSeekTo(int position) {
            log("onSeekTo,position=" + position);

            Message msg = mLocalHandler.obtainMessage(MESSAGE_ON_SEEK_TO);
            Bundle data = new Bundle();
            data.putInt(KEY_POSOTION, position);
            msg.setData(data);

            mLocalHandler.sendMessage(msg);
        }

        @Override
        public void onPause() {
            log("onPause");

            mLocalHandler.sendEmptyMessage(MESSAGE_ON_PAUSE);
        }

        @Override
        public void onCompletion() {
            log("onCompletion");

            mLocalHandler.sendEmptyMessage(MESSAGE_ON_COMPLETION);
        }

        @Override
        public void onPrev(MusicPlayerItem item, int itemIndex) {
            log("onPrev");

            Message msg = mLocalHandler.obtainMessage(MESSAGE_ON_PRE);
            Bundle data = new Bundle();
            data.putParcelable(KEY_ITEM, item);
            data.putInt(KEY_INDEX, itemIndex);
            msg.setData(data);

            mLocalHandler.sendMessage(msg);
        }

        @Override
        public void onNext(MusicPlayerItem item, int itemIndex) {
            log("onNext");

            Message msg = mLocalHandler.obtainMessage(MESSAGE_ON_NEXT);
            Bundle data = new Bundle();
            data.putParcelable(KEY_ITEM, item);
            data.putInt(KEY_INDEX, itemIndex);
            msg.setData(data);

            mLocalHandler.sendMessage(msg);
        }

        /**
         * 准备播放
         */
        @Override
        public void onPrePlaying(MusicPlayerItem item, int itemIndex) {
            log("onPrePlaying");

            Message msg = mLocalHandler.obtainMessage(MESSAGE_ON_PRE_PLAYING);
            Bundle data = new Bundle();
            data.putParcelable(KEY_ITEM, item);
            data.putInt(KEY_INDEX, itemIndex);
            msg.setData(data);

            mLocalHandler.sendMessage(msg);
        }

        /**
         * 开始播放
         */
        @Override
        public void onStartPlaying(MusicPlayerItem item) {
            log("onStartPlaying");

            Message msg = mLocalHandler.obtainMessage(MESSAGE_ON_START_PLAYING);
            Bundle data = new Bundle();
            data.putParcelable(KEY_ITEM, item);
            msg.setData(data);

            mLocalHandler.sendMessage(msg);
        }

        @Override
        public void onUpdatePlayingProgress(MusicPlayerItem item, int itemIndex, int position) {
            // log("onUpdatePlayingProgress,item=" + item.getMusicId() + "," +
            // item.getName()
            // + ",position=" + position);

            Message msg = mLocalHandler.obtainMessage(MESSAGE_ON_UPDATE_PLAYING_PROGRESS);
            Bundle data = new Bundle();
            data.putParcelable(KEY_ITEM, item);
            data.putInt(KEY_INDEX, itemIndex);
            data.putInt(KEY_POSOTION, position);
            msg.setData(data);

            mLocalHandler.sendMessage(msg);
        }

        @Override
        public void onUpdateStepCount(int stepCount) throws RemoteException {
            Message msg = mLocalHandler.obtainMessage(MESSAGE_UPDATE_STEP_COUNT);
            Bundle data = new Bundle();
            data.putInt(KEY_COUNT, stepCount);
            msg.setData(data);

            mLocalHandler.sendMessage(msg);

        }

        @Override
        public void onUpdateStepFrequency(float freq, float avgFreq) throws RemoteException {
            Message msg = mLocalHandler.obtainMessage(MESSAGE_UPDATE_STEP_FREQUENCY);
            Bundle data = new Bundle();
            data.putFloat(KEY_FREQ, freq);
            data.putFloat(KEY_AVG_FREQ, avgFreq);
            msg.setData(data);

            mLocalHandler.sendMessage(msg);
        }

        @Override
        public void onShowMessage(MusicPlayerItem item, int type, String message) {
            Message msg = mLocalHandler.obtainMessage(MESSAGE_ON_SHOW_MESSAGE);
            Bundle data = new Bundle();
            data.putParcelable(KEY_ITEM, item);
            data.putInt(KEY_TYPE, type);
            data.putString(KEY_MESSAGE, message);
            msg.setData(data);

            mLocalHandler.sendMessage(msg);

        }

        @Override
        public void onVideoSizeChanged(int width, int height) throws RemoteException {
            log("onVideoSizeChanged ,width=" + width + ",height=" + height);

            Message msg = mLocalHandler.obtainMessage(MESSAGE_ON_VIDEO_SIZE_CHANGED);
            Bundle data = new Bundle();
            data.putInt(KEY_WIDTH, width);
            data.putInt(KEY_HEIGHT, width);
            msg.setData(data);

            mLocalHandler.sendMessage(msg);
        }

        @Override
        public void onSeekComplete() throws RemoteException {
            log("onSeekComplete");

            mLocalHandler.sendEmptyMessage(MESSAGE_ON_SEEK_COMPLETE);
        }

        @Override
        public boolean onInfo(int what, int extra) throws RemoteException {
            log("onInfo" + ",what=" + what + ",extra=" + extra);

            Message msg = mLocalHandler.obtainMessage(MESSAGE_ON_INFO);
            Bundle data = new Bundle();
            data.putInt(KEY_WHAT, what);
            data.putInt(KEY_EXTRA, extra);
            msg.setData(data);

            mLocalHandler.sendMessage(msg);

            return false;
        }

        @Override
        public void onPrepared() throws RemoteException {
            log("onPrepared");

            mLocalHandler.sendEmptyMessage(MESSAGE_ON_PREPARED);
        }

        @Override
        public boolean onError(int what, int extra) throws RemoteException {
            log("onError" + ",what=" + what + ",extra=" + extra);

            Message msg = mLocalHandler.obtainMessage(MESSAGE_ON_ERROR);
            Bundle data = new Bundle();
            data.putInt(KEY_WHAT, what);
            data.putInt(KEY_EXTRA, extra);
            msg.setData(data);

            mLocalHandler.sendMessage(msg);

            return false;
        }

        @Override
        public void onBufferingUpdate(int percent) throws RemoteException {
            // log("onBufferingUpdate" + ",percent=" + percent);

            Message msg = mLocalHandler.obtainMessage(MESSAGE_ON_BUFFERING_UPDATE);
            Bundle data = new Bundle();
            data.putInt(KEY_PERCENT, percent);
            msg.setData(data);

            mLocalHandler.sendMessage(msg);

        }

        @Override
        public String getListenerTag() throws RemoteException {
            return MusicPlayerMainView.class.getSimpleName();
        }

        @Override
        public MusicPlayerConfig getPlayerConfig() throws RemoteException {
            if (null != mPlayerUIInterface) {
                return mPlayerUIInterface.getPlayerConfig();
            } else {
                return null;
            }
        }

        @Override
        public void onPlayerListChange() throws RemoteException {
            mLocalHandler.sendEmptyMessage(MESSAGE_ON_PLAYERLIST_CHANGE);
        }

        @Override
        public void onSavePlayingProgress(MusicPlayerItem item) throws RemoteException {
            Message msg = mLocalHandler.obtainMessage(MESSAGE_ON_SAVE_PLAYING_PROGRESS);
            Bundle data = new Bundle();
            data.putParcelable(KEY_ITEM, item);
            msg.setData(data);

            mLocalHandler.sendMessage(msg);
        }

    };

    private Handler mLocalHandler = new Handler() {
        @SuppressWarnings("unused")
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            int what = msg.what;

            switch (what) {
                case MESSAGE_ON_STOP: {
                    updatePlayingStatus(STATUS_STOPPED);
                    dismissWaitingDialog();

                    break;
                }

                case MESSAGE_ON_START: {
                    updatePlayingStatus(STATUS_STARTED);

                    break;
                }

                case MESSAGE_ON_SEEK_TO: {
                    Bundle data = msg.getData();
                    int position = data.getInt(KEY_POSOTION);
                    updatePlayingProgress(position);

                    break;
                }

                case MESSAGE_ON_PAUSE: {
                    updatePlayingStatus(STATUS_PAUSED);

                    break;
                }

                case MESSAGE_ON_COMPLETION: {
                    updatePlayingStatus(STATUS_STOPPED);

                    break;
                }

                case MESSAGE_ON_PRE: {
                    Bundle data = msg.getData();
                    MusicPlayerItem item = data.getParcelable(KEY_ITEM);
                    int itemIndex = data.getInt(KEY_INDEX);
                    updatePlayingChapterName(item, itemIndex);

                    if (null != item) {
                        updateOrderView(item.isNeedOrder());
                    }

                    break;
                }

                case MESSAGE_ON_NEXT: {
                    Bundle data = msg.getData();
                    MusicPlayerItem item = data.getParcelable(KEY_ITEM);
                    int itemIndex = data.getInt(KEY_INDEX);
                    updatePlayingChapterName(item, itemIndex);

                    if (null != item) {
                        updateOrderView(item.isNeedOrder());
                    }

                    break;
                }

                case MESSAGE_ON_PRE_PLAYING: {
                    Bundle data = msg.getData();
                    MusicPlayerItem item = data.getParcelable(KEY_ITEM);
                    int itemIndex = data.getInt(KEY_INDEX);
                    updatePlayingChapterName(item, itemIndex);
                    updateTotalTime(item);
                    updatePlayingProgress(item.getPosition());

                    mSeekBar.setEnabled(false);
                    // 准备播放
                    showWaitingDialog();
                    updateBufferProgress(0);
                    updatePlayingStatus(STATUS_STARTED);

                    if (START_DOWNLOAD_WHEN_START_PLAYER) {
                        // 开始播放时如果没有下载,启动下载
                        if (!PlayerUtils.isFileExist(item)
                                && Utils.isNetworkAvailable(getContext())
                                && Utils.isWifiActive(mContext)
                                ) {
                            PlayerUtils.download(musicAlbumObject.getListId(), item.getMusicId(),
                                    item.getUrl(), item.getFileSize());
                        }

                    }

                    if (null != item) {
                        updateOrderView(item.isNeedOrder());
                    }

                    break;
                }

                case MESSAGE_ON_START_PLAYING: {
                    Bundle data = msg.getData();
                    MusicPlayerItem item = data.getParcelable(KEY_ITEM);


                    mSeekBar.setEnabled(true);
                    // 开始播放
                    dismissWaitingDialog();

                    if (item.isLocalFileType()) {
                    } else {
                    }

                    break;
                }

                case MESSAGE_ON_UPDATE_PLAYING_PROGRESS: {
                    Bundle data = msg.getData();
                    MusicPlayerItem item = data.getParcelable(KEY_ITEM);
                    int itemIndex = data.getInt(KEY_INDEX);
                    int position = data.getInt(KEY_POSOTION);

                    updatePlayingProgress(position);

                    break;
                }

                case MESSAGE_UPDATE_STEP_FREQUENCY: {
                    Bundle data = msg.getData();
                    float freq = data.getFloat(KEY_FREQ);
                    float avgFreq = data.getFloat(KEY_AVG_FREQ);

                    if (null != mPlayerUIInterface) {
                        mPlayerUIInterface.onUpdateStepReq(freq, avgFreq);
                    }

                    break;
                }

                case MESSAGE_UPDATE_STEP_COUNT: {
                    Bundle data = msg.getData();
                    int stepCount = data.getInt(KEY_COUNT);

                    if (null != mPlayerUIInterface) {
                        mPlayerUIInterface.onUpdateStepCount(stepCount);
                    }

                    break;
                }

                case MESSAGE_ON_SHOW_MESSAGE: {
                    Bundle data = msg.getData();
                    MusicPlayerItem item = data.getParcelable(KEY_ITEM);
                    int type = data.getInt(KEY_TYPE);
                    String message = data.getString(KEY_MESSAGE);
                    if (null != mPlayerUIInterface) {
                        mPlayerUIInterface.onShowMessage(item, type, message);
                    }

                    break;
                }

                case MESSAGE_ON_PREPARED: {
                    log("MESSAGE_ON_PREPARED");
                    updateTotalTime(getCurrentPlayingItem());
                    updateBufferProgress(0);

                    break;
                }

                case MESSAGE_ON_BUFFERING_UPDATE: {
                    Bundle data = msg.getData();
                    int percent = data.getInt(KEY_PERCENT);

                    updateBufferProgress(percent);

                    break;
                }

                case MESSAGE_ON_VIDEO_SIZE_CHANGED: {
                    Bundle data = msg.getData();
                    int width = data.getInt(KEY_WIDTH);
                    int height = data.getInt(KEY_HEIGHT);

                    break;
                }

                case MESSAGE_ON_SEEK_COMPLETE: {

                    break;
                }

                case MESSAGE_ON_INFO: {
                    Bundle data = msg.getData();
                    int whatInt = data.getInt(KEY_WHAT);
                    int extra = data.getInt(KEY_EXTRA);

                    break;
                }

                case MESSAGE_ON_ERROR: {
                    Bundle data = msg.getData();
                    int whatInt = data.getInt(KEY_WHAT);
                    int extra = data.getInt(KEY_EXTRA);

                    if (whatInt == 1 && extra == -1004) {
                        ToastUtil.showToast(mContext,
                                mContext.getString(R.string.canot_play_error));
                    }

                    break;
                }

                case MESSAGE_ON_PLAYERLIST_CHANGE: {
                    // 播放列表更新
                    MusicPlayerItem item = getCurrentPlayingItem();
                    int index = getCurrentPlayingIndex();
                    updatePlayingChapterName(item, index);

                    if (null != item) {
                        updateOrderView(item.isNeedOrder());
                    }

                    break;
                }

                case MESSAGE_ON_SAVE_PLAYING_PROGRESS: {
                    Bundle data = msg.getData();
                    MusicPlayerItem item = data.getParcelable(KEY_ITEM);

                    break;
                }

                case MESSAGE_SET_LISTENER: {
                    setPlayerListener();

                    break;
                }

                default:
                    break;
            }
        }

    };

    private void showWaitingDialog() {
        mProBarlarge.setVisibility(View.VISIBLE);
        mPlayorpauseIv.setVisibility(View.INVISIBLE);
    }

    private void dismissWaitingDialog() {
        mProBarlarge.setVisibility(View.GONE);
        mPlayorpauseIv.setVisibility(View.VISIBLE);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();

        if (id == R.id.mp3down) {
            onAutoPlayingStatusInterrupted();
            if (null != mIMusicPlayerService) {
                try {
                    if (mIMusicPlayerService.canForward()) {
                        boolean success = mIMusicPlayerService.playNextItem();

                        if (!success) {
                            // showToast("Can't play this one.");
                        }
                    } else {
                        showToast(mContext.getString(R.string.reach_last_item));
                    }
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }

            // 播放前一首
        } else if (id == R.id.mp3up) {
            onAutoPlayingStatusInterrupted();
            if (null != mIMusicPlayerService) {
                try {
                    if (mIMusicPlayerService.canGoBack()) {
                        boolean success = mIMusicPlayerService.playPrevItem();
                        if (!success) {
                            // showToast("Can't play this one.");
                        }
                    } else {
                        showToast(mContext.getString(R.string.reach_first_item));
                    }
                } catch (RemoteException e) {
                    e.printStackTrace();
                }

            }

            // 暂停/播放
        } else if (id == R.id.playorpause) {
            onAutoPlayingStatusInterrupted();
            if (null != mIMusicPlayerService) {
                try {
                    int status = mIMusicPlayerService.getPlayerPlayingStatus();
                    // 如果當前都沒有MediaPlayer实例，说明当前还从没开始播放，直接调用开始播放，默认会播放当前的那个item.
                    // 如果当前播放器为空，那么直接启动播放器
                    if (mIMusicPlayerService.isMediaPlayerStatusInvalid()
                            || STATUS_STOPPED == status) {
                        MusicPlayerItem item = getCurrentPlayingItem();
                        boolean isHandled = prePlayHandle(item);
                        if (isHandled) {
                            return;
                        }

                        boolean success = mIMusicPlayerService.start();
                        if (success) {
                            // 这里可能会失败
                            updatePlayingStatus(STATUS_STARTED);
                        } else {
                            if (null != item) {
                                if (item.isNeedOrder()) {

                                }
                            }
                        }

                        return;
                    }

                } catch (RemoteException e) {
                    e.printStackTrace();

                    return;
                }

                try {
                    boolean isPrepared = mIMusicPlayerService.isPlayerPrepared();

                    if (!isPrepared) {
                        showToast(mContext.getString(R.string.it_is_preparing_now_please_wait));

                        return;
                    }
                } catch (RemoteException e) {
                    e.printStackTrace();
                }

                try {
                    if (mIMusicPlayerService.isPlaying()) {
                        boolean success = mIMusicPlayerService.pause();
                        if (success) {
                            updatePlayingStatus(STATUS_PAUSED);
                        }

                        return;
                    } else {
                        MusicPlayerItem item = getCurrentPlayingItem();
                        // 开始播放前需要做一序列的条件判断
                        boolean isHandled = prePlayHandle(item);
                        if (isHandled) {
                            return;
                        }

                        boolean success = mIMusicPlayerService.start();
                        if (success) {
                            updatePlayingStatus(STATUS_STARTED);
                        } else {

                        }
                    }
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        } else {
        }
    }

    private boolean prePlayHandle(MusicPlayerItem item) {
        if (null != item) {
            if (!item.isNeedOrder() && TextUtils.isEmpty(item.getUrl())) {
                // 播放链接为空,同步链接
                showToast(mContext.getString(R.string.sync_auth_list_please_wait));

                return true;
            } else {

            }
        }

        return false;
    }

    private void bindPlayerService() {
        Intent intent = new Intent();
        intent.setAction(BIND_ACTION);
        mContext.bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);
    }

    private void log(String message) {
        if (!DEBUG_LOG) {
            return;
        }

        if (null != message) {
            Log.i(TAG, message);
        }
    }

    private ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceDisconnected(ComponentName name) {
            log("onServiceDisconnected");
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            log("onServiceConnected");

            mIMusicPlayerService = IMusicPlayerService.Stub.asInterface(service);

            if (null != mIMusicPlayerService) {
                MusicPlayerItem item = getCurrentPlayingItem();
                if (null != item) {
                    log("recovery position:" + item.getPosition() + ",total time:"
                            + getCurrentPlayingTotalTime());
                    updateTotalTime(item);
                    updatePlayingProgress(item.getPosition());
                    updatePlayingChapterName(item, getCurrentPlayingIndex());
                } else {

                }

                try {
                    updatePlayingStatus(mIMusicPlayerService.getPlayerPlayingStatus());
                } catch (RemoteException e) {
                    e.printStackTrace();
                }

                setPlayerListener();

                onConnected();
            }
        }
    };

    private void onConnected() {
        if (null != musicList) {
            PlayerUtils.appendList2(getContext(), musicList);
            PlayerUtils.updateMusicListObject(getContext(), musicAlbumObject);
        }
    }

    int mSetListenerCount = 0;

    private void setPlayerListener() {
        if (null == mPlayerListener) {
            return;
        }

        try {
            mSetListenerCount++;
            // mListenerTime = System.currentTimeMillis();

            log("setPlayerListener,listener=" + mPlayerListener.getListenerTag());
            boolean success = mIMusicPlayerService.setPlayerListener(mPlayerListener);

            if (!success && mSetListenerCount <= 3) {
                mLocalHandler.sendEmptyMessageDelayed(MESSAGE_SET_LISTENER, SET_LISTENER_STEP
                        * mSetListenerCount);
            }

        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private MusicPlayerItem getCurrentPlayingItem() {
        if (null != mIMusicPlayerService) {
            try {
                return mIMusicPlayerService.getCurrentPlayingItem();
            } catch (RemoteException e) {
                e.printStackTrace();

                return null;
            }
        } else {
            return null;
        }
    }

    private int getCurrentPlayingIndex() {
        if (null != mIMusicPlayerService) {
            try {
                return mIMusicPlayerService.getCurrentPlayingIndex();
            } catch (RemoteException e) {
                e.printStackTrace();

                return 0;
            }
        } else {
            return 0;
        }
    }

    private List<MusicPlayerItem> getPlayerList() {
        if (null != mIMusicPlayerService) {
            try {
                return mIMusicPlayerService.getPlayerList();
            } catch (RemoteException e) {
                e.printStackTrace();

                return null;
            }
        } else {
            return null;
        }
    }

    private int getPlayerListCount() {
        List<MusicPlayerItem> musicPlayerItems = getPlayerList();
        if (null != musicPlayerItems) {
            return musicPlayerItems.size();
        } else {
            return 0;
        }
    }

    /**
     * 在启动时，默认会先跑到INVISIBLE，再跑到VISIBLE，所以会错误的造成选弹出notification,再消失的情况
     */
    private boolean mAreadyInitVisible = false;

    @Override
    protected void onVisibilityChanged(View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);

        switch (visibility) {
            case View.VISIBLE: {
                PlayerUtils.dismissNotification(mContext);
                log("onVisibilityChanged,VISIBLE");
                mAreadyInitVisible = true;


                break;
            }

            case View.INVISIBLE: {
                log("onVisibilityChanged,INVISIBLE");
                if (mAreadyInitVisible) {
                    PlayerUtils.showNotification(mContext);
                }

                break;
            }

            default:
                log("onVisibilityChanged,DEFAULT");
                break;
        }
    }

    private void updateTotalTime(MusicPlayerItem item) {
        int playzongtime1 = -1;
        if (null != item) {
            playzongtime1 = item.getTime();
        } else {
            playzongtime1 = getCurrentPlayingTotalTime();
        }

        String timeString = PlayerUtils.getTimeReadable(playzongtime1);
        mPlayzongTimeTv.setText(timeString);
        mSeekBar.setMax(playzongtime1);

        log("updateTotalTime,total=" + playzongtime1);
    }

    private void updatePlayingChapterName(MusicPlayerItem item, int itemIndex) {
        int playerListCount = getPlayerListCount();
        if (null != item) {
            log("updatePlayingChapterName,item=" + item);
            ToastUtil.showToast(getContext(), item.getName());

            updateTotalTime(item);
        } else {

        }
    }

    /**
     * 更新播放进度
     *
     * @param current
     */
    private void updatePlayingProgress(int current) {

        int totalTime = getCurrentPlayingTotalTime();

        String timeString = PlayerUtils.getTimeReadable(current);

        // log("updatePlayingProgress,current=" + timeString);

        if (totalTime > 0) {
            mSeekBar.setProgress(current);
        }

        // 如果在托动状态，不需要更新
        if (!mIsInSeekMode) {
            mPlayingTimeTv.setText(timeString);
        }
    }

    /**
     * 快速拖进时，更新播放时间
     *
     * @param current
     */
    private void updatePlayingProgressTime(int current) {
        log("updatePlayingProgressTime,current=" + current);

        String timeString = PlayerUtils.getTimeReadable(current);

        mPlayingTimeTv.setText(timeString);
    }

    /**
     * 更新缓冲进度
     *
     * @param bufferPercent
     */
    public void updateBufferProgress(int bufferPercent) {
        log("updateBufferProgress,bufferPercent=" + bufferPercent + ",Indeterminate=");
        // + mSeekBar.isIndeterminate());
        // mSeekBar.setIndeterminate(true);
        int bufferPosition = bufferPercent * getCurrentPlayingTotalTime() / 100;
        // mSeekBar.postInvalidate();
        // log("updateBufferProgress,bufferPercent=" + bufferPercent +
        // ",Indeterminate="
        // + mSeekBar.isIndeterminate());
        mSeekBar.setSecondaryProgress(bufferPosition);
    }

    private int getCurrentPlayingTotalTime() {
        if (null != mIMusicPlayerService) {
            try {
                return mIMusicPlayerService.getCurrentPlayingTotalTime();
            } catch (RemoteException e) {
                e.printStackTrace();

                return ENVALID_TIME_DURATION;
            }
        } else {
            return ENVALID_TIME_DURATION;
        }
    }

    public void updatePlayingStatus(int status) {
        switch (status) {
            case STATUS_STARTED:
                mPlayorpauseIv.setImageResource(R.drawable.musicplayer_pause);
                initAnimation(true);

                break;
            case STATUS_PAUSED:
                mPlayorpauseIv.setImageResource(R.drawable.musicplayer_play);
                initAnimation(false);
                break;
            case STATUS_STOPPED:
                mSeekBar.setProgress(0);
                mPlayingTimeTv.setText("00:00");
                mPlayorpauseIv.setImageResource(R.drawable.musicplayer_play);
                initAnimation(false);
                break;

            default:
                break;
        }
    }

    private void updateOrderView(boolean isNeedOrder) {
    }

    private void showToast(String message) {
        ToastUtil.showToast(mContext, message);
    }

    /**
     * 自动播放状态被中断
     */
    private void onAutoPlayingStatusInterrupted() {
//        if (null != mTingshuFragmentBridgeListener) {
//            mTingshuFragmentBridgeListener
//                    .onAutoPlayingStatusInterrupted(PlayerConstants.TINGSHU_SOURCE_PLAYER_MAIN_VIEW);
//        }
    }

    MusicAlbumObject musicAlbumObject;
    ArrayList<MusicPlayerItem> musicList;

    public void setMusicList(MusicAlbumObject musicAlbumObject) {
        this.musicAlbumObject = musicAlbumObject;
    }

    public void setMusicList(ArrayList<MusicPlayerItem> musicList) {
        this.musicList = musicList;
    }
}
