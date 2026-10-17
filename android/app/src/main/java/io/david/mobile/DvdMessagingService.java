package io.david.mobile;

import android.app.NotificationChannel;
import android.app.NotificationChannelGroup;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import io.david.mobile.data.NotificationPreferences;



/**
 * Local notification channels for david.io. There is no push backend in this fork:
 * Firebase Cloud Messaging was removed, so nothing here receives remote messages.
 */
public final class DvdMessagingService {
    public static final String ACTION_PUSH_RECEIVED = "io.david.mobile.PUSH_RECEIVED";
    public static final String CHANNEL_MESSAGES = "dvd_messages";
    public static final String CHANNEL_ALERTS = "dvd_alerts";
    public static final String CHANNEL_SOCIAL = "dvd_social";
    public static final String CHANNEL_RECEIPTS = "dvd_receipts";
    private static final String CHANNEL_GROUP = "dvd_notifications";
    private static final String SHADE_GROUP = "dvd";

    /** A low-priority device receipt, used only when backup work finishes off screen. */
    public static void showBackupReceipt(Context context, String title, String body) {
        if (!NotificationPreferences.isKindEnabled(context, "backup")) return;
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager == null) return;
        ensureChannels(context);
        Intent intent = new Intent(context, DvdActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra("dvd.kind", "backup");
        int id = 4999;
        PendingIntent pending = PendingIntent.getActivity(
                context, id, intent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationCompat.Builder notification = new NotificationCompat.Builder(context, CHANNEL_RECEIPTS)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                .setAutoCancel(true)
                .setGroup(SHADE_GROUP)
                .setContentIntent(pending);
        manager.notify(id, notification.build());
    }

    /** Creates the four user-controllable categories once during app startup. */
    public static void ensureChannels(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager == null) return;
        manager.createNotificationChannelGroup(new NotificationChannelGroup(
                CHANNEL_GROUP, "david.io"));
        createChannel(manager, CHANNEL_MESSAGES, "Messages and invites",
                NotificationManager.IMPORTANCE_HIGH);
        createChannel(manager, CHANNEL_ALERTS, "Alerts",
                NotificationManager.IMPORTANCE_HIGH);
        createChannel(manager, CHANNEL_SOCIAL, "Social and milestones",
                NotificationManager.IMPORTANCE_DEFAULT);
        createChannel(manager, CHANNEL_RECEIPTS, "Backup receipts",
                NotificationManager.IMPORTANCE_LOW);
    }

    private static void createChannel(NotificationManager manager, String id, String name,
                                      int importance) {
        NotificationChannel channel = new NotificationChannel(id, name, importance);
        channel.setGroup(CHANNEL_GROUP);
        manager.createNotificationChannel(channel);
    }
}
