package com.sillygirl.client;

import android.os.Bundle;
import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void load() {
        super.load();
        // Disable live-reload server; load static assets from file:// instead
        if (bridge != null && bridge.getServer() != null) {
            bridge.getServer().setStartServer(false);
        }
    }
}
