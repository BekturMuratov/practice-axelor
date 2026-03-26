package com.axelor.apps.seo.utils.sse;

import com.google.inject.Provider;
import org.jboss.resteasy.plugins.providers.sse.SseImpl;

import javax.ws.rs.sse.Sse;

public class SseProvider implements Provider<Sse> {
    @Override
    public Sse get() {
        return new SseImpl();
    }
}
