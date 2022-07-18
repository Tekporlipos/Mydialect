package gd.rf.tekporconsult.mypronouncer.service;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;
import androidx.annotation.RequiresApi;

public class App extends Application {
    public  static String OFFLINE_CHANNEL = "Offline Channel";
    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onCreate() {
        super.onCreate();
        NotificationChannel notificationChannel = new NotificationChannel(OFFLINE_CHANNEL,"Offline Notification", NotificationManager.IMPORTANCE_HIGH);
        notificationChannel.setDescription("This is the channel on which the download notification is listen on, Please feel free to disable it if you have you dictionary already offline");
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(notificationChannel);
    }
}