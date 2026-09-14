package org.prism.apm.ui.server.entity.model.topology;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import org.prism.apm.ui.server.config.JsonTypeHandlerPg;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

@Data // Automatically generate getter, setter, toString, equals, and hashCode
@NoArgsConstructor // No-args constructor
@AllArgsConstructor // All-args constructor
@TableName(value = "topology_warning_rules") // Specify table name
public class TopologyWarningRule implements Serializable {

    @TableId(type = IdType.AUTO) // Primary key, auto-increment
    private Integer id;

    private String ruleName;   // Rule name

    @TableField(typeHandler = JsonTypeHandlerPg.class)
    private String ruleDetails; // Rule details (JSON as a string)
    private Long userId;    // User ID
    private Integer enabled;
    private String serviceName;

    private Date updatedAt;

    private Integer warningLevel;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class RuleDetails {

        private String indicatorName;  // Metric name
        private String operator;       // Operator, such as >, <, =
        private String value;          // Value
        private String logicalOperator; // Logical operator, such as AND, OR

    }
}
