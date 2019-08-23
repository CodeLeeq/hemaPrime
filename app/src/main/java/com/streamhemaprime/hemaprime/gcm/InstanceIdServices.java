package com.streamhemaprime.hemaprime.gcm;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;
import com.streamhemaprime.hemaprime.util.sharedpref.PrefKeys;
import com.streamhemaprime.hemaprime.util.sharedpref.PrefUtils;

public class InstanceIdServices extends FirebaseInstanceIdService {

    @Override
    public void onTokenRefresh() {
        super.onTokenRefresh();
        String refreshedToken = FirebaseInstanceId.getInstance().getToken();
        saveDeviceToken(refreshedToken);
    }

    private void saveDeviceToken(String token) {
        PrefUtils.getInstance(this).setValue(PrefKeys.FCM_TOKEN, token);
    }

}