package org.prism.apm.parser.cache.listener;

import lombok.extern.slf4j.Slf4j;
import org.ehcache.event.CacheEvent;
import org.ehcache.event.CacheEventListener;

@Slf4j
public class CacheEventLogger implements CacheEventListener<Object, Object> {


    @Override
    public void onEvent(CacheEvent<?, ?> event) {
        switch (event.getType()) {
            case EVICTED:
                log.trace("Cache entry evicted. span id: {}", event.getKey());
                break;
            case EXPIRED:
                log.trace("Cache entry expired. span id: {}", event.getKey());
                break;
            default:
                // Optionally log other events if needed
                // log.debug("Cache event: {}, Key: {}", event.getType(), event.getKey());
                break;
        }
    }
}
