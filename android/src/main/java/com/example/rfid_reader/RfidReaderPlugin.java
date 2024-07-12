package com.example.rfid_reader;

import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;

import android.app.Activity;
import android.view.KeyEvent;
import android.view.Window;

import androidx.annotation.NonNull;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.plugin.common.EventChannel;
import io.flutter.plugin.common.PluginRegistry;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.accessibility.AccessibilityEvent;
import android.view.ActionMode;
import android.view.SearchEvent;
import android.view.WindowManager.LayoutParams;

import com.nativec.tools.ModuleManager;
import com.nativec.tools.SerialPort;
import com.rfid.RFIDReaderHelper;
import com.rfid.ReaderDataPackageParser;
import com.rfid.ReaderDataPackageProcess;
import com.rfid.rxobserver.RXObserver;
import com.rfid.rxobserver.bean.RXInventoryTag;
import com.rfid.rxobserver.ReaderSetting;

import android.os.Handler;
import android.os.Looper;

import java.io.File;


/**
 * RfidReaderPlugin
 */
public class RfidReaderPlugin implements FlutterPlugin, MethodCallHandler, ActivityAware, EventChannel.StreamHandler {

    private RFIDReaderHelper mRFIDReaderHelper;

    private SerialPort mSerialPort;
    private ReaderSetting m_curReaderSetting;

    private byte mBtRepeat, mBtSession, mBtTarget;

    private int mPos1 = 1, mPos2 = 0;

    private boolean mIsLoop = false;

    private Handler mLoopHandler = new Handler(Looper.getMainLooper());

    private String type = "rfid";

    private Runnable mLoopRunnable = new Runnable() {
        @Override
        public void run() {
            if (mRFIDReaderHelper == null || !mRFIDReaderHelper.isAlive()) {
                return;
            }
            mLoopHandler.removeCallbacks(this);
            mRFIDReaderHelper.customizedSessionTargetInventory(m_curReaderSetting.btReadId,
                    mBtSession,
                    mBtTarget,
                    mBtRepeat);
            mLoopHandler.postDelayed(this, 2000);
        }
    };


    private RXObserver mObserver = new RXObserver() {
        @Override
        protected void onInventoryTag(RXInventoryTag tag) {

            mLoopHandler.post(new Runnable() {
                @Override
                public void run() {
                    String epc = tag.strEPC;
                    String epcAsciiStr = epc.replace(" ", "");
                    String message = String.format("{\"value\":\"%s\", \"type\":\"rfid\"}", epcAsciiStr);
                    eventSink.success(message);
                }
            });


        }


        @Override
        protected void onInventoryTagEnd(final RXInventoryTag.RXInventoryTagEnd tagEnd) {

        }

        @Override
        protected void onExeCMDStatus(final byte cmd, final byte status) {
        }
    };


    public void close() {
        if (mRFIDReaderHelper != null) {
            mRFIDReaderHelper.unRegisterObserver(mObserver);
            ModuleManager.newInstance().setUHFStatus(false);
        }
        if (mSerialPort != null) {
            mSerialPort.close();
        }
    }

    private void setUHFPower(int power) {
        if (mRFIDReaderHelper == null) {
            System.out.println("mReaderHelper - null");
            return;
        }
        mRFIDReaderHelper.setOutputPower(m_curReaderSetting.btReadId, (byte) power);
        m_curReaderSetting.btAryOutputPower = new byte[]{(byte) power};
    }

