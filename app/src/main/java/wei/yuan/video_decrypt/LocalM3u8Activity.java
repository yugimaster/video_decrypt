package wei.yuan.video_decrypt;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
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
import java.util.Arrays;
import java.util.Formatter;
import java.util.Locale;

import androidx.core.app.ActivityCompat;
import wei.yuan.video_decrypt.m3u8server.M3u8Server;
import wei.yuan.video_decrypt.util.CommonUtil;
import wei.yuan.video_decrypt.util.ParseSystemUtil;

public class LocalM3u8Activity extends Activity implements View.OnClickListener {

    private static final String TAG = "LocalM3u8Activity";
    private static final String SDCARD_DIR = Environment.getExternalStorageDirectory().getPath();
    private static final String DMM_DIR = SDCARD_DIR + File.separator + "dmm";

    private static final int SHOW_BUTTON_PART_ONE = 0;
    private static final int SHOW_BUTTON_PART_TWO = 1;
    private static final int SHOW_BUTTON_PART_THREE = 2;
    private static final int SHOW_BUTTON_PART_FOUR = 3;

    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            "android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.WRITE_EXTERNAL_STORAGE"};

    private SimpleExoPlayer mSimpleExoPlayer;
    private SimpleExoPlayerView mExoPlayerView;

    private Context mContext;
    private MyHandler mHandler;

    private Button mBtnPermission;
    private Button mBtnPart1;
    private Button mBtnPart2;
    private Button mBtnPart3;
    private Button mBtnPart4;

    private String m3u8Dir = "";

    private boolean isSingle = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.v(TAG, "onCreate()");
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_localm3u8);

        mContext = getApplicationContext();
        mHandler = new MyHandler(getMainLooper());

        Intent intent = getIntent();
        Bundle bundle = intent.getBundleExtra("Info");
        m3u8Dir = bundle.getString("directory");
        // 开启本地代理
        M3u8Server.execute();
