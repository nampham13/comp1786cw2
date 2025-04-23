package com.example.myapplication.cloud;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.example.myapplication.MainActivity;
import com.example.myapplication.R;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

/**
 * Service to handle Firebase Cloud Messaging notifications
 */
public class YogaMessagingService extends FirebaseMessagingService {
    private static final String TAG = "YogaMessagingService";
    
    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        Log.d(TAG, "From: " + remoteMessage.getFrom());
        
        // Check if message contains a notification payload
        if (remoteMessage.getNotification() != null) {
            Log.d(TAG, "Message Notification Body: " + remoteMessage.getNotification().getBody());
            sendNotification(
                    remoteMessage.getNotification().getTitle(),
                    remoteMessage.getNotification().getBody()
            );
        }
        
        // Check if message contains data payload
        if (remoteMessage.getData().size() > 0) {
            Log.d(TAG, "Message data payload: " + remoteMessage.getData());
            
            String title = remoteMessage.getData().get("title");
            String message = remoteMessage.getData().get("message");
            
            if (title != null && message != null) {
                sendNotification(title, message);
            }
        }
    }
    
    @Override
    public void onNewToken(@NonNull String token) {
        Log.d(TAG, "Refreshed token: " + token);
        
        // If you want to send messages to this application instance or
        // manage this app's subscriptions on the server side, send the
        // FCM registration token to your app server.
        sendRegistrationToServer(token);
    }
    
    /**
     * Send FCM registration token to app server
     * @param token FCM token
     */
    private void sendRegistrationToServer(String token) {
        // Implement this method to send token to your app server
        // This is where you would store the token in your backend
        Log.d(TAG, "sendRegistrationTokenToServer: " + token);
    }
    
    /**
     * Create and show a notification with the given title and message
     * @param title Notification title
     * @param messageBody Notification message
     */
    private void sendNotification(String title, String messageBody) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, intent,
                PendingIntent.FLAG_IMMUTABLE
        );
        
        String channelId = getString(R.string.default_notification_channel_id);
        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this, channelId)
                        .setSmallIcon(R.drawable.ic_notification)
                        .setContentTitle(title)
                        .setContentText(messageBody)
                        .setAutoCancel(true)
                        .setSound(defaultSoundUri)
                        .setContentIntent(pendingIntent);
        
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        
        // Since android Oreo notification channel is needed
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    "Yoga Studio Notifications",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            notificationManager.createNotificationChannel(channel);
        }
        
        notificationManager.notify(0, notificationBuilder.build());
    }
}