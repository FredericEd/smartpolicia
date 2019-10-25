package com.smart.upolicia.Utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import android.content.Context
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessaging
import com.smart.upolicia.MainActivity
import com.smart.upolicia.R

class HeroMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "MyFirebaseMsgService"
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage?) {
        Log.wtf(TAG, "From: ${remoteMessage?.from}")
        remoteMessage?.data?.isNotEmpty()?.let {
            Log.wtf(TAG, "Message data payload: " + remoteMessage.data)
        }
        remoteMessage?.data?.let {
            /*it.forEach{
                Log.wtf(it.key, it.key)
            }*/
            sendNotification( it["title"]!!, it["body"]!!, it["nombre"]!!, it["imagen"]!!, it["latitud"]!!, it["longitud"]!!)
        }
    }

    override fun onNewToken(token: String?) {
        Log.wtf(TAG, "Refreshed token: $token")
        FirebaseMessaging.getInstance().subscribeToTopic(getString(R.string.default_notification_channel_id))
    }

    private fun sendNotification(messageTitle: String, messageBody: String, nombre: String, imagen: String, latitud: String, longitud: String) {

        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra("nombre", nombre)
        intent.putExtra("imagen", imagen)
        intent.putExtra("latitud", latitud)
        intent.putExtra("longitud", longitud)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(this, 0 /* Request code */, intent,
                    PendingIntent.FLAG_ONE_SHOT)

        val channelId = getString(R.string.default_notification_channel_id)
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(messageTitle)
                .setContentText(messageBody)
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setContentIntent(pendingIntent)
        val bigTextStyle = NotificationCompat.BigTextStyle()
        bigTextStyle.setBigContentTitle(messageTitle)
        bigTextStyle.bigText(messageBody)
        notificationBuilder.setStyle(bigTextStyle)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId,
                    "Channel human readable title",
                    NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }
        notificationManager.notify(0 /* ID of notification */, notificationBuilder.build())
    }
}