    private boolean init() {
        if (mSerialPort != null) {
            return true;
        }
        try {
            mSerialPort = new SerialPort(new File("/dev/ttyS4"), 115200, 0);
            mRFIDReaderHelper = RFIDReaderHelper.getDefaultHelper();
            mRFIDReaderHelper.setReader(mSerialPort.getInputStream(), mSerialPort.getOutputStream(), new ReaderDataPackageParser(), new ReaderDataPackageProcess());
            mRFIDReaderHelper.registerObserver(mObserver);
            m_curReaderSetting = ReaderSetting.newInstance();
            mBtRepeat = (byte) 1;
            mBtSession = (byte) (mPos1 & 0xFF);
            mBtTarget = (byte) (mPos2 & 0xFF);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        if (!ModuleManager.newInstance().setUHFStatus(true)) {
            throw new RuntimeException("UHF RFID power on failure,may you open in other" +
                    " Process and do not exit it");
        }
        return true;
    }


    /**
     * 开始或者停止盘存
     *
     * @param startStop bool
     */
    private boolean startStop(boolean startStop) {
        if (mRFIDReaderHelper == null) {
            return false;
        }
        mIsLoop = startStop;

        if (startStop) {

            mBtRepeat = (byte) 1;
            mBtSession = (byte) (mPos1 & 0xFF);
            mBtTarget = (byte) (mPos2 & 0xFF);
            mLoopRunnable.run();
        } else {
            mLoopHandler.removeCallbacks(mLoopRunnable);
        }

        return true;
    }


    private FlutterPluginBinding pluginBinding;

    private Activity activity;

    private ActivityPluginBinding activityBinding;

    private MethodChannel channel;

    private Context context;

    private EventChannel keyEventChannel;

    private EventChannel.EventSink eventSink;

    private long downLastKeyEventTime = 0;

    private long upLastKeyEventTime = 0;

    @Override
    public void onAttachedToEngine(final FlutterPluginBinding binding) {
        context = binding.getApplicationContext();
        this.pluginBinding = binding;
        keyEventChannel = new EventChannel(binding.getBinaryMessenger(), "my_key_event_channel");
        keyEventChannel.setStreamHandler(this);
    }

    @Override
    public void onDetachedFromEngine(final FlutterPluginBinding binding) {
        this.pluginBinding = null;
    }

    @Override
    public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
        if (call.method.equals("getPlatformVersion")) {
            result.success("Android " + android.os.Build.VERSION.RELEASE);
        } else if (call.method.equals("init")) {
            boolean res = this.init();
            result.success(res ? "true" : "false");
        } else if (call.method.equals("setUHFPower")) {
            int power = call.argument("power");
            this.setUHFPower(power);
            result.success("true");
        } else if (call.method.equals("changeType")) {
            type = call.argument("type");
            result.success("true");
        } else {
            result.notImplemented();
        }
    }

    @Override
    public void onListen(Object arguments, EventChannel.EventSink events) {
        eventSink = events;
    }

    @Override
    public void onCancel(Object arguments) {
        eventSink = null;
    }

