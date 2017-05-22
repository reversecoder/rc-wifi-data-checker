package com.reversecoder.wifidatachecker.application;

import android.app.Application;

public class WifiDataCheckerApplication extends Application {

  private static WifiDataCheckerApplication instance;

  @Override public void onCreate() {
    super.onCreate();
    instance = this;
  }

  public static WifiDataCheckerApplication getInstance() {
    return instance;
  }
}
