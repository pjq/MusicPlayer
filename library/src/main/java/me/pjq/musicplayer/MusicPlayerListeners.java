
package me.pjq.musicplayer;

import android.content.Context;
import android.os.RemoteException;
import android.util.Log;

import java.util.ArrayList;
import java.util.Vector;

import me.pjq.musicplayer.utils.Utils;

public class MusicPlayerListeners {
    private Vector<IMusicPlayerListener> mIPlayerListeners;

    public MusicPlayerListeners() {
        mIPlayerListeners = new Vector<IMusicPlayerListener>();
    }

    public synchronized boolean setPlayerListener(IMusicPlayerListener listener) {
        if (null == listener) {
            return false;
        }

        try {
            int index = contains(listener);

            if (index < 0) {
                mIPlayerListeners.add(listener);
                Utils.i(
                        "PlayerListeners",
                        "setPlayerListener,add listener success, listener="
                                + listener.getListenerTag());
                return true;
            } else {
                mIPlayerListeners.remove(index);
                mIPlayerListeners.add(listener);

                Utils.i("PlayerListeners",
                        "setPlayerListener,already exists, listener=" + listener.getListenerTag());

                return false;
            }

        } catch (Exception e) {
            e.printStackTrace();

            return false;
        }
    }

    private int contains(IMusicPlayerListener listener) {
        int size = mIPlayerListeners.size();

        for (int i = 0; i < size; i++) {
            IMusicPlayerListener item = mIPlayerListeners.get(i);
            try {
                if (item.getListenerTag().equals(listener.getListenerTag())) {
                    return i;
                }

            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        return -1;
    }

    private ArrayList<IMusicPlayerListener> containsCount(IMusicPlayerListener listener) {
        int size = mIPlayerListeners.size();

        ArrayList<IMusicPlayerListener> indexList = new ArrayList<IMusicPlayerListener>();
        for (int i = 0; i < size; i++) {
            IMusicPlayerListener item = mIPlayerListeners.get(i);
            try {
                if (item.getListenerTag().equals(listener.getListenerTag())) {

                    indexList.add(item);
                }

            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        return indexList;
    }

    /**
     * 注销IPlayerListener,做一便特殊处理，如果，当前List只包含一个，则不注销
     *
     * @param listener
     * @return
     */
    public synchronized boolean unSetPlayerListener(IMusicPlayerListener listener) {
        if (null == listener) {
            return false;
        }

        try {
            ArrayList<IMusicPlayerListener> indexList = containsCount(listener);
            int count = indexList.size();

            if (count >= 1) {
                for (int i = 0; i < count; i++) {
                    mIPlayerListeners.remove(indexList.get(i));
                }

                Utils.i("PlayerListeners",
                        "unSetPlayerListener,remove success, listener="
                                + listener.getListenerTag());

                return true;
            } else {
                Utils.i(
                        "PlayerListeners",
                        "unSetPlayerListener,remove failed,count<=1, listener="
                                + listener.getListenerTag());

                return false;
            }

        } catch (Exception e) {
            return false;
        }
    }

    public void onVideoSizeChanged(int width, int height) throws RemoteException {
        int size = mIPlayerListeners.size();

        for (int i = 0; i < size; i++) {
            IMusicPlayerListener listener = mIPlayerListeners.get(i);

            try {
                listener.onVideoSizeChanged(width, height);
            } catch (RemoteException e) {
                e.printStackTrace();
                size = remoteExceptionHandle(listener);
            }

        }
    }

    public void onSeekComplete() throws RemoteException {
        int size = mIPlayerListeners.size();

        for (int i = 0; i < size; i++) {
            IMusicPlayerListener listener = mIPlayerListeners.get(i);

            try {
                listener.onSeekComplete();
            } catch (RemoteException e) {
                e.printStackTrace();
                size = remoteExceptionHandle(listener);
            }
        }
    }

    public boolean onInfo(int what, int extra) throws RemoteException {

        int size = mIPlayerListeners.size();

        for (int i = 0; i < size; i++) {
            IMusicPlayerListener listener = mIPlayerListeners.get(i);

            try {
                listener.onInfo(what, extra);
            } catch (RemoteException e) {
                e.printStackTrace();
                size = remoteExceptionHandle(listener);
            }
        }

        return false;
    }

    public void onCompletion() throws RemoteException {
        int size = mIPlayerListeners.size();

        for (int i = 0; i < size; i++) {
            IMusicPlayerListener listener = mIPlayerListeners.get(i);

            try {
                listener.onCompletion();
            } catch (RemoteException e) {
                e.printStackTrace();
                size = remoteExceptionHandle(listener);
            }
        }
    }

    public void onPrepared() throws RemoteException {
        int size = mIPlayerListeners.size();

        for (int i = 0; i < size; i++) {
            IMusicPlayerListener listener = mIPlayerListeners.get(i);

            try {
                listener.onPrepared();
            } catch (RemoteException e) {
                e.printStackTrace();
                size = remoteExceptionHandle(listener);
            }
        }
    }

    public boolean onError(int what, int extra) throws RemoteException {
        int size = mIPlayerListeners.size();

        for (int i = 0; i < size; i++) {
            IMusicPlayerListener listener = mIPlayerListeners.get(i);

            try {
                listener.onError(what, extra);
            } catch (RemoteException e) {
                e.printStackTrace();
                size = remoteExceptionHandle(listener);
            }
        }

        return false;
    }

    public void onBufferingUpdate(int percent) throws RemoteException {
        int size = mIPlayerListeners.size();

        for (int i = 0; i < size; i++) {
            IMusicPlayerListener listener = mIPlayerListeners.get(i);

            try {
                listener.onBufferingUpdate(percent);
            } catch (RemoteException e) {
                e.printStackTrace();
                size = remoteExceptionHandle(listener);
            }
        }

    }

    public void onStop() throws RemoteException {
        int size = mIPlayerListeners.size();

        for (int i = 0; i < size; i++) {
            IMusicPlayerListener listener = mIPlayerListeners.get(i);

            try {
                listener.onStop();
            } catch (RemoteException e) {
                e.printStackTrace();
                size = remoteExceptionHandle(listener);
            }
        }

    }

    public void onPause() throws RemoteException {
        int size = mIPlayerListeners.size();

        for (int i = 0; i < size; i++) {
            IMusicPlayerListener listener = mIPlayerListeners.get(i);

            try {
                listener.onPause();
            } catch (RemoteException e) {
                e.printStackTrace();
                size = remoteExceptionHandle(listener);
            }
        }
    }

    public void onStart() throws RemoteException {
        int size = mIPlayerListeners.size();

        for (int i = 0; i < size; i++) {
            IMusicPlayerListener listener = mIPlayerListeners.get(i);

            try {
                listener.onStart();
            } catch (RemoteException e) {
                e.printStackTrace();
                size = remoteExceptionHandle(listener);
            }
        }
    }

    public void onSeekTo(int position) throws RemoteException {
        int size = mIPlayerListeners.size();

        for (int i = 0; i < size; i++) {
            IMusicPlayerListener listener = mIPlayerListeners.get(i);

            try {
                listener.onSeekTo(position);
            } catch (RemoteException e) {
                e.printStackTrace();
                size = remoteExceptionHandle(listener);
            }
        }
    }

    public void onPrev(MusicPlayerItem object, int itemIndex) throws RemoteException {
        int size = mIPlayerListeners.size();

        for (int i = 0; i < size; i++) {
            IMusicPlayerListener listener = mIPlayerListeners.get(i);

            try {
                listener.onPrev(object, itemIndex);
            } catch (RemoteException e) {
                e.printStackTrace();
                size = remoteExceptionHandle(listener);
            }
        }
    }

    public void onNext(MusicPlayerItem object, int itemIndex) throws RemoteException {
        int size = mIPlayerListeners.size();

        for (int i = 0; i < size; i++) {
            IMusicPlayerListener listener = mIPlayerListeners.get(i);

            try {
                listener.onNext(object, itemIndex);
            } catch (RemoteException e) {
                e.printStackTrace();
                size = remoteExceptionHandle(listener);
            }
        }

    }

    public void onPrePlaying(MusicPlayerItem item, int itemIndex) throws RemoteException {
        int size = mIPlayerListeners.size();

        for (int i = 0; i < size; i++) {
            IMusicPlayerListener listener = mIPlayerListeners.get(i);

            try {
                listener.onPrePlaying(item, itemIndex);
            } catch (RemoteException e) {
                e.printStackTrace();
                size = remoteExceptionHandle(listener);
            }
        }
    }

    public void onStartPlaying(MusicPlayerItem item) throws RemoteException {
        int size = mIPlayerListeners.size();

        for (int i = 0; i < size; i++) {
            IMusicPlayerListener listener = mIPlayerListeners.get(i);

            try {
                listener.onStartPlaying(item);
            } catch (RemoteException e) {
                e.printStackTrace();
                size = remoteExceptionHandle(listener);
            }
        }
    }

    public void onUpdatePlayingProgress(MusicPlayerItem item, int itemIndex, int position) {
        int size = mIPlayerListeners.size();

        for (int i = 0; i < size; i++) {
            IMusicPlayerListener listener = mIPlayerListeners.get(i);
            try {
                listener.onUpdatePlayingProgress(item, itemIndex, position);
            } catch (RemoteException e) {
                e.printStackTrace();
                size = remoteExceptionHandle(listener);
            }
        }
    }

    public void onUpdateStepCount(int stepCount) {
        int size = mIPlayerListeners.size();

        for (int i = 0; i < size; i++) {
            IMusicPlayerListener listener = mIPlayerListeners.get(i);
            try {
                listener.onUpdateStepCount(stepCount);
            } catch (RemoteException e) {
                e.printStackTrace();
                size = remoteExceptionHandle(listener);
            }
        }
    }

    public void onUpdateStepFreq(float freq, float avgFreq) {
        int size = mIPlayerListeners.size();

        for (int i = 0; i < size; i++) {
            IMusicPlayerListener listener = mIPlayerListeners.get(i);
            try {
                listener.onUpdateStepFrequency(freq, avgFreq);
            } catch (RemoteException e) {
                e.printStackTrace();
                size = remoteExceptionHandle(listener);
            }
        }
    }

    private int remoteExceptionHandle(IMusicPlayerListener listener) {
        mIPlayerListeners.remove(listener);
        listener = null;
        return mIPlayerListeners.size();
    }

    public void onShowMessage(MusicPlayerItem item, int type, String message) throws RemoteException {
        int size = mIPlayerListeners.size();

        for (int i = 0; i < size; i++) {
            IMusicPlayerListener listener = mIPlayerListeners.get(i);
            try {
                listener.onShowMessage(item, type, message);
            } catch (RemoteException e) {
                e.printStackTrace();
                size = remoteExceptionHandle(listener);
            }

        }
    }

    public MusicPlayerConfig getPlayerConfig() {
        int size = mIPlayerListeners.size();

        MusicPlayerConfig config = null;
        for (int i = 0; i < size; i++) {
            IMusicPlayerListener listener = null;
            try {
                listener = mIPlayerListeners.get(i);
                config = listener.getPlayerConfig();
                if (null != config) {
                    break;
                }
            } catch (RemoteException e) {
                e.printStackTrace();
                size = remoteExceptionHandle(listener);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return config;
    }

    /**
     * 播放列表改变
     *
     * @throws android.os.RemoteException
     */
    public void onPlayerListChange() {
        int size = mIPlayerListeners.size();

        for (int i = 0; i < size; i++) {
            IMusicPlayerListener listener = mIPlayerListeners.get(i);
            try {
                listener.onPlayerListChange();
            } catch (RemoteException e) {
                e.printStackTrace();
                size = remoteExceptionHandle(listener);
            }
        }
    }

    /**
     * 保存当前播放进度到数据库
     */
    public void onSavePlayingProgress(Context context, MusicPlayerItem item, MusicAlbumObject musicList, int totalCount,
                                      int index) {
        if (null == item) {
            return;
        }
        savePlayingStatus(context, item, musicList, totalCount, index);

        Log.i("PlayerListeners", "onSavePlayingProgress,item=" + item);

        int size = mIPlayerListeners.size();

        for (int i = 0; i < size; i++) {
            IMusicPlayerListener listener = null;
            try {
                listener = mIPlayerListeners.get(i);
                listener.onSavePlayingProgress(item);
            } catch (RemoteException e) {
                e.printStackTrace();
                size = remoteExceptionHandle(listener);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void savePlayingStatus(Context context, MusicPlayerItem item, MusicAlbumObject musicList, int totalCount,
                                  int index) {
        if (null == item) {
            return;
        }
    }

    public void clear() {
        mIPlayerListeners.clear();
        mIPlayerListeners = null;
    }
}
