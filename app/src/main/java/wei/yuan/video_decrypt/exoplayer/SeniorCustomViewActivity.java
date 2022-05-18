package wei.yuan.video_decrypt.exoplayer;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
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

import com.danikula.videocache.HttpProxyCacheServer;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.ConcatenatingMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.util.Util;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import wei.yuan.video_decrypt.R;
import wei.yuan.video_decrypt.cache.HttpProxyCacheUtil;
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
//    private static final String DEFAULT_URL = "http://wxsnsdy.tc.qq.com/105/20210/snsdyvideodownload?filekey=30280201010421301f0201690402534804102ca905ce620b1241b726bc41dcff44e00204012882540400&bizid=1023&hy=SH&fileparam=302c020101042530230204136ffd93020457e3c4ff02024ef202031e8d7f02030f42400204045a320a0201000400%20%E2%80%94%E2%80%94%E2%80%94%E2%80%94%E2%80%94%E2%80%94%E2%80%94%E2%80%94%E2%80%94%E2%80%94%E2%80%94%E2%80%94%E2%80%94%E2%80%94%E2%80%94%E2%80%94%20%E7%89%88%E6%9D%83%E5%A3%B0%E6%98%8E%EF%BC%9A%E6%9C%AC%E6%96%87%E4%B8%BACSDN%E5%8D%9A%E4%B8%BB%E3%80%8C%E7%A7%A6%E5%B7%9D%E5%B0%8F%E5%B0%86%E3%80%8D%E7%9A%84%E5%8E%9F%E5%88%9B%E6%96%87%E7%AB%A0%EF%BC%8C%E9%81%B5%E5%BE%AACC%204.0%20BY-SA%E7%89%88%E6%9D%83%E5%8D%8F%E8%AE%AE%EF%BC%8C%E8%BD%AC%E8%BD%BD%E8%AF%B7%E9%99%84%E4%B8%8A%E5%8E%9F%E6%96%87%E5%87%BA%E5%A4%84%E9%93%BE%E6%8E%A5%E5%8F%8A%E6%9C%AC%E5%A3%B0%E6%98%8E%E3%80%82%20%E5%8E%9F%E6%96%87%E9%93%BE%E6%8E%A5%EF%BC%9Ahttps://blog.csdn.net/mjb00000/article/details/107720249";
    private static final String SDCARD_DIR = Environment.getExternalStorageDirectory().getPath();
    private static final String DMM_DIR = SDCARD_DIR + File.separator + "dmm";

    private Context mContext;
    private SimpleExoPlayer mPlayer;
    private MyPlayerView mVideoView;

    private String m3u8Dir = "";
    private String fileDir;
    private String httpServer;

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
        chooseHttpServer(dir);
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

    private void initView(int mode) {
        mPlayer = new SimpleExoPlayer.Builder(mContext).build();
        mPlayer.setPlayWhenReady(true);
        mVideoView = (MyPlayerView) findViewById(R.id.video_view);
        mVideoView.setPlayer(mPlayer);
        String m3u8Name = mode == 1 ? "playlist.m3u8" : "playlist_115.m3u8";
        String playUrl = fileDir + File.separator + m3u8Name;
        Log.d(TAG, "play url: " + playUrl);
        Uri uri = Uri.parse(playUrl);
        int type = Util.inferContentType(uri.getLastPathSegment());
        MediaSource mediaSource = null;
        if (type == C.TYPE_HLS || type == C.TYPE_DASH || type == C.TYPE_SS) {
            mediaSource = buildMediaSource(uri);
        } else {
            String cacheUrl = getAndroidVideoCacheUrl(playUrl);
            uri = Uri.parse(cacheUrl);
            mediaSource = buildVideoFileMediaSource(uri);
        }
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

    /**
     * Use AndroidVideoCache library for exoplayer cache
     * Not support for DASH, SS and HLS
     * @param uri
     * @return
     */
    private MediaSource buildVideoFileMediaSource(Uri uri) {
        String userAgent = "user-agent";
        // 构建一个默认的Http数据资源处理工厂
        DefaultHttpDataSourceFactory httpDataSourceFactory = new DefaultHttpDataSourceFactory(userAgent);
        // DefaultDataSourceFactory决定数据加载模式，是从网络加载还是本地缓存加载
        DefaultDataSourceFactory dataSourceFactory = new DefaultDataSourceFactory(mContext, httpDataSourceFactory);
        // AndroidVideoCache库不支持DASH, SS(Smooth Streaming：平滑流媒体，如直播流), HLS数据格式
        // 所以这里使用一个常见媒体转换数据资源工厂
        return new ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(uri);
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

    private String getAndroidVideoCacheUrl(String url) {
        HttpProxyCacheServer httpProxyCacheServer = HttpProxyCacheUtil.getVideoProxy();
        // 将url传入，AndroidVideoCache判断是否使用缓存文件
        return httpProxyCacheServer.getProxyUrl(url);
    }

    private void chooseHttpServer(String dir) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this,
                R.style.Theme_AppCompat_Light_Dialog);
        builder.setTitle("选择服务器");
        final String[] servers = {"阿里云", "115"};
        builder.setItems(servers, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String currentServer = servers[which];
                int mode = 0;
                showToastMsg(mContext, "HttpServer: " + currentServer);
                if (which == 0) {
                    httpServer = "http://47.100.53.117:6868/dmm";
                    mode = 1;
                } else if (which == 1) {
                    httpServer = "http://47.100.53.117:6868/dmm";
                    mode = 2;
                } else {
                    httpServer = "";
                }
                fileDir = httpServer + File.separator + dir;
                initView(mode);
                initEvent();
            }
        });
        builder.show();
    }
}
