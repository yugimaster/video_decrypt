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
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import wei.yuan.video_decrypt.R;

public class SimpleCustomViewActivity extends AppCompatActivity implements
        SimpleExoPlayer.EventListener {

    private static final String TAG = "DefaultViewActivity";
    private static final String DEFAULT_URL = "https://cdn.singsingenglish.com/new-sing/66c3d05eaa177e07d57465f948f0d8b934b7a7ba.mp4";

    private SimpleExoPlayer mPlayer;
    private Context mContext;
    private PlayerView mVideoView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_exo_simple_view);
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
                break;
            case SimpleExoPlayer.STATE_READY:
                break;
            case SimpleExoPlayer.STATE_ENDED:
                break;
            case SimpleExoPlayer.STATE_IDLE:
                break;
            default:
                break;
        }
    }

    @Override
    public void onPlayerError(ExoPlaybackException error) {
        Log.e(TAG, "ExoPlaybackException: " + error);
    }

    private void initView() {
        SimpleExoPlayer.Builder builder = new SimpleExoPlayer.Builder(mContext);
        mPlayer = builder.build();
        mVideoView = (PlayerView) findViewById(R.id.video_view);
        mVideoView.setPlayer(mPlayer);
        mPlayer.setPlayWhenReady(true);
        Uri uri = Uri.parse(DEFAULT_URL);
        MediaSource mediaSource = buildMediaSource(uri);
        mPlayer.prepare(mediaSource, false, true);
    }

    private void initEvent() {
        mPlayer.addListener(this);
    }

    private MediaSource buildMediaSource(Uri uri) {
        DataSource.Factory dataSourceFactory = new DefaultHttpDataSourceFactory("user-agent");
        MediaSource videoSource = new ProgressiveMediaSource.Factory(dataSourceFactory)
                .createMediaSource(uri);

        return new ConcatenatingMediaSource(videoSource);
    }
}
