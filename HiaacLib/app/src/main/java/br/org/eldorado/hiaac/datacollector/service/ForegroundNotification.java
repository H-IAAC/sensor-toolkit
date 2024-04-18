package br.org.eldorado.hiaac.datacollector.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.net.Uri;

import androidx.core.app.NotificationCompat;

import br.org.eldorado.hiaac.R;

public class ForegroundNotification {
    public static final int NOTIFICATION_SERVICE_ID = (int) (System.currentTimeMillis() % Integer.MAX_VALUE);
    public static final String NOTIFICATION_CHANNEL_ID = "br.org.eldorado.hiaac.channelId";
    public static final String NOTIFICATION_CHANNEL_NAME = "hiaac.channel";
    public static final String NOTIFICATION_CHANNEL_DESCRIPTION = "Use to handle foreground service";

    public static Notification getNotification(Context context, String text) {
        return getNotification(context, "H-IAAC", text);
    }

    public static Notification getNotification(Context context, String title, String text) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID);

        return builder.setOngoing(true)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setOngoing(true)
                .setContentTitle(title)
                .setContentText(text)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .build();
    }

    /**
     * for API 26+ create notification channels
     */
    public static void createNotificationChannel(Context context) {
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationChannel mChannel = new NotificationChannel(NOTIFICATION_CHANNEL_ID,
                                                               ForegroundNotification.NOTIFICATION_CHANNEL_NAME,
                                                               NotificationManager.IMPORTANCE_HIGH);

        mChannel.setDescription(ForegroundNotification.NOTIFICATION_CHANNEL_DESCRIPTION);
        mChannel.setShowBadge(true);

        nm.createNotificationChannel(mChannel);
    }
}
