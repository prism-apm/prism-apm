package org.prism.apm.parser.service.save;

import org.prism.apm.parser.cache.CacheService;
import org.prism.apm.parser.model.entity.ServiceCallEntity;
import org.prism.apm.parser.model.span.SpanInfo;
import org.prism.apm.parser.repository.ServiceCallMapper;
import org.prism.apm.parser.service.common.JsonService;
import org.prism.apm.parser.service.save.bo.SpanSaveProcessor;
import org.prism.apm.parser.service.stats.StatsService;
import jakarta.annotation.Resource;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;

import org.prism.apm.parser.model.entity.ResourceHashDictionaryEntity;

import java.util.*;

/**
 * @author john
 * @date 2024-06-06 17:49
 */
@Service
@Slf4j
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SpanSaveService {

    // TODO: 2024/8/26 How is transaction defined?
    @Resource
    private ServiceCallMapper svcCallMapper;

    @Resource
    private CacheService cacheService;

    @Resource
    private SpanSaveProcessor spanSaveProcessor;

    @Resource
    private StatsService statsService;

    @Resource
    private JsonService jsonService;

    @Resource
    private org.prism.apm.parser.repository.ResourceHashDictionaryMapper resourceHashDictionaryMapper;

    @Resource
    private ResourceHashResolver resourceHashResolver;

    // todo:junit
    public List<String> handle(List<String> spanIdList, boolean isReconsumption) {

        log.trace("processing spans size:" + spanIdList.size());

        // spans to save
        List<ServiceCallEntity> sysCallEntites = new ArrayList<>();
        // spans to delay process
        List<String> delayedProcessingSpanIds = new ArrayList<>();

        // TODO: 2024/8/26 Optimization idea: sort the span list and put the earliest spans first. Then, when looking for call relationships, later spans
        // may find the caller span directly and avoid MQ.

        for (String spanId : spanIdList) {
            String spanStr = null;
            String callerSpanStr = null;

            /**
             * 1, check span
             */
            // todo: Can bytes from MQ be converted directly to objects? This would remove one conversion layer.
            // if span does not exist in cache, maybe something wrong. drop it.
            spanStr = cacheService.get(spanId);
            log.debug("spanStr:" + spanStr);
            if (StringUtils.isEmpty(spanStr)) {
                log.warn("spanStr is empty. spanStr:" + spanStr + ", spanInfo spanId:" + spanId);
                continue;
            }

            // if span has timed out, drop it.
            SpanInfo spanInfo = jsonService.fromJson(spanStr, SpanInfo.class);
            callerSpanStr = cacheService.get(spanInfo.getParentSpanId());
            log.debug("callerSpanStr:" + callerSpanStr);
            SpanInfo caller = null;
            if (!StringUtils.isEmpty(callerSpanStr)) {
                caller = jsonService.fromJson(
                        callerSpanStr, SpanInfo.class);
            }
            spanSaveProcessor.process(caller, spanInfo, sysCallEntites, delayedProcessingSpanIds);
        }

        // check data
        if (sysCallEntites.isEmpty()) {
            log.trace("delay processing span size:" + delayedProcessingSpanIds.size());
            return delayedProcessingSpanIds;
        }

        // todo: To avoid data loss when the database has issues or during deployments.
        // Should this be changed to write to MQ first and then consume from MQ, or let the server consume again?
        // Think through the flow from the beginning (Kafka) and decide how to handle failures in intermediate environments.
        // save data

        // save resource data
        // Resolve resource name and convert the string to a UUID hash
        List<ResourceHashDictionaryEntity> newResourceHashes = new ArrayList<>();
        try {
            newResourceHashes = resourceHashResolver.resolveHashes(sysCallEntites);
            // Batch save new hash mappings to the database and update the cache
            if (!newResourceHashes.isEmpty()) {
                resourceHashDictionaryMapper.batchUpsert(newResourceHashes);
                // Update cache
                for (ResourceHashDictionaryEntity entity : newResourceHashes) {
                    cacheService.setResourceHash(entity.getTextValue(), entity.getHashKey());
                }
            }
        } catch (Exception e) {
            // Even if database insertion fails, continue inserting later spans because the corresponding actual names can be manually backfilled.
            log.error("Failed to batch upsert resource hash dictionary. newResourceHashes: {}",
                    newResourceHashes, e);
        }

        // ==========================================================================================
        // End of Phase 4
        // ==========================================================================================

        // save span data
        try {

            long startTime = System.currentTimeMillis(); // Record start time
            int upsertResult = 0;
            if (isReconsumption) {
//                upsertResult = svcCallMapper.batchInsert(sysCallEntites);
                upsertResult = svcCallMapper.batchUpsert(sysCallEntites);
                log.trace("reconsumption save span size:" + upsertResult);
            } else {
//                upsertResult = svcCallMapper.batchInsert(sysCallEntites);
                upsertResult = svcCallMapper.batchUpsert(sysCallEntites);
                // svcCallMapper.batchInsert(sysCallEntites);
                log.trace("save span size:" + upsertResult);

            }
            // Record end time
            long endTime = System.currentTimeMillis();
            log.trace("SQL execution time: {} ms. data size:{}", (endTime - startTime), sysCallEntites.size());
            // add to stats
            statsService.countSpanSaved(sysCallEntites.size());

        } catch (Exception e) {
            log.error("insert exception:", e);
            for (ServiceCallEntity sysCallEntity : sysCallEntites) {
                log.error("sysCallEntity:" + sysCallEntity);
            }
        }

        log.trace("delay processing span size:" + delayedProcessingSpanIds.size());
        return delayedProcessingSpanIds;

    }

}