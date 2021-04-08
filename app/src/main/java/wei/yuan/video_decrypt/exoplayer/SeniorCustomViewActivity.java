package wei.yuan.video_decrypt.exoplayer;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;

import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.ConcatenatingMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import wei.yuan.video_decrypt.R;
import wei.yuan.video_decrypt.exoplayer.custom.MyPlayerControlView;
import wei.yuan.video_decrypt.exoplayer.custom.MyPlayerView;

public class SeniorCustomViewActivity extends AppCompatActivity implements
        SimpleExoPlayer.EventListener, MyPlayerControlView.CustomEventListener {

    private static final String TAG = "SeniorViewActivity";
    private static final String DEFAULT_URL = "https://cdn.singsingenglish.com/new-sing/66c3d05eaa177e07d57465f948f0d8b934b7a7ba.mp4";

    private Context mContext;
    private SimpleExoPlayer mPlayer;
    private MyPlayerView mVideoView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_exo_senior_view);
        mContext = getApplicationContext();

        initView();
        initEvent();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mPlayer.release();
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
    public void onBackClick() {
        Log.i(TAG, "MyPlayerView onBackClick()");
        this.finish();
    }

    @Override
    public void onBroadCastClick() {
        Log.i(TAG, "MyPlayerView onBroadCastClick()");
    }

    private void initView() {
        mPlayer = new SimpleExoPlayer.Builder(mContext).build();
        mPlayer.setPlayWhenReady(true);
        mVideoView = (MyPlayerView) findViewById(R.id.video_view);
        mVideoView.setPlayer(mPlayer);
        Uri uri = Uri.parse(DEFAULT_URL);
        MediaSource mediaSource = buildMediaSource(uri);
        mPlayer.prepare(mediaSource);
    }

    private void initEvent() {
        mPlayer.addListener(this);
        mVideoView.controller.setCustomEventListener(this);
    }

    private MediaSource buildMediaSource(Uri uri) {
        DataSource.Factory dataSourceFactory = new DefaultHttpDataSourceFactory("user-agent");
        MediaSource videoSource = new ProgressiveMediaSource.Factory(dataSourceFactory)
                .createMediaSource(uri);

        return new ConcatenatingMediaSource(videoSource);
    }
}
