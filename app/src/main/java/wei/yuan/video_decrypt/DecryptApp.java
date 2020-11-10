package wei.yuan.video_decrypt;

import android.app.Application;

import com.arialyy.aria.core.Aria;

public class DecryptApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        Aria.init(this);
    }
}