    @Override
    public void onAttachedToActivity(final ActivityPluginBinding binding) {
        this.activityBinding = binding;
        this.activity = this.activityBinding.getActivity();
        channel = new MethodChannel(this.pluginBinding.getBinaryMessenger(), "rfid_reader");
        channel.setMethodCallHandler(this);
        if (activity != null) {
            activity.getWindow().getCallback();
            Window.Callback originalCallback = activity.getWindow().getCallback();
            activity.getWindow().setCallback(new Window.Callback() {

                @Override
                public boolean dispatchKeyEvent(KeyEvent event) {
                    if (eventSink != null) {
                        switch (event.getAction()) {
                            case KeyEvent.ACTION_DOWN:
                                if (downLastKeyEventTime == event.getEventTime()) {
                                    break;
                                }
                                if (type == "") {
                                    break;
                                }
                                downLastKeyEventTime = event.getEventTime();
                                if (type == "rfid") {
                                    startStop(true);
                                }
                                eventSink.success("{\"value\":\"onKeyDown\", \"type\":\"onKeyDown\"}");
                                break;
                            case KeyEvent.ACTION_UP:
                                if (upLastKeyEventTime == event.getEventTime()) {
                                    break;
                                }
                                if (type == "") {
                                    break;
                                }
                                upLastKeyEventTime = event.getEventTime();
                                if (type == "rfid") {
                                    startStop(false);
                                    eventSink.success("{\"value\":\"onKeyUp\", \"type\":\"rfidOnKeyUp\"}");
                                } else {
                                    eventSink.success("{\"value\":\"onKeyUp\", \"type\":\"onKeyUp\"}");
                                }
                                break;
                        }
                    }
                    return originalCallback.dispatchKeyEvent(event);
                }

                @Override
                public boolean dispatchKeyShortcutEvent(KeyEvent event) {
                    return originalCallback.dispatchKeyShortcutEvent(event);
                }

                @Override
                public boolean dispatchTouchEvent(MotionEvent event) {
                    return originalCallback.dispatchTouchEvent(event);
                }

                @Override
                public boolean dispatchTrackballEvent(MotionEvent event) {
                    return originalCallback.dispatchTrackballEvent(event);
                }

                @Override
                public boolean dispatchGenericMotionEvent(MotionEvent event) {
                    return originalCallback.dispatchGenericMotionEvent(event);
                }

                @Override
                public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent event) {
                    return originalCallback.dispatchPopulateAccessibilityEvent(event);
                }

                @Override
                public View onCreatePanelView(int featureId) {
                    return originalCallback.onCreatePanelView(featureId);
                }

                @Override
                public boolean onCreatePanelMenu(int featureId, Menu menu) {
                    return originalCallback.onCreatePanelMenu(featureId, menu);
                }

                @Override
                public boolean onPreparePanel(int featureId, View view, Menu menu) {
                    return originalCallback.onPreparePanel(featureId, view, menu);
                }

                @Override
                public boolean onMenuOpened(int featureId, Menu menu) {
                    return originalCallback.onMenuOpened(featureId, menu);
                }

                @Override
                public boolean onMenuItemSelected(int featureId, MenuItem item) {
                    return originalCallback.onMenuItemSelected(featureId, item);
                }

                @Override
                public void onWindowAttributesChanged(LayoutParams attrs) {
                    originalCallback.onWindowAttributesChanged(attrs);
                }

                @Override
                public void onContentChanged() {
                    originalCallback.onContentChanged();
                }

                @Override
                public void onWindowFocusChanged(boolean hasFocus) {
                    originalCallback.onWindowFocusChanged(hasFocus);
                }

                @Override
                public void onAttachedToWindow() {
                    originalCallback.onAttachedToWindow();
                }

                @Override
                public void onDetachedFromWindow() {
                    originalCallback.onDetachedFromWindow();
                }

                @Override
                public void onPanelClosed(int featureId, Menu menu) {
                    originalCallback.onPanelClosed(featureId, menu);
                }

                @Override
                public boolean onSearchRequested() {
                    return originalCallback.onSearchRequested();
                }

                @Override
                public boolean onSearchRequested(SearchEvent searchEvent) {
                    return originalCallback.onSearchRequested(searchEvent);
                }

                @Override
                public ActionMode onWindowStartingActionMode(ActionMode.Callback callback) {
                    return originalCallback.onWindowStartingActionMode(callback);
                }

                @Override
                public ActionMode onWindowStartingActionMode(ActionMode.Callback callback, int type) {
                    return originalCallback.onWindowStartingActionMode(callback, type);
                }

                @Override
                public void onActionModeStarted(ActionMode mode) {
                    originalCallback.onActionModeStarted(mode);
                }

                @Override
                public void onActionModeFinished(ActionMode mode) {
                    originalCallback.onActionModeFinished(mode);
                }
            });
        }

    }

    @Override
    public void onDetachedFromActivityForConfigChanges() {
        this.activityBinding = null;
        this.channel.setMethodCallHandler(null);
        this.channel = null;
    }

    @Override
    public void onReattachedToActivityForConfigChanges(final ActivityPluginBinding binding) {
        this.onAttachedToActivity(binding);
    }


    @Override
    public void onDetachedFromActivity() {
        this.activityBinding = null;
        this.channel.setMethodCallHandler(null);
        this.channel = null;
    }


}