//        // 请求权限
//        findViewById(R.id.btn1).setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                verifyStoragePermissions(LocalM3u8Activity.this);
//            }
//        });
//        // 播放
//        findViewById(R.id.btn2).setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                initPlayer();
//                playVideo();
//            }
//        });
        initView();
        setVideoPartButton(m3u8Dir);
    }

    @Override
    protected void onPause() {
        Log.v(TAG, "onPause()");
        super.onPause();
        if (mSimpleExoPlayer != null) {
            mSimpleExoPlayer.stop();
        }
    }

    @Override
    protected void onStop() {
        Log.v(TAG, "onStop()");
        super.onStop();
        if (mSimpleExoPlayer != null) {
            mSimpleExoPlayer.release();
        }
    }

    @Override
    protected void onDestroy() {
        Log.v(TAG, "onDestroy()");
        super.onDestroy();
        M3u8Server.close();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_permission:
                Log.v(TAG, "request storage permission");
                verifyStoragePermissions(LocalM3u8Activity.this);
                break;
            case R.id.btn_part1:
                startVideoPartPlay("1");
                break;
            case R.id.btn_part2:
                startVideoPartPlay("2");
                break;
            case R.id.btn_part3:
                startVideoPartPlay("3");
                break;
            case R.id.btn_part4:
                startVideoPartPlay("4");
                break;
            default:
                break;
        }
    }

    private class MyHandler extends Handler {
        MyHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case SHOW_BUTTON_PART_ONE:
                    Log.v(TAG, "show button part one");
                    mBtnPart1.setVisibility(View.VISIBLE);
                    break;
                case SHOW_BUTTON_PART_TWO:
                    mBtnPart2.setVisibility(View.VISIBLE);
                    break;
                case SHOW_BUTTON_PART_THREE:
                    mBtnPart3.setVisibility(View.VISIBLE);
                    break;
                case SHOW_BUTTON_PART_FOUR:
                    mBtnPart4.setVisibility(View.VISIBLE);
                    break;
                default:
                    break;
            }
        }
    }

    private void verifyStoragePermissions(Activity activity) {
        try {
            //检测是否有写的权限
            int permission = ActivityCompat.checkSelfPermission(activity,
                    "android.permission.WRITE_EXTERNAL_STORAGE");
            if (permission != PackageManager.PERMISSION_GRANTED) {
                // 没有写的权限，去申请写的权限，会弹出对话框
                ActivityCompat.requestPermissions(activity, PERMISSIONS_STORAGE, REQUEST_EXTERNAL_STORAGE);
            } else {
                showToastMsg(mContext, "写入权限无需再次申请！");
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

    private void playVideo(String videoPath, String m3u8Path) {
        Log.v(TAG, "---playVideo---");

        if (m3u8Dir.isEmpty()) {
            showToastMsg(mContext, "请输入m3u8文件所在目录！");
            return;
        }

        keyConvert(videoPath);

        Log.d(TAG, "external storage dir: " + SDCARD_DIR);
//        String localUrl = String.format("http://127.0.0.1:%d%s", M3u8Server.PORT, DMM_DIR + File.separator
//                + m3u8Dir + File.separator + "local.m3u8");
        String localUrl = String.format("http://127.0.0.1:%d%s", M3u8Server.PORT, m3u8Path);
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

    private void keyConvert(String videoPath) {
        File keyFile = new File(videoPath + File.separator + "key.key");
        if (keyFile.exists()) {
            Log.d(TAG, keyFile.getAbsolutePath() + " exists");
            if (keyFile.length() == 16) {
                Log.d(TAG, keyFile.getName() + " is correct");
                return;
            } else {
                Log.d(TAG, "error " + keyFile.getName() + ", delete it...");
                showToastMsg(mContext, "error " + keyFile.getName());
                keyFile.delete();
            }
        }
        File keyOriginFile = new File(videoPath + File.separator + "key_o.key");
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

    private void showToastMsg(Context context, String msg) {
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
    }

    private void setVideoPartButton(String path) {
        File dir = new File(DMM_DIR + File.separator + path);
        File[] files = dir.listFiles();
        String[] nums = {"1", "2", "3", "4"};
        if (files == null || files.length == 0) {
            Log.v(TAG, dir.getPath() + " is empty");
            return;
        }
        File m3u8 = new File(dir + File.separator + "local.m3u8");
        if (m3u8.exists() && m3u8.isFile()) {
            Log.v(TAG, m3u8.getPath() + " exists");
            mHandler.sendEmptyMessage(SHOW_BUTTON_PART_ONE);
            return;
        }
        isSingle = false;
        for (File f : files) {
            String name = f.getName();
            if (f.isDirectory() && Arrays.asList(nums).contains(name)) {
                switch (name) {
                    case "1":
                        mHandler.sendEmptyMessage(SHOW_BUTTON_PART_ONE);
                        break;
                    case "2":
                        mHandler.sendEmptyMessage(SHOW_BUTTON_PART_TWO);
                        break;
                    case "3":
                        mHandler.sendEmptyMessage(SHOW_BUTTON_PART_THREE);
                        break;
                    case "4":
                        mHandler.sendEmptyMessage(SHOW_BUTTON_PART_FOUR);
                        break;
                }
            }
        }
    }

    private void initView() {
        mBtnPermission = (Button) findViewById(R.id.btn_permission);
        mBtnPermission.setOnClickListener(this);
        mBtnPart1 = (Button) findViewById(R.id.btn_part1);
        mBtnPart1.setOnClickListener(this);
        mBtnPart2 = (Button) findViewById(R.id.btn_part2);
        mBtnPart2.setOnClickListener(this);
        mBtnPart3 = (Button) findViewById(R.id.btn_part3);
        mBtnPart3.setOnClickListener(this);
        mBtnPart4 = (Button) findViewById(R.id.btn_part4);
        mBtnPart4.setOnClickListener(this);
    }

    private void startVideoPartPlay(String part) {
        String videoPath = DMM_DIR + File.separator + m3u8Dir;
        String m3u8Path = "";
        if (part.equals("1") && isSingle) {
            m3u8Path = videoPath + File.separator + "local.m3u8";
        } else {
            m3u8Path = videoPath + File.separator + part + File.separator + "local.m3u8";
        }

        if (mSimpleExoPlayer != null) {
            mSimpleExoPlayer.stop();
            mSimpleExoPlayer.release();
            mSimpleExoPlayer = null;
        }

        initPlayer();
        playVideo(videoPath, m3u8Path);
    }
}
