package com.lime.player.service;

import android.os.Handler;
import android.os.Looper;

/**
 * Binder 线程与播放页之间的命令通道。
 * 语音助手通过 AIDL（oneway）下发指令，Binder 线程回调后统一切到主线程转发给当前播放页。
 */
public final class PlaybackCommandBus {

    /** 当前活跃的播放页实现；没有播放页时指令被丢弃。 */
    public interface Sink {
        void onSkip(int skipType);

        void onFocusActor(String actorId);

        /** 继续播放。 */
        void onVoiceResume();

        /** 暂停播放。 */
        void onVoicePause();

        /** 设置播放速度，单位倍。 */
        void onVoiceSetSpeed(float speed);
    }

    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static volatile Sink sink;

    private PlaybackCommandBus() {
    }

    public static void register(Sink s) {
        sink = s;
    }

    public static void unregister(Sink s) {
        if (sink == s) sink = null;
    }

    public static boolean hasSink() {
        return sink != null;
    }

    public static void dispatchSkip(final int skipType) {
        MAIN.post(new Runnable() {
            @Override
            public void run() {
                Sink s = sink;
                android.util.Log.d("VoiceBus", "dispatch skip=" + skipType + " sink=" + (s != null));
                if (s != null) s.onSkip(skipType);
            }
        });
    }

    public static void dispatchFocusActor(final String actorId) {
        MAIN.post(new Runnable() {
            @Override
            public void run() {
                Sink s = sink;
                if (s != null) s.onFocusActor(actorId);
            }
        });
    }

    public static void dispatchResume() {
        MAIN.post(new Runnable() {
            @Override
            public void run() {
                Sink s = sink;
                android.util.Log.d("VoiceBus", "dispatch resume sink=" + (s != null));
                if (s != null) s.onVoiceResume();
            }
        });
    }

    public static void dispatchPause() {
        MAIN.post(new Runnable() {
            @Override
            public void run() {
                Sink s = sink;
                android.util.Log.d("VoiceBus", "dispatch pause sink=" + (s != null));
                if (s != null) s.onVoicePause();
            }
        });
    }

    public static void dispatchSpeed(final float speed) {
        MAIN.post(new Runnable() {
            @Override
            public void run() {
                Sink s = sink;
                android.util.Log.d("VoiceBus", "dispatch speed=" + speed + " sink=" + (s != null));
                if (s != null) s.onVoiceSetSpeed(speed);
            }
        });
    }
}
