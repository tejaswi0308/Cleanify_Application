package com.example.cleanify_application.network;

import android.content.Context;

import java.lang.ref.WeakReference;

import okhttp3.Callback;

/**
 * Safe callback wrapper that uses WeakReference to prevent memory leaks
 * Activities should implement this interface and use SafeCallback when making network requests
 */
public class SafeCallback {

    /**
     * Creates a safe callback that won't leak activity context
     * @param callback The original callback
     * @param context The context (activity) to hold weakly
     * @return A wrapped callback with weak reference
     */
    public static Callback wrap(Callback callback, Context context) {
        return new WeakCallback(callback, context);
    }

    /**
     * Inner class that holds weak reference to context
     */
    private static class WeakCallback implements Callback {
        private final WeakReference<Callback> callbackRef;
        private final WeakReference<Context> contextRef;

        WeakCallback(Callback callback, Context context) {
            this.callbackRef = new WeakReference<>(callback);
            this.contextRef = new WeakReference<>(context);
        }

        @Override
        public void onFailure(okhttp3.Call call, java.io.IOException e) {
            Callback callback = callbackRef.get();
            if (callback != null && contextRef.get() != null) {
                callback.onFailure(call, e);
            }
        }

        @Override
        public void onResponse(okhttp3.Call call, okhttp3.Response response) throws java.io.IOException {
            Callback callback = callbackRef.get();
            if (callback != null && contextRef.get() != null) {
                callback.onResponse(call, response);
            }
        }
    }
}
