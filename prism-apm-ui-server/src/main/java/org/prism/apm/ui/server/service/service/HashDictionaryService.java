package org.prism.apm.ui.server.service.service;

import org.prism.apm.ui.server.constant.SpanConstants;
import org.prism.apm.ui.server.entity.db.ResourceHashDictionary;
import org.prism.apm.ui.server.entity.model.service.ServiceCallView;
import org.prism.apm.ui.server.mapper.service.ResourceHashDictionaryMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * HashKey dictionary conversion service
 * Provides mapping queries from HashKey to original text
 */
@Service
public class HashDictionaryService {

    private final ResourceHashDictionaryMapper resourceHashDictionaryMapper;

    private static final String CATEGORY_TARGET_NAME = "target_name";

    public HashDictionaryService(ResourceHashDictionaryMapper resourceHashDictionaryMapper) {
        this.resourceHashDictionaryMapper = resourceHashDictionaryMapper;
    }

    /**
     * Build the HashKey -> original text mapping
     *
     * @param hashKeys HashKey list
     * @return Map<HashKey, TextValue>
     */
    public Map<String, String> buildHashToTextMap(List<String> hashKeys) {
        if (CollectionUtils.isEmpty(hashKeys)) {
            return Collections.emptyMap();
        }
        // Deduplicate
        List<String> uniqueHashKeys = hashKeys.stream()
                .filter(StringUtils::hasText)
                .distinct()
                .collect(Collectors.toList());

        if (CollectionUtils.isEmpty(uniqueHashKeys)) {
            return Collections.emptyMap();
        }

        List<ResourceHashDictionary> dictionaries = resourceHashDictionaryMapper.batchQuery(uniqueHashKeys, CATEGORY_TARGET_NAME);
        Map<String, String> result = dictionaries.stream()
                .collect(Collectors.toMap(
                        ResourceHashDictionary::getHashKey,
                        ResourceHashDictionary::getTextValue,
                        (existing, replacement) -> existing
                ));
        // Handle the default UUID value case
        result.put(SpanConstants.DEFAULT_UUID_VALUE_WHEN_NO_DATA, SpanConstants.DEFAULT_STRING_VALUE_WHEN_NO_DATA);
        return result;
    }

    /**
     * Extract targetName and callerTargetName from the ServiceCallView list and build the HashKey -> original text mapping
     *
     * @param dataList ServiceCallView list
     * @return Map<HashKey, TextValue>
     */
    public Map<String, String> buildHashToTextMapFromServiceCallView(List<ServiceCallView> dataList) {
        if (CollectionUtils.isEmpty(dataList)) {
            return Collections.emptyMap();
        }

        List<String> allHashKeys = new ArrayList<>();
        for (ServiceCallView d : dataList) {
            allHashKeys.add(d.getTargetName());
            allHashKeys.add(d.getCallerTargetName());
        }

        return buildHashToTextMap(allHashKeys);
    }

    /**
     * Extract only targetName from the ServiceCallView list and build the HashKey -> original text mapping
     *
     * @param dataList ServiceCallView list
     * @return Map<HashKey, TextValue>
     */
    public Map<String, String> buildHashToTextMapFromTargetName(List<ServiceCallView> dataList) {
        if (CollectionUtils.isEmpty(dataList)) {
            return Collections.emptyMap();
        }

        List<String> allHashKeys = dataList.stream()
                .map(ServiceCallView::getTargetName)
                .filter(StringUtils::hasText)
                .distinct()
                .collect(Collectors.toList());

        return buildHashToTextMap(allHashKeys);
    }

    /**
     * Extract only callerTargetName from the ServiceCallView list and build the HashKey -> original text mapping
     *
     * @param dataList ServiceCallView list
     * @return Map<HashKey, TextValue>
     */
    public Map<String, String> buildHashToTextMapFromCallerTargetName(List<ServiceCallView> dataList) {
        if (CollectionUtils.isEmpty(dataList)) {
            return Collections.emptyMap();
        }

        List<String> allHashKeys = dataList.stream()
                .map(ServiceCallView::getCallerTargetName)
                .filter(StringUtils::hasText)
                .distinct()
                .collect(Collectors.toList());

        return buildHashToTextMap(allHashKeys);
    }
}
