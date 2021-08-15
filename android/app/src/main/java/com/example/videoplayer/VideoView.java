package com.example.videoplayer;

import android.content.Context;
import android.net.Uri;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup.LayoutParams;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.source.dash.DashMediaSource;
import com.google.android.exoplayer2.source.dash.DefaultDashChunkSource;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.source.rtsp.RtspMediaSource;
import com.google.android.exoplayer2.source.smoothstreaming.SsMediaSource;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.ExoTrackSelection;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource;
import com.google.android.exoplayer2.upstream.HttpDataSource;
import com.google.android.exoplayer2.util.MimeTypes;
import com.google.android.exoplayer2.util.Util;
import com.google.android.exoplayer2.video.VideoSize;

public class VideoView extends SurfaceView implements Player.Listener{

    // all possible internal states
    private static final int STATE_ERROR = -1;
    private static final int STATE_IDLE = 0;
    private static final int STATE_PREPARING = 1;
    private static final int STATE_PREPARED = 2;
    private static final int STATE_PLAYING = 3;
    private static final int STATE_PAUSED = 4;
    private static final int STATE_PLAYBACK_COMPLETED = 5;

    // currentState is a VideoView object's current state.
    private int mCurrentState = STATE_IDLE;

    // settable by the client
    private Uri mPlayUri;

    // All the stuff we need for playing and showing a video
    private Context mContext;
    private SurfaceHolder mSurfaceHolder = null;
    private SimpleExoPlayer mMediaPlayer = null;

    private int mVideoWidth;
    private int mVideoHeight;

    private boolean mIsFirstFlag;

    private OnPreparedListener mOnPreparedListener;
    private OnErrorListener mOnErrorListener;
    private OnCompletionListener mOnCompletionListener;


    SurfaceHolder.Callback mSurfaceHolderCallback = new SurfaceHolder.Callback() {

        @Override
        public void surfaceChanged(final SurfaceHolder holder, final int format,
                                   final int width, final int height) {

        }

        @Override
        public void surfaceCreated(final SurfaceHolder holder) {
            mSurfaceHolder = holder;
            openVideo();
        }

        @Override
        public void surfaceDestroyed(final SurfaceHolder holder) {
            mSurfaceHolder = null;
        }
    };

    public void setVideoURI(final String playUri) {
        mPlayUri = Uri.parse(playUri);
        openVideo();
    }

    public VideoView(final Context context) {
        super(context);
        mContext = context;
        mVideoWidth = 0;
        mVideoHeight = 0;
        getHolder().addCallback(mSurfaceHolderCallback);
        setFocusable(true);
        requestFocus();
    }

    @Override
    protected void onMeasure(final int widthMeasureSpec, final int heightMeasureSpec) {
        int width = getDefaultSize(mVideoWidth, widthMeasureSpec);
        int height = getDefaultSize(mVideoHeight, heightMeasureSpec);

        if (mVideoWidth == 0 || mVideoHeight == 0) {
            setMeasuredDimension(width, height);
        } else {

            if (mVideoWidth * height > width * mVideoHeight) {
                height = width * mVideoHeight / mVideoWidth;
            } else if (mVideoWidth * height < width * mVideoHeight) {
                width = height * mVideoWidth / mVideoHeight;
            }
            setMeasuredDimension(width, height);
        }
    }

