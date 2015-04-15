
package me.pjq.musicplayer.utils;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;

import me.pjq.musicplayer.MusicAlbumObject;
import me.pjq.musicplayer.MusicNotificationClassProvider;
import me.pjq.musicplayer.MusicPlayerConstants;
import me.pjq.musicplayer.R;

public class NotificationUtil {
    private static final String TAG = NotificationUtil.class.getSimpleName();

    private static MusicNotificationClassProvider notificationClassProvider;

    public static final int NOTIFICATION_ID = 1000;

    public static void setNotificationClassProvider(MusicNotificationClassProvider provider) {
        notificationClassProvider = provider;
    }

    public static PendingIntent createPendingIntent(Context context, MusicAlbumObject musicList) {
        PendingIntent pendingIntent = null;
        Intent intent = new Intent();
        Bundle bundle = new Bundle();
        bundle.putSerializable(MusicPlayerConstants.KEY_BOOK, musicList);
        intent.putExtras(bundle);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        if (null != notificationClassProvider) {
            intent.setClass(context, notificationClassProvider.getNotificationClass());
        }
        pendingIntent = PendingIntent.getActivity(context, NOTIFICATION_ID, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        return pendingIntent;
    }

    @SuppressWarnings("deprecation")
    public static void showNotification(Context context, String message, MusicAlbumObject musicList) {
        Utils.i(TAG,
                "showNotification,context=" + context + ",message=" + message + ",book=" + musicList);

        if (null == context || null == musicList) {
            return;
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
        builder.setAutoCancel(false);

        PendingIntent pi = createPendingIntent(context, musicList);

        builder.setContentIntent(pi);
        builder.setContentTitle(context.getString(R.string.app_name) + "(" + musicList.getListName() + ")");
        builder.setContentText(message);
        builder.setTicker(context.getString(R.string.app_name));

        builder.setSmallIcon(notificationClassProvider.getSmallIcon());

        builder.setWhen(System.currentTimeMillis());
        NotificationManager nm = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);

        Notification notification = builder.getNotification();
        notification.flags |= Notification.FLAG_NO_CLEAR;

        nm.notify(NOTIFICATION_ID, notification);

    }

    public static void dismissNotification(Context context) {
        NotificationManager nm = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);
        nm.cancel(NOTIFICATION_ID);

    }
}
