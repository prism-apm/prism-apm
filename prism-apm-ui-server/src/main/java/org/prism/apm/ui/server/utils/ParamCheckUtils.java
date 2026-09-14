package org.prism.apm.ui.server.utils;

import org.prism.apm.ui.server.entity.param.common.DateParam;
import org.prism.apm.ui.server.enums.SpanKindEnum;
import org.prism.apm.ui.server.exceptions.BizException;
import org.springframework.util.StringUtils;

import java.util.List;

public class ParamCheckUtils {

    public static void validateDateAndServiceName(DateParam dateParam, String serviceName) {
        validateDate(dateParam);
        validateServiceName(serviceName);
    }

    public static void validateDateAndServiceNameAndIfaceName(DateParam dateParam, String serviceName, String ifaceName) {
        validateDate(dateParam);
        validateIfaceName(ifaceName);
        validateServiceName(serviceName);
    }

    public static void validateSystemType(String systemType){
        if (!StringUtils.hasText(systemType)) {
            throw new BizException("systemType cannot be empty");
        }
    }

    public static void validateSpinKind(Short spinKind){
        if (SpanKindEnum.getEnumByValue(spinKind).equals(SpanKindEnum.UNKNOWN)) {
            throw new BizException("spanKind invalid");
        }
    }

    public static void validateServiceNamePermission(List<String> grantedServiceNames, String queryServiceName) {
        if (!grantedServiceNames.contains(queryServiceName)) {
            throw new BizException("No permission to access this service");
        }
    }

    private static void validateDate(DateParam dateParam) {
        if (!StringUtils.hasText(dateParam.getStartDate())) {
            throw new BizException("startDate cannot be empty");
        }
        if (!StringUtils.hasText(dateParam.getEndDate())) {
            throw new BizException("endDate cannot be empty");
        }
    }

    public static void validateServiceName(String serviceName){
        if (!StringUtils.hasText(serviceName)) {
            throw new BizException("Please select a service to view");
        }
    }

    private static void validateIfaceName(String ifaceName) {
        if (!StringUtils.hasText(ifaceName)) {
            throw new BizException("Please select an endpoint to view");
        }
    }

}
