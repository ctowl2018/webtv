package com.fongmi.android.tv.api;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.collection.ArrayMap;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.bean.Danmaku;
import com.fongmi.android.tv.impl.Callback;
import com.fongmi.android.tv.setting.DanmakuSetting;
import com.github.catvod.crawler.SpiderDebug;
import com.github.catvod.net.OkHttp;
import com.github.catvod.utils.Trans;

import java.util.function.Consumer;
import java.util.function.Supplier;

import okhttp3.Call;
import okhttp3.Response;

public class DanmakuApi {

    private static final String TAG = DanmakuApi.class.getSimpleName();
    private static final DanmakuSearchGeneration GENERATIONS = new DanmakuSearchGeneration();

    public static boolean canSearch() {
        boolean load = DanmakuSetting.isLoad();
        boolean auto = DanmakuSetting.isAuto();
        String url = DanmakuSetting.getEffectiveApiUrl();
        SpiderDebug.log("danmaku", "canSearch load=%s auto=%s url=%s", load, auto, url);
        return load && auto && !TextUtils.isEmpty(url);
    }

    public static Call newCall(String name, String episode) {
        return newCall(name, episode, TAG);
    }

    public static Call newCall(String name, String episode, String tag) {
        OkHttp.cancel(tag);
        name = Trans.t2s(name);
        episode = Trans.t2s(episode);
        String url = DanmakuSetting.getEffectiveApiUrl();
        if (url.contains("{name}") || url.contains("{episode}")) {
            return OkHttp.newCall(url.replace("{name}", name).replace("{episode}", episode), tag);
        } else {
            ArrayMap<String, String> params = new ArrayMap<>();
            params.put("name", name);
            params.put("episode", episode);
            return OkHttp.newCall(url, OkHttp.toBody(params), tag);
        }
    }

    public static void search(String name, String episode, String context, Supplier<String> currentContext, Consumer<Danmaku> found) {
        DanmakuSearchGeneration.Token token = GENERATIONS.begin(context);
        newCall(name, episode).enqueue(new Callback() {
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                try {
                    String body = response.body().string();
                    Danmaku.arrayFrom(body).stream().findFirst().ifPresent(item -> App.post(() -> {
                        boolean current = GENERATIONS.isCurrent(token, currentContext.get());
                        SpiderDebug.log("danmaku", "search resp code=%s len=%s current=%s url=%s", response.code(), body.length(), current, item.getUrl());
                        if (current) found.accept(item);
                    }));
                } catch (Exception e) {
                    SpiderDebug.log("danmaku", e);
                }
            }

            @Override
            public void onFailure(@NonNull Call call, @NonNull java.io.IOException e) {
                SpiderDebug.log("danmaku", e);
            }
        });
    }

    public static void cancel(String tag) {
        OkHttp.cancel(tag);
    }

    public static void cancel() {
        GENERATIONS.invalidate();
        cancel(TAG);
    }
}
