package io.invertase.admob;

/*
 * Copyright (c) 2016-present Invertase Limited & Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this library except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

import android.app.Activity;
import android.util.SparseArray;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.InterstitialAd;

import javax.annotation.Nullable;

import io.invertase.admob.common.ReactNativeModule;
import io.invertase.admob.ReactNativeAdMobEvent;

import static io.invertase.admob.ReactNativeAdMobCommon.buildAdRequest;
import static io.invertase.admob.ReactNativeAdMobCommon.getCodeAndMessageFromAdErrorCode;
import static io.invertase.admob.ReactNativeAdMobCommon.sendAdEvent;
import static io.invertase.admob.ReactNativeAdMobEvent.AD_CLICKED;
import static io.invertase.admob.ReactNativeAdMobEvent.AD_CLOSED;
import static io.invertase.admob.ReactNativeAdMobEvent.AD_ERROR;
import static io.invertase.admob.ReactNativeAdMobEvent.AD_LEFT_APPLICATION;
import static io.invertase.admob.ReactNativeAdMobEvent.AD_LOADED;
import static io.invertase.admob.ReactNativeAdMobEvent.AD_OPENED;

public class ReactNativeAdMobInterstitialModule extends ReactNativeModule {
  private static final String SERVICE = "AdMobInterstitial";
  private static SparseArray<InterstitialAd> interstitialAdArray = new SparseArray<>();

  public ReactNativeAdMobInterstitialModule(ReactApplicationContext reactContext) {
    super(reactContext, SERVICE);
  }

  private void sendInterstitialEvent(String type, int requestId, String adUnitId, @Nullable WritableMap error) {
    sendAdEvent(
      ReactNativeAdMobEvent.EVENT_INTERSTITIAL,
      requestId,
      type,
      adUnitId,
      error
    );
  }

  @ReactMethod
  public void interstitialLoad(int requestId, String adUnitId, ReadableMap adRequestOptions) {
    Activity currentActivity = getCurrentActivity();
    if (currentActivity == null) {
      WritableMap error = Arguments.createMap();
      error.putString("code", "null-activity");
      error.putString("message", "Interstitial ad attempted to load but the current Activity was null.");
      sendInterstitialEvent(AD_ERROR, requestId, adUnitId, error);
      return;
    }
    currentActivity.runOnUiThread(() -> {
      InterstitialAd interstitialAd = new InterstitialAd(currentActivity);
      interstitialAd.setAdUnitId(adUnitId);

      // Apply AdRequest builder
      interstitialAd.loadAd(buildAdRequest(adRequestOptions));

      interstitialAd.setAdListener(new AdListener() {
        @Override
        public void onAdLoaded() {
          sendInterstitialEvent(AD_LOADED, requestId, adUnitId, null);
        }

        @Override
        public void onAdFailedToLoad(int errorCode) {
          WritableMap error = Arguments.createMap();
          String[] codeAndMessage = getCodeAndMessageFromAdErrorCode(errorCode);
          error.putString("code", codeAndMessage[0]);
          error.putString("message", codeAndMessage[1]);
          sendInterstitialEvent(AD_ERROR, requestId, adUnitId, error);
        }

        @Override
        public void onAdOpened() {
          sendInterstitialEvent(AD_OPENED, requestId, adUnitId, null);
        }

        @Override
        public void onAdClicked() {
          sendInterstitialEvent(AD_CLICKED, requestId, adUnitId, null);
        }

        @Override
        public void onAdLeftApplication() {
          sendInterstitialEvent(AD_LEFT_APPLICATION, requestId, adUnitId, null);
        }

        @Override
        public void onAdClosed() {
          sendInterstitialEvent(AD_CLOSED, requestId, adUnitId, null);
        }
      });

      interstitialAdArray.put(requestId, interstitialAd);
    });
  }

  @ReactMethod
  public void interstitialShow(int requestId, ReadableMap showOptions, Promise promise) {
    if (getCurrentActivity() == null) {
      rejectPromiseWithCodeAndMessage(promise, "null-activity", "Interstitial ad attempted to show but the current Activity was null.");
      return;
    }
    getCurrentActivity().runOnUiThread(() -> {
      InterstitialAd interstitialAd = interstitialAdArray.get(requestId);
      if (interstitialAd == null) {
        rejectPromiseWithCodeAndMessage(promise, "null-interstitialAd", "Interstitial ad attempted to show but its object was null.");
        return;
      }

      if (showOptions.hasKey("immersiveModeEnabled")) {
        interstitialAd.setImmersiveMode(showOptions.getBoolean("immersiveModeEnabled"));
      } else {
        interstitialAd.setImmersiveMode(false);
      }

      if (interstitialAd.isLoaded()) {
        interstitialAd.show();
        promise.resolve(null);
      } else {
        rejectPromiseWithCodeAndMessage(promise, "not-ready", "Interstitial ad attempted to show but was not ready.");
      }
    });
  }
}