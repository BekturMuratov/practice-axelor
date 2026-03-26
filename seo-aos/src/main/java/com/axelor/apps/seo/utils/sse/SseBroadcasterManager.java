package com.axelor.apps.seo.utils.sse;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import javax.ws.rs.sse.Sse;
import javax.ws.rs.sse.SseBroadcaster;

@Singleton
public class SseBroadcasterManager {
    private SseBroadcaster sseBroadcaster;

    private final Sse sse;

    @Inject
    public SseBroadcasterManager(Sse sse) {
        this.sse = sse;
    }

    public SseBroadcaster getBroadcaster() {
        if (sseBroadcaster == null) {
            sseBroadcaster = sse.newBroadcaster();
        }
        return sseBroadcaster;
    }
}
