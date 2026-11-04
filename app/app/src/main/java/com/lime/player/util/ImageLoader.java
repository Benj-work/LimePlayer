package com.lime.player.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.util.LruCache;
import android.widget.ImageView;

import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory;

import java.lang.ref.WeakReference;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** 极简海报加载器：内存 LRU + 采样解码 + 圆角。 */
public final class ImageLoader {

    private static final LruCache<String, Bitmap> CACHE =
            new LruCache<String, Bitmap>((int) (Runtime.getRuntime().maxMemory() / 8)) {
                @Override
                protected int sizeOf(String key, Bitmap value) {
                    return value.getByteCount();
                }
            };

    private static final ExecutorService POOL = Executors.newFixedThreadPool(3);
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private ImageLoader() {
    }

    public static void load(final ImageView view, final String path, final int targetWidthPx,
                            final float radiusPx) {
        if (view == null) return;
        if (path == null || path.isEmpty()) {
            view.setTag("none");
            view.setImageDrawable(null);
            return;
        }
        final String key = path + "@" + targetWidthPx;
        Bitmap cached = CACHE.get(key);
        if (cached != null) {
            view.setTag(key);
            apply(view, cached, radiusPx);
            return;
        }
        view.setTag(key);
        final WeakReference<ImageView> ref = new WeakReference<>(view);
        POOL.execute(new Runnable() {
            @Override
            public void run() {
                final Bitmap bmp = decode(path, targetWidthPx);
                if (bmp == null) return;
                CACHE.put(key, bmp);
                MAIN.post(new Runnable() {
                    @Override
                    public void run() {
                        ImageView iv = ref.get();
                        if (iv != null && key.equals(iv.getTag())) {
                            apply(iv, bmp, radiusPx);
                        }
                    }
                });
            }
        });
    }

    private static void apply(ImageView view, Bitmap bmp, float radiusPx) {
        Context ctx = view.getContext();
        if (radiusPx > 0 && ctx != null) {
            RoundedBitmapDrawable d = RoundedBitmapDrawableFactory.create(ctx.getResources(), bmp);
            d.setCornerRadius(radiusPx);
            d.setAntiAlias(true);
            view.setImageDrawable(d);
        } else {
            view.setImageBitmap(bmp);
        }
    }

    private static Bitmap decode(String path, int targetWidth) {
        try {
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(path, bounds);
            if (bounds.outWidth <= 0) return null;

            int sample = 1;
            if (targetWidth > 0) {
                while (bounds.outWidth / (sample * 2) >= targetWidth) sample *= 2;
            }
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inSampleSize = sample;
            opts.inPreferredConfig = Bitmap.Config.RGB_565;
            return BitmapFactory.decodeFile(path, opts);
        } catch (Throwable t) {
            return null;
        }
    }

    public static void clear() {
        CACHE.evictAll();
    }
}
