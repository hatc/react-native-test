package com.basictest;

import android.os.Bundle;
import android.view.Gravity;
import android.widget.FrameLayout;

import com.facebook.react.ReactRootView;

import javax.annotation.Nullable;

public class MainActivity extends ReactAppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        setSupportActionBar((android.support.v7.widget.Toolbar)findViewById(R.id.toolbar));

        onCreateImpl();

        presenter = new MainPresenter(this, ((MainApplication)getApplication()).getModel());
    }
    private MainPresenter presenter;

    private ReactRootView reactRootView;
    @Override
    protected void mountReactApp() {
        if (reactRootView != null)
            throw new IllegalStateException("react app is already mounted and running");

        reactRootView = new ReactRootView(this);
        reactRootView.startReactApplication(
                getReactNativeHost().getReactInstanceManager(),
                MainApplication.sJSAppKey,
                null);

        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
                Gravity.CENTER);
        ((FrameLayout) findViewById(R.id.reactViewPlaceholder)).addView(reactRootView, layoutParams);

        /*
         * https://github.com/facebook/react-native/tree/master/packager
         *
         * GET /path/to/moduleName.bundle
         * Does the following in order:
         *  parse out path/to/moduleName
         *  add a .js suffix to the path
         *  looks in your project root(s) for the file
         *  ... */
    }
    @Override
    protected @Nullable ReactRootView getRootView() {
        return reactRootView;
    }
    @Override
    protected void unmountReactApp() {
        if (reactRootView != null) {
            reactRootView.unmountReactApplication();
            reactRootView = null;
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return false;
    }
}
