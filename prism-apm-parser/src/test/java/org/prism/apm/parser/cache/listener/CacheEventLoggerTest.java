package org.prism.apm.parser.cache.listener;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import org.ehcache.Cache;
import org.ehcache.CacheManager;
import org.ehcache.config.CacheConfiguration;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheEventListenerConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.config.builders.ExpiryPolicyBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.ehcache.config.units.EntryUnit;
import org.ehcache.event.EventType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;

class CacheEventLoggerTest {

    private CacheManager cacheManager;
    private Cache<String, String> testCache;
    private InMemoryLogAppender logAppender;
    private Logger cacheLogger;

    // Helper class to capture log events in memory
    static class InMemoryLogAppender extends AppenderBase<ILoggingEvent> {
        private final List<ILoggingEvent> events = new ArrayList<>();

        @Override
        protected void append(ILoggingEvent eventObject) {
            events.add(eventObject);
        }

        public List<ILoggingEvent> getEvents() {
            return events;
        }

        public boolean containsMessage(String message) {
            return events.stream().anyMatch(event -> event.getFormattedMessage().contains(message));
        }
    }

    @BeforeEach
    void setUp() {
        // 1. Set up the in-memory log appender
        logAppender = new InMemoryLogAppender();
        logAppender.start();
        cacheLogger = (Logger) LoggerFactory.getLogger(CacheEventLogger.class);
        cacheLogger.addAppender(logAppender);
        cacheLogger.setLevel(ch.qos.logback.classic.Level.TRACE);


        // 2. Configure the event listener
        CacheEventListenerConfigurationBuilder listenerConfig = CacheEventListenerConfigurationBuilder
                .newEventListenerConfiguration(new CacheEventLogger(), EventType.EXPIRED, EventType.EVICTED)
                .synchronous();

        // 3. Configure the cache with small size and short TTL to trigger events
        CacheConfiguration<String, String> cacheConfiguration = CacheConfigurationBuilder.newCacheConfigurationBuilder(
                        String.class, String.class,
                        ResourcePoolsBuilder.newResourcePoolsBuilder().heap(1, EntryUnit.ENTRIES)
                )
                .withExpiry(ExpiryPolicyBuilder.timeToLiveExpiration(Duration.ofSeconds(1)))
                .withService(listenerConfig)
                .build();

        // 4. Build the cache manager and create the cache
        cacheManager = CacheManagerBuilder.newCacheManagerBuilder()
                .withCache("testCache", cacheConfiguration)
                .build(true); // auto-init

        testCache = cacheManager.getCache("testCache", String.class, String.class);
    }

    @AfterEach
    void tearDown() {
        if (cacheManager != null) {
            cacheManager.close();
        }
        if (cacheLogger != null && logAppender != null) {
            cacheLogger.detachAppender(logAppender);
        }
    }

    @Test
    void testEvictionEventIsLogged() {
        // Act
        testCache.put("key1", "value1");
        testCache.put("key2", "value2"); // This should evict key1

        // Give async listener a moment to process
        try {
            TimeUnit.MILLISECONDS.sleep(500); // Increased sleep time
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        assertTrue(logAppender.containsMessage("Cache entry evicted. span id: key1"),
                "Log should contain eviction message for key1");
    }

    @Test
    void testExpirationEventIsLogged() throws InterruptedException {
        // Act
        testCache.put("key1", "value1");
        // Wait for the entry to expire
        TimeUnit.SECONDS.sleep(2);
        testCache.get("key1"); // Accessing the key often triggers the expiration event processing
        // Give async listener a moment to process
        try {
            TimeUnit.MILLISECONDS.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Assert
        assertTrue(logAppender.containsMessage("Cache entry expired. span id: key1"),
                "Log should contain expiration message for key1");
    }
}
