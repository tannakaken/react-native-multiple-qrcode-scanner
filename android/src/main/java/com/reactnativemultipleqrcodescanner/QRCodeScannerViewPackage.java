package com.reactnativemultipleqrcodescanner;

import androidx.annotation.NonNull;

import com.facebook.react.ReactPackage;
import com.facebook.react.bridge.NativeModule;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.uimanager.ViewManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class QRCodeScannerViewPackage implements ReactPackage {
    @Override
    @NonNull
    public List<ViewManager> createViewManagers(@NonNull ReactApplicationContext reactContext) {
        List<ViewManager> managers = new ArrayList<ViewManager>();
        managers.add(new QRCodeScannerViewManager());
        return managers;
    }

    @Override
    @NonNull
    public List<NativeModule> createNativeModules(@NonNull ReactApplicationContext reactContext) {
        return Collections.emptyList();
    }
}
