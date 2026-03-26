package com.axelor.apps.seo.utils.sse;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.sse.SseBroadcaster;
import javax.ws.rs.sse.SseEventSink;
import java.lang.invoke.MethodHandles;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Path("events")
@Singleton
public class SseWebService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    @Inject
    private SseBroadcasterManager sseBroadcasterManager;
    Map<String, SseEventSink> sseSinks = new ConcurrentHashMap<>();
    Map<SseEventSink, LocalDateTime> oldSseSink = new ConcurrentHashMap<>();

    @GET
    @Produces(MediaType.SERVER_SENT_EVENTS)
    public void messages(final @Context SseEventSink sink, @Context HttpHeaders httpHeaders) throws Exception {

        Cookie cookie = httpHeaders.getCookies().get("CSRF-TOKEN");
        String clientKey = cookie.getName() + "=" + cookie.getValue();

        SseBroadcaster broadcaster = sseBroadcasterManager.getBroadcaster();

        SseEventSink oldSink = sseSinks.get(clientKey);

        if (oldSink != null) {
            try {
                oldSink.close();
            } catch (Exception ignored) {}

            sseSinks.remove(clientKey);
            oldSseSink.remove(oldSink);
        }

        sseSinks.put(clientKey, sink);
        oldSseSink.put(sink, LocalDateTime.now());
        broadcaster.register(sink);
    }

    public void cleanOldSinks(){

        LOGGER.info("Total SSE connections: {}", sseSinks.size());
        LOGGER.info("Old SSE connections: {}", oldSseSink.size());

        LocalDateTime threshold = LocalDateTime.now().minusMinutes(8);

        oldSseSink.entrySet().removeIf(entry -> {
            SseEventSink sink = entry.getKey();
            boolean isOld = entry.getValue().isBefore(threshold) || sink.isClosed();

            if (isOld) {
                try {
                    sink.close();
                } catch (Exception ignored) {}
                sseSinks.entrySet().removeIf(e -> e.getValue().equals(sink));
            }

            return isOld;
        });
    }

}
