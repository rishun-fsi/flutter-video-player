package com.example.videoplayer;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Map;

import io.flutter.plugin.common.BasicMessageChannel;
import io.flutter.plugin.common.BinaryMessenger;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.StandardMessageCodec;
import io.flutter.plugin.common.StringCodec;
import io.flutter.plugin.platform.PlatformView;

public class NativeView implements PlatformView, MethodChannel.MethodCallHandler {

    private final String CHANNEL = "package.name/video_player";
    private final String METHOD_MSG = "controlPlayer";

    // Dart側へデータを返却するためのオブジェクト
    private MethodChannel.Result result;

    private BinaryMessenger messenger;

    private VideoView videoView;

    NativeView(@NonNull Context context, int id, @Nullable Map<String, Object> creationParams, BinaryMessenger messenger) {

        videoView = new VideoView(context);
        final MediaPlayerAdapter mediaPlayerAdapter = new MediaPlayerAdapter();
        videoView.setOnPreparedListener(mediaPlayerAdapter);
        videoView.setOnErrorListener(mediaPlayerAdapter);
        videoView.setOnCompletionListener(mediaPlayerAdapter);

        if (creationParams != null) {
            String url  = (String) creationParams.get("url");
            videoView.setVideoURI(url);
        }

        this.messenger = messenger;
        new MethodChannel(messenger, CHANNEL).setMethodCallHandler(this);
    }

    @NonNull
    @Override
    public View getView() {
        return videoView;
    }

    @Override
    public void dispose() {
        if (videoView != null) {
            videoView.release();
            videoView = null;
        }
    }

    @Override
    public void onMethodCall(@NonNull MethodCall call, @NonNull MethodChannel.Result result) {
        if (call.method.equals(METHOD_MSG)) {
            String msg = (String) call.arguments;
            this.result = result;
            Log.d(" 111111 ", " msg = " + msg);
            if (msg.equals("pause")) {
                videoView.pause();
                result.success("play");
            } else if (msg.equals("play")) {
                videoView.play();
                result.success("pause");
            }
            else if (msg.equals("position")) {
                long position = videoView.getCurrentPosition() / 1000;
                result.success(position);
            }
            else if (msg.equals("duration")) {
                long duration = videoView.getDuration() / 1000;
                result.success(duration);
            }
            else if (msg.equals("fast_rewind")) {
                long position = videoView.getCurrentPosition();
                long seekPos = position - 10000 < 0 ? 0 : position - 10000;
                videoView.seekTo(seekPos);
                result.success(seekPos);
            }
            else if (msg.equals("fast_forward")) {
                long position = videoView.getCurrentPosition();
                long duration = videoView.getDuration();
                long seekPos = Math.min(position + 10000, duration);
                videoView.seekTo(seekPos);
                result.success(seekPos);
            }
        }
    }

    /**
     * Playerイベント通知アダプタ
     */
    private class MediaPlayerAdapter implements VideoView.OnPreparedListener,
            VideoView.OnErrorListener, VideoView.OnCompletionListener {

        @Override
        public void onCompletion(final VideoView videoView) {
//            if (videoView != null) {
//                videoView.reset();
//            }
        }

        @Override
        public boolean onError(final int what, final int extra, final Throwable throwable) {
            if (messenger != null) {
                new BasicMessageChannel(messenger, CHANNEL, StandardMessageCodec.INSTANCE).send(throwable.getMessage());
            }
            return false;
        }

        @Override
        public void onPrepared(final VideoView videoView) {
            if (videoView != null) {
                videoView.start();
                if (messenger != null) {
                    new BasicMessageChannel(messenger, CHANNEL, StandardMessageCodec.INSTANCE).send("prepared");
                }
            }
        }
    }
}