    private void openVideo() {
        if (mPlayUri == null || mSurfaceHolder == null) {
            // not ready for playback just yet, will try again later
            return;
        }

        final MediaSource mMediaSource = buildMediaSource(mPlayUri);

        if (mMediaPlayer == null) {
            ExoTrackSelection.Factory adaptiveTrackSelectionFactory = new AdaptiveTrackSelection.Factory();
            DefaultTrackSelector.ParametersBuilder builder = new DefaultTrackSelector.ParametersBuilder(mContext);
            final DefaultTrackSelector mDefaultTrackSelector = new DefaultTrackSelector(
                    builder.build(), adaptiveTrackSelectionFactory);
            DefaultRenderersFactory renderersFactory = new DefaultRenderersFactory(mContext);
            mIsFirstFlag = true;
            mMediaPlayer = new SimpleExoPlayer.Builder(/* context= */ mContext, renderersFactory)
                    .setTrackSelector(mDefaultTrackSelector)
                    .build();

            mMediaPlayer.setVideoSurfaceView(this);
            mMediaPlayer.addListener(this);
            mMediaPlayer.addVideoListener(this);

        }
        if (mIsFirstFlag) {
            mIsFirstFlag = false;
            this.setKeepScreenOn(true);
            mMediaPlayer.setMediaSource(mMediaSource);
            mMediaPlayer.prepare();
            mCurrentState = STATE_PREPARING;
        }
    }

    private MediaSource buildMediaSource(Uri uri) {
        @C.ContentType int type = Util.inferContentType(uri);
        final DataSource.Factory mediaDataSourceFactory = buildDataSourceFactory();
        MediaItem.Builder mediaItem = new MediaItem.Builder();
        switch (type) {
            case C.TYPE_DASH:
                return new DashMediaSource.Factory(
                        new DefaultDashChunkSource.Factory(mediaDataSourceFactory),
                        buildDataSourceFactory())
                        .createMediaSource(mediaItem.setUri(uri).setMimeType(MimeTypes.APPLICATION_MPD).build());
            case C.TYPE_HLS:
                return new HlsMediaSource.Factory(mediaDataSourceFactory).createMediaSource(mediaItem.setUri(uri).setMimeType(MimeTypes.APPLICATION_M3U8).build());
            case C.TYPE_SS:
                return new SsMediaSource.Factory(mediaDataSourceFactory).createMediaSource(mediaItem.setUri(uri).setMimeType(MimeTypes.APPLICATION_SS).build());
            case C.TYPE_RTSP:
                return new RtspMediaSource.Factory().createMediaSource(mediaItem.setUri(uri).setMimeType(MimeTypes.APPLICATION_RTSP).build());
            case C.TYPE_OTHER:
                return new ProgressiveMediaSource.Factory(mediaDataSourceFactory).createMediaSource(
                        mediaItem.setUri(uri).build());
            default: {
                throw new IllegalStateException("Unsupported type: " + type);
            }
        }
    }

    @Override
    public void onVideoSizeChanged(VideoSize videoSize) {
        mVideoWidth = videoSize.width;
        mVideoHeight = videoSize.height;
        if (mVideoWidth != 0 && mVideoHeight != 0) {
            setFixedVideoSize(mVideoWidth, mVideoHeight);
        }
    }


    /** Returns a {@link HttpDataSource.Factory}. */
    public HttpDataSource.Factory buildHttpDataSourceFactory() {
        return new DefaultHttpDataSource.Factory().setUserAgent(Util.getUserAgent(mContext, mContext.getPackageName()));
    }

    /** Returns a {@link HttpDataSource.Factory}. */
    private DataSource.Factory buildDataSourceFactory() {
        return new DefaultDataSourceFactory(mContext, buildHttpDataSourceFactory());
    }

    public void setOnPreparedListener(final OnPreparedListener listener) {
        mOnPreparedListener = listener;
    }

    public void setOnErrorListener(final OnErrorListener listener) {
        mOnErrorListener = listener;
    }

    public void setOnCompletionListener(final OnCompletionListener listener) {
        mOnCompletionListener = listener;
    }


    /**
     * reset the media player to idle state from any state
     */
    public void reset() {
        if (mMediaPlayer != null && !mIsFirstFlag) {
            mMediaPlayer.stop(true);
            mCurrentState = STATE_IDLE;
            mIsFirstFlag = true;
        }
    }


