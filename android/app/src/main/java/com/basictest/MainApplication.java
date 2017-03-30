package com.basictest;

import android.app.Application;
import android.content.res.AssetManager;

import com.facebook.react.ReactApplication;
import com.facebook.react.ReactNativeHost;
import com.facebook.react.ReactPackage;
import com.facebook.react.shell.MainReactPackage;
import com.facebook.soloader.SoLoader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class MainApplication extends Application implements ReactApplication {
    private static final Logger logger = LoggerFactory.getLogger(MainApplication.class);

    public static final String sJSMainModuleName = "index.android";
    public static final String sJSBundleName = sJSMainModuleName + ".bundle";
    // specify appKey for js app used in AppRegistry.registerComponent
    public static final String sJSAppKey = "basictest";

    private final MainModel model = new MainModel();
    // make ReactNativeHost from onCreate() and set js bundle to valid file path
    private final ReactNativeHost reactNativeHost = new ReactNativeHost(this) {
        @Override
        public boolean getUseDeveloperSupport() {
            return BuildConfig.DEBUG;
        }

        @Override
        protected @Nullable String getBundleAssetName() { return sJSBundleName; }

        /**
         * Determines the URL used to fetch the JS bundle from the packager server.
         * It is only used when dev support is enabled.
         * */
        @Override
        protected String getJSMainModuleName() { return sJSMainModuleName; }

        @Override
        protected @Nullable String getJSBundleFile() {
            return null;
            /*ReactNativeHost.createReactInstanceManager():
            * String jsBundleFile = getJSBundleFile();
            * if (jsBundleFile != null) {
            *  builder.setJSBundleFile(jsBundleFile);
            * } else {
            *  builder.setBundleAssetName(Assertions.assertNotNull(getBundleAssetName()));
            * }
            * i.e. if override both, getJSBundleFile() have priority
            * */
        }

        @Override
        protected List<ReactPackage> getPackages() {
            return Arrays.asList(
                    new MainReactPackage(),
                    new Package(model)
            );
        }

        // ReactNativeHost.getReactInstanceManager() method used for create actual ReactInstanceManager
        // with properties from ReactNativeHost - so override methods to configure ReactInstanceManager
    };

    @Override
    public ReactNativeHost getReactNativeHost() {
        return reactNativeHost;
    }

    public MainModel getModel() { return model; }

    private static void logAssets(AssetManager assets, String relativePath) {
        logger.info("logAssets(\"{}\"):", relativePath);
        try {
            for (String v : assets.list(relativePath))
                logger.info("asset: \"{}\"", v);
        } catch (IOException e) { logger.error("logAssets() failed", e); }
    }

    @Override
    public void onCreate() {
        super.onCreate();

        // logAssets(this.getAssets(), "");

        SoLoader.init(this, /* native exopackage */ false);
    }
}
