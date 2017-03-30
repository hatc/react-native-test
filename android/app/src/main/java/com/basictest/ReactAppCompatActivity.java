package com.basictest;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.view.KeyEvent;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import com.facebook.common.logging.FLog;
import com.facebook.infer.annotation.Assertions;
import com.facebook.react.ReactApplication;
import com.facebook.react.ReactInstanceManager;
import com.facebook.react.ReactNativeHost;
import com.facebook.react.ReactRootView;
import com.facebook.react.common.ReactConstants;
import com.facebook.react.devsupport.DoubleTapReloadRecognizer;
import com.facebook.react.modules.core.DefaultHardwareBackBtnHandler;
import com.facebook.react.modules.core.PermissionAwareActivity;
import com.facebook.react.modules.core.PermissionListener;
import com.facebook.react.bridge.Callback;

import javax.annotation.Nullable;

/**
 * Base AppCompatActivity for React Native applications.
 */
public abstract class ReactAppCompatActivity
        extends AppCompatActivity
        implements DefaultHardwareBackBtnHandler, PermissionAwareActivity {
    private final int REQUEST_OVERLAY_PERMISSION_CODE = 1111;
    private static final String REDBOX_PERMISSION_GRANTED_MESSAGE =
            "Overlay permissions have been granted.";
    private static final String REDBOX_PERMISSION_MESSAGE =
            "Overlay permissions needs to be granted in order for react native apps to run in dev mode";

    private @Nullable DoubleTapReloadRecognizer mDoubleTapReloadRecognizer;
    private @Nullable PermissionListener mPermissionListener;
    private @Nullable Callback mPermissionsCallback;

    /**
     * Get the {@link ReactNativeHost} used by this app. By default, assumes
     * {@link Activity#getApplication()} is an instance of {@link ReactApplication} and calls
     * {@link ReactApplication#getReactNativeHost()}. Override this method if your application class
     * does not implement {@code ReactApplication} or you simply have a different mechanism for
     * storing a {@code ReactNativeHost}, e.g. as a static field somewhere.
     */
    protected ReactNativeHost getReactNativeHost() {
        return ((ReactApplication) getApplication()).getReactNativeHost();
    }

    public ReactInstanceManager getReactInstanceManager() {
        return getReactNativeHost().getReactInstanceManager();
    }

    /**
     * In implementation perform necessary initialization and startReactApplication
     * What about .jsbundle reload?
     * */
    protected abstract void mountReactApp();
    protected abstract @Nullable ReactRootView getRootView();
    protected abstract void unmountReactApp();

    // not a onCreate() override, coz we have to create our own layout in onCreate...
    protected void onCreateImpl() {
        boolean needsOverlayPermission = false;
        if (getReactNativeHost().getUseDeveloperSupport() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Get permission to show redbox in dev builds.
            if (!Settings.canDrawOverlays(this)) {
                needsOverlayPermission = true;
                Intent serviceIntent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
                FLog.w(ReactConstants.TAG, REDBOX_PERMISSION_MESSAGE);
                Toast.makeText(this, REDBOX_PERMISSION_MESSAGE, Toast.LENGTH_LONG).show();
                startActivityForResult(serviceIntent, REQUEST_OVERLAY_PERMISSION_CODE);
            }
        }

        if (!needsOverlayPermission)
            mountReactApp();
        mDoubleTapReloadRecognizer = new DoubleTapReloadRecognizer();
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (getReactNativeHost().hasInstance())
            getReactNativeHost().getReactInstanceManager().onHostPause(this);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (getReactNativeHost().hasInstance()) {
            getReactNativeHost().getReactInstanceManager().onHostResume(
                    this,
                    this);
        }

        if (mPermissionsCallback != null) {
            mPermissionsCallback.invoke();
            mPermissionsCallback = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (getRootView() != null)
            unmountReactApp();
        if (getReactNativeHost().hasInstance())
            getReactNativeHost().getReactInstanceManager().onHostDestroy(this);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (getReactNativeHost().hasInstance()) {
            getReactNativeHost()
                    .getReactInstanceManager()
                    .onActivityResult(this, requestCode, resultCode, data);
        } else {
            // Did we request overlay permissions?
            if (requestCode == REQUEST_OVERLAY_PERMISSION_CODE && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (Settings.canDrawOverlays(this)) {
                    if (getRootView() == null)
                        mountReactApp();
                    Toast.makeText(this, REDBOX_PERMISSION_GRANTED_MESSAGE, Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (getReactNativeHost().hasInstance() && getReactNativeHost().getUseDeveloperSupport()) {
            if (keyCode == KeyEvent.KEYCODE_MENU) {
                getReactNativeHost().getReactInstanceManager().showDevOptionsDialog();
                return true;
            }
            boolean didDoubleTapR = Assertions.assertNotNull(mDoubleTapReloadRecognizer)
                    .didDoubleTapR(keyCode, getCurrentFocus());
            if (didDoubleTapR) {
                getReactNativeHost().getReactInstanceManager().getDevSupportManager().handleReloadJS();
                return true;
            }
        }
        return super.onKeyUp(keyCode, event);
    }

    @Override
    public void onBackPressed() {
        if (getReactNativeHost().hasInstance())
            // If JS does not consume the event, invokeDefaultOnBackPressed will be invoked at the end of the round trip to JS.
            getReactNativeHost().getReactInstanceManager().onBackPressed();
        else
            super.onBackPressed();
    }

    @Override
    public void invokeDefaultOnBackPressed() {
        super.onBackPressed();
    }

    @Override
    public void onNewIntent(Intent intent) {
        // hmph, gimme invokeDefaultOnNewIntent pls? ^_^
        // lets make everyone happy
        if (getReactNativeHost().hasInstance())
            getReactNativeHost().getReactInstanceManager().onNewIntent(intent);
        super.onNewIntent(intent);
    }

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    public void requestPermissions(String[] permissions, int requestCode, PermissionListener listener) {
        mPermissionListener = listener;
        requestPermissions(permissions, requestCode);
    }

    @Override
    public void onRequestPermissionsResult(final int requestCode, final @NonNull String[] permissions, final @NonNull int[] grantResults) {
        mPermissionsCallback = new Callback() {
            @Override
            public void invoke(Object... args) {
                if (mPermissionListener != null && mPermissionListener.onRequestPermissionsResult(requestCode, permissions, grantResults)) {
                    mPermissionListener = null;
                }
            }
        };
    }
}
