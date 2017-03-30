package com.basictest;

import android.app.Activity;
import android.support.v4.app.FragmentActivity;

import com.facebook.react.ReactActivityDelegate;

import javax.annotation.Nullable;
/**
 * loadApp overridden so we could instantiate our own layout in MainActivity
 *
 * well idea with Delegate is good, but ReactActivity extends Activity, not AppCompatActivity
 * and these faggots makes all ReactActivityDelegate methods protected, so they could be used only from same package...
 */
class ReactDelegate extends ReactActivityDelegate {
    public ReactDelegate(Activity activity, @Nullable String mainComponentName) {
        super(activity, mainComponentName);
    }

    public ReactDelegate(FragmentActivity fragmentActivity, @Nullable String mainComponentName) {
        super(fragmentActivity, mainComponentName);
    }

    @Override
    protected void loadApp(String appKey) {
        // nop - create RootView in Activity
    }
}
