package org.prism.apm.parser.utils;

import java.util.ArrayList;
import java.util.List;

/**
 * Cache entry size estimator.
 *
 * Assumptions:
 * 1. 64-bit HotSpot JVM
 * 2. Compressed OOPs enabled
 * 3. JDK 9+ compact strings enabled
 * 4. String content is mostly ASCII, so String uses byte[]
 *
 * This is an ESTIMATION tool, not an exact JVM memory calculator.
 *
 * A cache entry example of SpanInfo:
 * {
 *     "hostName": "sample-consumer-v2",
 *     "spanKind": 3,
 *     "component": "io.opentelemetry.apache-dubbo-2.7",
 *     "duration": 35121,
 *     "serviceName": "feign-consumer",
 *     "actionName": "",
 *     "spanId": "eaa3ce8c98bc3101",
 *     "traceId": "7857d529d3e0cdf685529be20e1762a4",
 *     "error": 0,
 *     "time": 1755761611365223,
 *     "parentSpanId": "0dc46c38cc495db8",
 *     "remoteAddr": "172.18.0.16",
 *     "targetName": "com.sample.provider.api.dubbo.GreetingService/sayHello",
 *     "systemType": "apache_dubbo",
 *     "serverArrivalTime": 1776064985450
 * }
 *
 *
 */
public final class CacheEntrySizeCalculator {

    /**
     * Estimated base overhead for one cache entry.
     *
     * This includes:
     * - cache node / wrapper / references / metadata
     * - some map structure overhead
     *
     * You can tune this value according to your real cache implementation.
     */
    public static final long ENTRY_BASE_SIZE = 128L;

    /**
     * Approximate size of a String object itself, excluding backing byte[] content.
     * On modern HotSpot JVM, String object is roughly 24 bytes.
     */
    public static final long STRING_OBJECT_SIZE = 24L;

    /**
     * Approximate size of a byte[] header.
     */
    public static final long BYTE_ARRAY_HEADER_SIZE = 16L;

    /**
     * Primitive sizes
     */
    public static final long LONG_SIZE = 8L;
    public static final long INT_SIZE = 4L;
    public static final long SHORT_SIZE = 2L;
    public static final long BOOLEAN_SIZE = 1L;
    public static final long BYTE_SIZE = 1L;

    /**
     * Approximate size of an object reference with compressed oops.
     */
    public static final long REFERENCE_SIZE = 4L;

    private final List<Long> parts = new ArrayList<Long>();

    public CacheEntrySizeCalculator() {
        parts.add(ENTRY_BASE_SIZE);
    }

    /**
     * Add a String field by actual content.
     * Null string will be treated as one null reference only.
     */
    public CacheEntrySizeCalculator addString(String value) {
        parts.add(calculateStringSize(value));
        return this;
    }

    /**
     * Add a String field by content length.
     * This is useful when you do not have the real content,
     * but know the approximate length.
     *
     * @param length string content length
     */
    public CacheEntrySizeCalculator addStringByLength(int length) {
        parts.add(calculateStringSizeByLength(length));
        return this;
    }

    /**
     * Add a primitive long field.
     */
    public CacheEntrySizeCalculator addLong() {
        parts.add(LONG_SIZE);
        return this;
    }

    /**
     * Add a boxed Long field.
     * Approximate:
     * - reference: 4
     * - Long object: roughly 24 bytes aligned
     */
    public CacheEntrySizeCalculator addBoxedLong() {
        parts.add(REFERENCE_SIZE + 24L);
        return this;
    }

    /**
     * Add a primitive int field.
     */
    public CacheEntrySizeCalculator addInt() {
        parts.add(INT_SIZE);
        return this;
    }

    /**
     * Add a primitive short field.
     */
    public CacheEntrySizeCalculator addShort() {
        parts.add(SHORT_SIZE);
        return this;
    }

    /**
     * Add a boxed Short field.
     * Approximate:
     * - reference: 4
     * - Short object: roughly 16 bytes aligned
     */
    public CacheEntrySizeCalculator addBoxedShort() {
        parts.add(REFERENCE_SIZE + 16L);
        return this;
    }

    /**
     * Add a primitive boolean field.
     */
    public CacheEntrySizeCalculator addBoolean() {
        parts.add(BOOLEAN_SIZE);
        return this;
    }

    /**
     * Add a custom size directly.
     */
    public CacheEntrySizeCalculator addCustom(long size) {
        if (size < 0) {
            throw new IllegalArgumentException("size must be >= 0");
        }
        parts.add(size);
        return this;
    }

    /**
     * Calculate total bytes.
     */
    public long calculate() {
        long total = 0L;
        for (Long part : parts) {
            total += part.longValue();
        }
        return align8(total);
    }

    /**
     * Calculate String size by actual value.
     */
    public static long calculateStringSize(String value) {
        if (value == null) {
            return REFERENCE_SIZE;
        }
        return calculateStringSizeByLength(value.length());
    }

    /**
     * Calculate String size by content length.
     *
     * Assumption:
     * - ASCII content => Compact String => byte[]
     * - String object ~= 24 bytes
     * - byte[] header ~= 16 bytes
     * - payload = length bytes
     */
    public static long calculateStringSizeByLength(int length) {
        if (length < 0) {
            throw new IllegalArgumentException("length must be >= 0");
        }

        long byteArraySize = BYTE_ARRAY_HEADER_SIZE + length;
        byteArraySize = align8(byteArraySize);

        return STRING_OBJECT_SIZE + byteArraySize;
    }

    /**
     * 8-byte alignment.
     */
    public static long align8(long size) {
        long remainder = size % 8;
        return remainder == 0 ? size : (size + 8 - remainder);
    }


    public static void main(String[] args) {
        // Estimate the cache entry size for SpanInfo
        long size = new CacheEntrySizeCalculator()
                .addStringByLength(50)  // hostName
                .addBoxedShort()        // spanKind
                .addStringByLength(50)  // component
                .addBoxedLong()         // duration
                .addStringByLength(40)  // serviceName
                .addStringByLength(40)   // actionName
                .addStringByLength(16)  // spanId
                .addStringByLength(32)  // traceId
                .addBoxedLong()         // error
                .addBoxedLong()         // time
                .addStringByLength(16)  // parentSpanId
                .addStringByLength(15)  // remoteAddr
                .addStringByLength(70)  // targetName
                .addStringByLength(20)  // systemType
                .addLong()              // serverArrivalTime
                .calculate();
        System.out.println("SpanInfo cache entry size: " + size + " bytes");
    }


}