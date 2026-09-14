package org.prism.apm.common.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author john
 * @date 2025-09-09 18:30
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FieldContentReplacerRuleEntity {
    private Integer id;

    private String serviceName;

    private String component;

    private Short spanKind;

    private String fieldName;

    private String matchType;

    private String searchContent;

    private String replacement;

    private Boolean enabled;
}