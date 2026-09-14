package org.prism.apm.ui.server.interceptor;

import org.prism.apm.ui.server.entity.param.common.DateParam;
import org.prism.apm.ui.server.exceptions.BizException;
import org.prism.apm.ui.server.utils.DateUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@Slf4j
public class DateValidateInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String startDate = null;
        String endDate = null;
        if ("GET".equalsIgnoreCase(request.getMethod())) {
            startDate = request.getParameter(DateParam.FIELD_NAME_START_DATE);
            endDate = request.getParameter(DateParam.FIELD_NAME_END_DATE);
            String timezone = request.getParameter(DateParam.FIELD_NAME_TIMEZONE);
            if (!StringUtils.hasText(timezone)) {
                throw new BizException("Please select a time zone");
            }
        }
        if (startDate == null && endDate == null) {
            return true;
        }
        if (startDate == null || endDate == null) {
            throw new BizException("Please select a complete time range");
        }

        long diff_days = DateUtils.getTimeBetweenInDays(startDate, endDate);
        if (diff_days > 360) {
            throw new BizException("The time range cannot exceed 360 days");
        }

        long timeBetweenInMinutes = DateUtils.getTimeBetweenInMinutes(startDate, endDate);
        if (timeBetweenInMinutes <= 0) {
            throw new BizException("The time range cannot be less than 1 minute");
        }

        return true;
    }
}
