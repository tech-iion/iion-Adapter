package com.admob.custom.adapter;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;

import com.iion.api.AlxAdSDK;
import com.iion.api.AlxInterstitialAD;
import com.iion.api.AlxInterstitialADListener;
import com.iion.api.AlxSdkInitCallback;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.mediation.Adapter;
import com.google.android.gms.ads.mediation.InitializationCompleteCallback;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationConfiguration;
import com.google.android.gms.ads.mediation.MediationInterstitialAd;
import com.google.android.gms.ads.mediation.MediationInterstitialAdCallback;
import com.google.android.gms.ads.mediation.MediationInterstitialAdConfiguration;
import com.google.android.gms.ads.mediation.VersionInfo;

import org.json.JSONObject;

import java.util.List;

/**
 * Google Mobile ads IION Interstitial Adapter
 */
public class IionInterstitialAdapter extends Adapter implements MediationInterstitialAd {

    private static final String TAG = "IionInterstitialAdapter";

    private String unitid = "";
    private String appid = "";
    private String sid = "";
    private String token = "";
    private Boolean isdebug = false;

    private MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback> mMediationLoadCallback;
    private MediationInterstitialAdCallback mMediationEventCallback;

    AlxInterstitialAD interstitialAd;

    @Override
    public void initialize(Context context, InitializationCompleteCallback initializationCompleteCallback, List<MediationConfiguration> list) {
        Log.d(TAG, "alx-admob-adapter: initialize");
        if (context == null) {
            initializationCompleteCallback.onInitializationFailed(
                    "Initialization Failed: Context is null.");
            return;
        }
        initializationCompleteCallback.onInitializationSucceeded();
    }

    @Override
    public void loadInterstitialAd(@NonNull MediationInterstitialAdConfiguration configuration, @NonNull MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback> callback) {
        Log.d(TAG, "alx-admob-adapter-version:" + IionMetaInf.ADAPTER_VERSION);
        Log.d(TAG, "alx-admob-adapter: loadInterstitialAd");
        mMediationLoadCallback = callback;
        String parameter = configuration.getServerParameters().getString("parameter");
        if (!TextUtils.isEmpty(parameter)) {
            parseServer(parameter);
        }
        initSdk(configuration.getContext());
    }

    private void initSdk(final Context context) {
        if (TextUtils.isEmpty(unitid)) {
            Log.d(TAG, "alx unitid is empty");
            loadError(1, "alx unitid is empty.");
            return;
        }
        if (TextUtils.isEmpty(sid)) {
            Log.d(TAG, "alx sid is empty");
            loadError(1, "alx sid is empty.");
            return;
        }
        if (TextUtils.isEmpty(appid)) {
            Log.d(TAG, "alx appid is empty");
            loadError(1, "alx appid is empty.");
            return;
        }
        if (TextUtils.isEmpty(token)) {
            Log.d(TAG, "alx token is empty");
            loadError(1, "alx token is empty");
            return;
        }

        try {
            Log.i(TAG, "alx token: " + token + " alx appid: " + appid + "alx sid: " + sid);
            // init
            AlxAdSDK.init(context, token, sid, appid, new AlxSdkInitCallback() {
                @Override
                public void onInit(boolean isOk, String msg) {
                    preloadAd(context);
                }
            });
//                // set GDPR
//                // Subject to GDPR Flag: Please pass a Boolean value to indicate if the user is subject to GDPR regulations or not.
//                // Your app should make its own determination as to whether GDPR is applicable to the user or not.
//                AlxAdSDK.setSubjectToGDPR(true);
//                // set GDPR Consent
//                AlxAdSDK.setUserConsent("1");
//                // set COPPA
//                AlxAdSDK.setBelowConsentAge(true);
//                // set CCPA
//                AlxAdSDK.subjectToUSPrivacy("1YYY");
            AlxAdSDK.setDebug(isdebug);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
            e.printStackTrace();
            loadError(1, "alx sdk init error");
        }
    }

    @Override
    public void showAd(@NonNull Context context) {
        Log.i(TAG, "alx showAd");
        if (interstitialAd != null) {
            if (!interstitialAd.isReady()) {
                if (mMediationEventCallback != null) {
                    mMediationEventCallback.onAdFailedToShow(new AdError(1, "isReady: Ad not loaded or Ad load failed"
                            , AlxAdSDK.getNetWorkName()));
                }
                return;
            }
            if (context != null && context instanceof Activity) {
                interstitialAd.show((Activity) context);
            } else {
                Log.i(TAG, "context is not an Activity");
                interstitialAd.show(null);
            }
        }
    }

    private void loadError(int code, String message) {
        if (mMediationLoadCallback != null) {
            mMediationLoadCallback.onFailure(new AdError(code, message, AlxAdSDK.getNetWorkName()));
        }
    }

    private void parseServer(String s) {
        if (TextUtils.isEmpty(s)) {
            Log.d(TAG, "serviceString  is empty ");
            return;
        }
        Log.d(TAG, "serviceString   " + s);
        try {
            JSONObject json = new JSONObject(s);
            appid = json.getString("appid");
            sid = json.getString("sid");
            token = json.getString("token");
            unitid = json.getString("unitid");
            String debug = json.optString("isdebug", "false");
            if (TextUtils.equals(debug, "true")) {
                isdebug = true;
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage() + "");
        }
    }

    private void preloadAd(final Context context) {
        interstitialAd = new AlxInterstitialAD();
        interstitialAd.load(context, unitid, new AlxInterstitialADListener() {

            @Override
            public void onInterstitialAdLoaded() {
                if (mMediationLoadCallback != null) {
                    mMediationEventCallback = mMediationLoadCallback.onSuccess(IionInterstitialAdapter.this);
                }
            }

            @Override
            public void onInterstitialAdLoadFail(int errorCode, String errorMsg) {
                loadError(errorCode, errorMsg);
            }

            @Override
            public void onInterstitialAdClicked() {
                if (mMediationEventCallback != null) {
                    mMediationEventCallback.reportAdClicked();
                }
            }

            @Override
            public void onInterstitialAdShow() {
                if (mMediationEventCallback != null) {
                    mMediationEventCallback.reportAdImpression();
                    mMediationEventCallback.onAdOpened();
                }
            }

            @Override
            public void onInterstitialAdClose() {
                if (mMediationEventCallback != null) {
                    mMediationEventCallback.onAdClosed();
                }
            }

            @Override
            public void onInterstitialAdVideoStart() {

            }

            @Override
            public void onInterstitialAdVideoEnd() {

            }

            @Override
            public void onInterstitialAdVideoError(int errorCode, String errorMsg) {
            }
        });
    }


    @Override
    public VersionInfo getVersionInfo() {
        String versionString = AlxAdSDK.getNetWorkVersion();
        VersionInfo result = getAdapterVersionInfo(versionString);
        if (result != null) {
            return result;
        }
        return new VersionInfo(0, 0, 0);
    }

    @Override
    public VersionInfo getSDKVersionInfo() {
        String versionString = AlxAdSDK.getNetWorkVersion();
        VersionInfo result = getAdapterVersionInfo(versionString);
        if (result != null) {
            return result;
        }
        return new VersionInfo(0, 0, 0);
    }

    private VersionInfo getAdapterVersionInfo(String version) {
        if (TextUtils.isEmpty(version)) {
            return null;
        }
        try {
            String[] arr = version.split("\\.");
            if (arr == null || arr.length < 3) {
                return null;
            }
            int major = Integer.parseInt(arr[0]);
            int minor = Integer.parseInt(arr[1]);
            int micro = Integer.parseInt(arr[2]);
            return new VersionInfo(major, minor, micro);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}