    public void release() {
        mContext = null;
        if (mSurfaceHolderCallback != null) {
            getHolder().removeCallback(mSurfaceHolderCallback);
            mSurfaceHolderCallback = null;
        }
        mSurfaceHolder = null;
        if (mMediaPlayer != null) {
            mMediaPlayer.release();
            mCurrentState = STATE_IDLE;
            mMediaPlayer = null;
        }
    }


    public boolean onTrackballEvent(final MotionEvent ev) {
        return false;
    }

    public void start() {
        if (isInPlaybackState()) {
            mMediaPlayer.setPlayWhenReady(true);
            mCurrentState = STATE_PLAYING;
        }
    }

    public long getCurrentPosition() {
        return mMediaPlayer.getCurrentPosition();
    }

    public long getDuration() {
        return mMediaPlayer.getDuration();
    }

    public void pause() {
        if (isPlaying()) {
            if (mMediaPlayer.getPlayWhenReady()) {
                mMediaPlayer.setPlayWhenReady(false);
                mCurrentState = STATE_PAUSED;
            }
        }
    }

    public void play() {
        if (mCurrentState == STATE_PAUSED) {
            mMediaPlayer.setPlayWhenReady(true);
            mCurrentState = STATE_PLAYING;
        }
    }

    public void seekTo(long position) {
        if (mCurrentState == STATE_PAUSED || mCurrentState == STATE_PLAYING) {
            mMediaPlayer.seekTo(position);
        }
    }

    public boolean isPlaying() {
        if (isInPlaybackState()) {
            int playbackState = mMediaPlayer.getPlaybackState();
            return playbackState != Player.STATE_IDLE && playbackState != Player.STATE_ENDED
                    && mMediaPlayer.getPlayWhenReady();
        }
        return false;
    }


    private boolean isInPlaybackState() {
        return (mMediaPlayer != null &&
                mCurrentState != STATE_ERROR &&
                mCurrentState != STATE_IDLE &&
                mCurrentState != STATE_PREPARING);
    }


    @Override
    public void onPlaybackStateChanged(int state) {
        switch (state) {
            case Player.STATE_ENDED:
                mCurrentState = STATE_PLAYBACK_COMPLETED;
                if (mOnCompletionListener != null) {
                    mOnCompletionListener.onCompletion(VideoView.this);
                }
                break;
            case Player.STATE_READY:
                if (mCurrentState == STATE_PREPARING) {
                    mCurrentState = STATE_PREPARED;
                    if (mOnPreparedListener != null) {
                        mOnPreparedListener.onPrepared(VideoView.this);
                    }
                }
                break;
            case Player.STATE_BUFFERING:
                break;
        }
    }


    @Override
    public void onPlayerError(ExoPlaybackException error) {
        if (mOnErrorListener != null) {
            mOnErrorListener.onError(error.rendererIndex, error.type, error.getCause());
        }
        mCurrentState = STATE_ERROR;
    }

    private void setFixedVideoSize(final int width, final int height) {
        if (width > 0 && height > 0) {
            final View currentParent = (View) getParent();
            if (currentParent != null) {
                // resize the surfaceView to keep the aspect-ratio
                final int cw = currentParent.getWidth();
                final int ch = currentParent.getHeight();
                final LayoutParams surfaceLp = getLayoutParams();
                if (width * ch > height * cw) {
                    // the video has a greater aspect-ratio than the container
                    surfaceLp.width = cw;
                    surfaceLp.height = cw * height / width;
                } else {
                    // the container has a greater aspect-ratio than the video
                    surfaceLp.height = ch;
                    surfaceLp.width = ch * width / height;
                }
                requestLayout();
            }
        }
    }

    public interface OnPreparedListener {
        void onPrepared(final VideoView videoView);
    }

    public interface OnErrorListener {
        boolean onError(final int what, final int extra, final Throwable throwable);
    }

    public interface OnCompletionListener {
        void onCompletion(final VideoView videoView);
    }

}
