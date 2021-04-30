package wei.yuan.video_decrypt.exoplayer;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.ConcatenatingMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.util.Util;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import wei.yuan.video_decrypt.R;
import wei.yuan.video_decrypt.exoplayer.custom.MyPlayerControlView;
import wei.yuan.video_decrypt.exoplayer.custom.MyPlayerView;
import wei.yuan.video_decrypt.m3u8server.M3u8Server;
import wei.yuan.video_decrypt.util.CommonUtil;
import wei.yuan.video_decrypt.util.ParseSystemUtil;

public class SeniorCustomViewActivity extends AppCompatActivity implements
        SimpleExoPlayer.EventListener, MyPlayerControlView.CustomEventListener,
        MyPlayerControlView.CustomOrientationListener {

    private static final String TAG = "SeniorViewActivity";
    private static final String DEFAULT_URL = "https://cdn.singsingenglish.com/new-sing/66c3d05eaa177e07d57465f948f0d8b934b7a7ba.mp4";
    private static final String SDCARD_DIR = Environment.getExternalStorageDirectory().getPath();
    private static final String DMM_DIR = SDCARD_DIR + File.separator + "dmm";

    private Context mContext;
    private SimpleExoPlayer mPlayer;
    private MyPlayerView mVideoView;

    private String m3u8Dir = "";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_exo_senior_view);
        mContext = getApplicationContext();

        Intent intent = getIntent();
        Bundle bundle = intent.getBundleExtra("Info");
        String dir = bundle.getString("directory");
        m3u8Dir = DMM_DIR + File.separator + dir;
        // 开启本地代理
        M3u8Server.execute();

        initView();
        initEvent();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mPlayer.release();
        M3u8Server.close();
    }

    @Override
    public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
        Log.i(TAG, "playWhenReady: " + playWhenReady + ", playbackState: " + playbackState);
        switch (playbackState) {
            case SimpleExoPlayer.STATE_BUFFERING:
                Log.v(TAG, "player buffering");
                break;
            case SimpleExoPlayer.STATE_READY:
                Log.v(TAG, "player ready");
                break;
            case SimpleExoPlayer.STATE_ENDED:
                Log.v(TAG, "player ended");
                break;
            case SimpleExoPlayer.STATE_IDLE:
                Log.v(TAG, "player idle");
                break;
            default:
                break;
        }
    }

    @Override
    public void onPlayerError(ExoPlaybackException error) {
        Log.e(TAG, "ExoPlaybackException: " + error);
    }

    @Override
    public void onBackClick(boolean isPortrait) {
        Log.i(TAG, "MyPlayerView onBackClick()");
        if (isPortrait) {
            this.finish();
        } else {
            showSystemStatusUI();
        }
    }

    @Override
    public void onBroadCastClick() {
        Log.i(TAG, "MyPlayerView onBroadCastClick()");
    }

    @Override
    public void onFillClick(boolean isPortrait) {
        Log.i(TAG, "MyPlayerView onFillClick()");
        if (isPortrait) {
            showSystemStatusUI();
        } else {
            hideSystemStatusUI();
        }
    }

    @Override
    public void onOrientationChanged(int orientation) {
        Log.i(TAG, "MyPlayerControlView onOrientationChanged()");
        if (orientation == OnOrientationChangedListener.SENSOR_PORTRAIT) {
            Log.v(TAG, "change to portrait");
            changeToPortrait();
        } else if (orientation == OnOrientationChangedListener.SENSOR_LANDSCAPE) {
            Log.v(TAG, "change to landscape");
            changeToLandscape();
        }
    }

    private void initView() {
        mPlayer = new SimpleExoPlayer.Builder(mContext).build();
        mPlayer.setPlayWhenReady(true);
        mVideoView = (MyPlayerView) findViewById(R.id.video_view);
        mVideoView.setPlayer(mPlayer);
        String url = createVideoPlayUrl();
        String playUrl = "";
        if (url.isEmpty()) {
            playUrl = DEFAULT_URL;
        } else {
            keyConvert(m3u8Dir);
            playUrl = String.format("http://127.0.0.1:%d%s", M3u8Server.PORT, url);
        }
        Uri uri = Uri.parse(playUrl);
        MediaSource mediaSource = buildMediaSource(uri);
        mPlayer.prepare(mediaSource);
    }

    private void initEvent() {
        mPlayer.addListener(this);
        mVideoView.controller.setCustomEventListener(this);
        mVideoView.controller.setCustomOrientationListener(this);
    }

    private MediaSource buildMediaSource(Uri uri) {
        if (uri == null || TextUtils.isEmpty(uri.toString())) {
            return null;
        }

        int type = Util.inferContentType(uri.getLastPathSegment());
        String userAgent = "user-agent";
        DataSource.Factory dataSourceFactory = null;
        switch (type) {
            case C.TYPE_HLS:
                // use the faked user agent when play hls
                userAgent = "Mozilla/5.0 (iPhone; CPU iPhone OS 11_0 like Mac OS X) " +
                        "AppleWebKit/604.1.38 (KHTML, like Gecko) " +
                        "Version/11.0 Mobile/15A372 Safari/604.1";
                dataSourceFactory = new DefaultHttpDataSourceFactory(userAgent);
                return new HlsMediaSource.Factory(dataSourceFactory).createMediaSource(uri);
            case C.TYPE_OTHER:
                dataSourceFactory = new DefaultHttpDataSourceFactory(userAgent);
                MediaSource mediaSource = new ProgressiveMediaSource.Factory(dataSourceFactory)
                        .createMediaSource(uri);
                return new ConcatenatingMediaSource(mediaSource);
            default:
                return null;
        }
    }

    private String createVideoPlayUrl() {
        String url = "";
        File dir = new File(m3u8Dir);
        File[] files = dir.listFiles();
        if (files == null || files.length == 0) {
            Log.v(TAG, dir.getPath() + " is empty");
            return "";
        }
        File m3u8 = new File(m3u8Dir + File.separator + "local.m3u8");
        if (m3u8.exists() && m3u8.isFile()) {
            Log.v(TAG, m3u8.getPath() + " exists");
            url = m3u8.getPath();
        } else {
            m3u8 = new File(m3u8Dir + File.separator + "1/local.m3u8");
            if (m3u8.exists() && m3u8.isFile()) {
                url = m3u8.getPath();
            } else {
                return "";
            }
        }

        return url;
    }

    private void keyConvert(String m3u8dir) {
        File keyFile = new File(m3u8dir + File.separator + "key.key");
        if (keyFile.exists()) {
            if (keyFile.length() == 16) {
                return;
            } else {
                showToastMsg(mContext, "error " + keyFile.getName());
            }
        }
        File keyOriginFile = new File(m3u8dir + File.separator + "key_o.key");
        if (keyOriginFile.exists()) {
            Log.d(TAG, keyOriginFile.getAbsolutePath() + " exists");
            if (keyOriginFile.length() != 32) {
                showToastMsg(mContext, "error " + keyOriginFile.getName());
                return;
            }
            try {
                String keyHexStr = getKeyHex(keyOriginFile);
                byte[] bytes = ParseSystemUtil.parseHexStr2Byte(keyHexStr);
                boolean isSuccess = CommonUtil.writeBinaryFile(bytes, keyFile, null);
                if (isSuccess) {
                    showToastMsg(mContext, "write " + keyFile.getName() + " success");
                } else {
                    showToastMsg(mContext, "write " + keyFile.getName() + " fail");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            Log.d(TAG, keyOriginFile.getAbsolutePath() + " doesn't exist");
        }
    }

    private void showToastMsg(Context context, String msg) {
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
    }

    private String getKeyHex(File keyFile) {
        try {
            FileInputStream fileInputStream = new FileInputStream(keyFile);
            InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream, "UTF-8");
            char[] input = new char[fileInputStream.available()];
            inputStreamReader.read(input);
            inputStreamReader.close();
            fileInputStream.close();
            return new String(input);
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    /**
     * Show system status UI when portrait
     */
    private void showSystemStatusUI() {
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
    }

    /**
     * Hide system status UI when landscape
     */
    private void hideSystemStatusUI() {
        WindowManager wm = (WindowManager) getApplicationContext()
                .getSystemService(Context.WINDOW_SERVICE);
        if (wm == null) {
            Log.v(TAG, "window manager is null!");
            return;
        }

        int flag = View.SYSTEM_UI_FLAG_LOW_PROFILE
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        getWindow().getDecorView().setSystemUiVisibility(flag);
    }

    private void changeToPortrait() {
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
        WindowManager.LayoutParams params = getWindow().getAttributes();
        Window window = getWindow();
        window.setAttributes(params);
        window.clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
    }

    private void changeToLandscape() {
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
        WindowManager.LayoutParams params = getWindow().getAttributes();
        Window window = getWindow();
        window.setAttributes(params);
        // hide system status with original layout
        window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
    }
}
