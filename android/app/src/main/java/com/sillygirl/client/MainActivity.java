package com.sillygirl.client;

import android.os.Bundle;
import com.getcapacitor.Bridge;
import com.getcapacitor.BridgeActivity;
import com.getcapacitor.Config;

public class MainActivity extends BridgeActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected Bridge init(BridgeServer server) {
        Bridge bridge = new Bridge(
            Config.load(this.getAssets(), "capacitor.config.json"),
            new ArrayList<>(),
            this,
            server
        );
        // Don't start the live-reload server
        bridge.getServer().setStartServer(false);
        return bridge;
    }
}
