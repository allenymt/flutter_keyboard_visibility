package com.github.adee42.keyboardvisibility;

import android.app.Activity;
import android.app.Application;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;

import io.flutter.embedding.android.FlutterView;
import io.flutter.plugin.common.EventChannel;
import io.flutter.plugin.common.EventChannel.EventSink;
import io.flutter.plugin.common.EventChannel.StreamHandler;
import io.flutter.plugin.common.PluginRegistry.Registrar;


public class KeyboardVisibilityPlugin implements StreamHandler, Application.ActivityLifecycleCallbacks, ViewTreeObserver.OnGlobalLayoutListener {
    private static final String STREAM_CHANNEL_NAME = "github.com/adee42/flutter_keyboard_visibility";
    View mainView = null;
    EventSink eventsSink;
    Registrar registrar;
    boolean isVisible;


    KeyboardVisibilityPlugin(Registrar registrar) {
        this.registrar = registrar;
        eventsSink = null;
    }

    public static void registerWith(Registrar registrar) {

        final EventChannel eventChannel = new EventChannel(registrar.messenger(), STREAM_CHANNEL_NAME);
        KeyboardVisibilityPlugin instance = new KeyboardVisibilityPlugin(registrar);
        eventChannel.setStreamHandler(instance);
        Application application = registrar.activity().getApplication();
        application.registerActivityLifecycleCallbacks(instance);
        //  引擎是在resume的时候初始化的，所以当第一个打开的页面是flutteractivity时，onResume监听不会回调，需要手动注册，这只是临时方案
        // 把插件注册方式升级到v2就可以了
        try{
            Activity activity = registrar.activity();
            if (instance.checkIsFlutterActivity(activity)){
                View mainView = ((ViewGroup) activity.findViewById(android.R.id.content)).getChildAt(0);
                mainView.getViewTreeObserver().addOnGlobalLayoutListener(instance);
                instance.setMainView(mainView);
            }
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    public void setMainView(View mainView) {
        this.mainView = mainView;
    }

    @Override
    public void onGlobalLayout() {
        Rect r = new Rect();

        if (mainView != null) {
            mainView.getWindowVisibleDisplayFrame(r);

            // check if the visible part of the screen is less than 85%
            // if it is then the keyboard is showing
            boolean newState = ((double) r.height() / (double) mainView.getRootView().getHeight()) < 0.85;

            if (newState != isVisible) {
                isVisible = newState;
                if (eventsSink != null) {
                    eventsSink.success(isVisible ? 1 : 0);
                }
            }
        }
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle bundle) {

    }

    @Override
    public void onActivityStarted(Activity activity) {

    }

    @Override
    public void onActivityResumed(Activity activity) {
        if (!checkIsFlutterActivity(activity)) {
            return;
        }
        try {
            mainView = ((ViewGroup) activity.findViewById(android.R.id.content)).getChildAt(0);
            mainView.getViewTreeObserver().addOnGlobalLayoutListener(this);
        } catch (Exception e) {
            // do nothing
        }
    }

    @Override
    public void onActivityPaused(Activity activity) {
        if (!checkIsFlutterActivity(activity)) {
            return;
        }
        unregisterListener(activity);
    }

    @Override
    public void onActivityStopped(Activity activity) {

    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle bundle) {
    }

    @Override
    public void onActivityDestroyed(Activity activity) {
    }

    private void unregisterListener(Activity activity) {
        if (activity != null) {
            View currentMainView = getMainView(activity);
            if (currentMainView != null) {
                currentMainView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
            mainView = null;
        }
    }

    public View getMainView(Activity activity) {
        return ((ViewGroup) activity.findViewById(android.R.id.content)).getChildAt(0);
    }

    public boolean checkIsFlutterActivity(Activity activity) {
        return findFlutterView(getMainView(activity));
    }

    public boolean findFlutterView(View contentView) {
        boolean hasFlutterView = false;
        if (contentView instanceof ViewGroup) {
            ViewGroup parentView = (ViewGroup) contentView;
            for (int index = 0; index < parentView.getChildCount(); index++) {
                hasFlutterView = findFlutterView(parentView.getChildAt(index));
                if (hasFlutterView) {
                    break;
                }
            }
        }
        if (contentView instanceof View && !hasFlutterView) {
            hasFlutterView = contentView instanceof FlutterView;
        }
        return hasFlutterView;
    }

    @Override
    public void onListen(Object arguments, final EventSink eventsSink) {
        // register listener
        this.eventsSink = eventsSink;

        // is keyboard is visible at startup, let our subscriber know
        if (isVisible) {
            eventsSink.success(1);
        }
    }

    @Override
    public void onCancel(Object arguments) {
        eventsSink = null;
    }
}
