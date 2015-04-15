
package me.pjq.musicplayer;


import android.content.Context;
import android.media.AudioManager;

public class AudioFocusHelper implements AudioManager.OnAudioFocusChangeListener {
    AudioManager mAudioManager;

    Context mContext;

    MusicPlayerService.OnAudioFocusChangeListener mListener;

    // other fields here, you'll probably hold a reference to an interface
    // that you can use to communicate the focus changes to your Service

    public AudioFocusHelper(Context ctx, MusicPlayerService.OnAudioFocusChangeListener listener) {
        mContext = ctx;
        mListener = listener;
        mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
    }

    public boolean requestFocus() {
        return AudioManager.AUDIOFOCUS_REQUEST_GRANTED == mAudioManager.requestAudioFocus(this,
                AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
    }

    public boolean abandonFocus() {
        return AudioManager.AUDIOFOCUS_REQUEST_GRANTED == mAudioManager.abandonAudioFocus(this);
    }

    @Override
    public void onAudioFocusChange(int focusChange) {
        if (null != mListener) {
            mListener.onAudioFocusChange(focusChange);
        }
    }
}
