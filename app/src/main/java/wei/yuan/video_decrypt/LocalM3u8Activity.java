package wei.yuan.video_decrypt;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.ActivityCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.LoadControl;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.ui.SimpleExoPlayerView;
import com.google.android.exoplayer2.upstream.BandwidthMeter;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.util.Util;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.Formatter;
import java.util.Locale;

import wei.yuan.video_decrypt.m3u8server.M3u8Server;
import wei.yuan.video_decrypt.util.CommonUtil;
import wei.yuan.video_decrypt.util.ParseSystemUtil;

public class LocalM3u8Activity extends Activity {

    private static final String TAG = "LocalM3u8Activity";
    private static final String DMM_DIR = "/dmm/swd134/";

    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            "android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.WRITE_EXTERNAL_STORAGE"};

    private SimpleExoPlayer mSimpleExoPlayer;
    private SimpleExoPlayerView mExoPlayerView;

    private String m3u8Dir = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.v(TAG, "onCreate()");
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_localm3u8);

        Intent intent = getIntent();
        Bundle bundle = intent.getBundleExtra("Info");
        m3u8Dir = bundle.getString("directory");
        // 开启本地代理
        M3u8Server.execute();
        // 请求权限
        findViewById(R.id.btn1).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                verifyStoragePermissions(LocalM3u8Activity.this);
            }
        });
        // 播放
        findViewById(R.id.btn2).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                initPlayer();
                playVideo();
            }
        });
    }

    @Override
    protected void onPause() {
        Log.v(TAG, "onPause()");
        super.onPause();
        mSimpleExoPlayer.stop();
    }

    @Override
    protected void onStop() {
        Log.v(TAG, "onStop()");
        super.onStop();
        mSimpleExoPlayer.release();
    }

    private void verifyStoragePermissions(Activity activity) {
        try {
            //检测是否有写的权限
            int permission = ActivityCompat.checkSelfPermission(activity,
                    "android.permission.WRITE_EXTERNAL_STORAGE");
            if (permission != PackageManager.PERMISSION_GRANTED) {
                // 没有写的权限，去申请写的权限，会弹出对话框
                ActivityCompat.requestPermissions(activity, PERMISSIONS_STORAGE, REQUEST_EXTERNAL_STORAGE);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initPlayer() {
        //1. 创建一个默认的 TrackSelector
        BandwidthMeter bandwidthMeter = new DefaultBandwidthMeter();
        TrackSelection.Factory videoTackSelectionFactory =
                new AdaptiveTrackSelection.Factory(bandwidthMeter);
        TrackSelector trackSelector = new DefaultTrackSelector(videoTackSelectionFactory);
        LoadControl loadControl = new DefaultLoadControl();
        //2.创建ExoPlayer
        mSimpleExoPlayer = ExoPlayerFactory.newSimpleInstance(this, trackSelector, loadControl);
        //3.创建SimpleExoPlayerView
        mExoPlayerView = (SimpleExoPlayerView) findViewById(R.id.exoView);
        //4.为SimpleExoPlayer设置播放器
        mExoPlayerView.setPlayer(mSimpleExoPlayer);
    }

    private void playVideo() {
        Log.v(TAG, "---playVideo---");

        if (m3u8Dir.isEmpty()) {
            Toast.makeText(getApplicationContext(), "请输入m3u8文件所在目录！", Toast.LENGTH_LONG).show();
            return;
        }

        keyConvert();

        String sdDir = Environment.getExternalStorageDirectory().getPath();
        Log.d(TAG, "external storage dir: " + sdDir);
//        String localUrl = String.format("http://127.0.0.1:%d%s", M3u8Server.PORT, sdDir + "/local_m3u8/local.m3u8");
//        String localUrl = String.format("http://127.0.0.1:%d%s", M3u8Server.PORT, sdDir + DMM_DIR + "local_swd134.m3u8");
        String localUrl = String.format("http://127.0.0.1:%d%s", M3u8Server.PORT, sdDir + "/dmm/"
                + m3u8Dir + "/" + m3u8Dir + ".m3u8");
        Log.d(TAG, "localUrl: " + localUrl);

        //Prepare the player with the source
        mSimpleExoPlayer.prepare(createMediaSource(Uri.parse(localUrl)));
        //添加监听的listener
        mSimpleExoPlayer.addListener(eventListener);
        mSimpleExoPlayer.setPlayWhenReady(true);
    }

    private MediaSource createMediaSource(Uri uri) {
        if (uri == null || TextUtils.isEmpty(uri.toString())) {
            return null;
        }

        DataSource.Factory mDataSourceFactory = new DefaultHttpDataSourceFactory("ExoPlayer", null);

        int type = Util.inferContentType(uri.getLastPathSegment());
        switch (type) {
            // .m3u8
            case C.TYPE_HLS:
                return new HlsMediaSource.Factory(mDataSourceFactory).createMediaSource(
                        uri, new Handler(Looper.getMainLooper()), null);
            case C.TYPE_OTHER:
                return new ExtractorMediaSource.Factory(mDataSourceFactory).createMediaSource(
                        uri, new Handler(Looper.getMainLooper()), null);
            default:
                return null;
        }
    }

    private ExoPlayer.EventListener eventListener = new ExoPlayer.EventListener() {

        @Override
        public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {
            Log.v(TAG, "onTracksChanged");
        }

        @Override
        public void onLoadingChanged(boolean isLoading) {
            Log.v(TAG, "onLoadingChanged");
        }

        @Override
        public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
            Log.v(TAG, "onPlayerStateChanged: playWhenReady = " + String.valueOf(playWhenReady)
                    + " playbackState = " + playbackState);
            switch (playbackState) {
                case ExoPlayer.STATE_ENDED:
                    Log.v(TAG, "Playback ended!");
                    //Stop playback and return to start position
                    setPlayPause(false);
                    mSimpleExoPlayer.seekTo(0);
                    break;
                case ExoPlayer.STATE_READY:
                    Log.v(TAG, "ExoPlayer ready! pos: " + mSimpleExoPlayer.getCurrentPosition()
                            + " max: " + stringForTime((int) mSimpleExoPlayer.getDuration()));
                    setProgress(0);
                    break;
                case ExoPlayer.STATE_BUFFERING:
                    Log.v(TAG, "Playback buffering");
                    break;
                case ExoPlayer.STATE_IDLE:
                    Log.v(TAG, "ExoPlayer idle!");
                    break;
            }
        }

        @Override
        public void onPlayerError(ExoPlaybackException error) {
            Log.e(TAG, "onPlaybackError: " + error.getMessage());
        }
    };

    /**
     * Starts or stops playback. Also takes care of the Play/Pause button toggling
     *
     * @param play True if playback should be started
     */
    private void setPlayPause(boolean play) {
        mSimpleExoPlayer.setPlayWhenReady(play);
    }

    private String stringForTime(int timeMs) {
        StringBuilder mFormatBuilder;
        Formatter mFormatter;
        mFormatBuilder = new StringBuilder();
        mFormatter = new Formatter(mFormatBuilder, Locale.getDefault());
        int totalSeconds = timeMs / 1000;

        int seconds = totalSeconds % 60;
        int minutes = (totalSeconds / 60) % 60;
        int hours = totalSeconds / 3600;

        mFormatBuilder.setLength(0);
        if (hours > 0) {
            return mFormatter.format("%d:%02d:%02d", hours, minutes, seconds).toString();
        } else {
            return mFormatter.format("%02d:%02d", minutes, seconds).toString();
        }
    }

    private void keyConvert() {
        String sdDir = Environment.getExternalStorageDirectory().getPath();
        File keyFile = new File(sdDir + "/dmm/" + m3u8Dir + "/key_o.key");
        if (keyFile.exists()) {
            Log.d(TAG, keyFile.getAbsolutePath() + " exists");
            try {
                String keyHexStr = getKeyHex(keyFile);
                byte[] bytes = ParseSystemUtil.parseHexStr2Byte(keyHexStr);
                File newKey = new File(sdDir + "/dmm/" + m3u8Dir + "/key.key");
                boolean isSuccess = CommonUtil.writeBinaryFile(bytes, newKey, null);
                if (isSuccess) {
                    Log.d(TAG, "create key.key success");
                } else {
                    Log.d(TAG, "create key.key fail");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            Log.d(TAG, keyFile.getAbsolutePath() + " doesn't exist");
        }
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
}
