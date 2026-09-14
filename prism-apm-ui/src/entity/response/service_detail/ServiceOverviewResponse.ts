import { RedMetricResponse } from '../base/RedMetricResponse';

class ServiceOverviewResponse {
    serviceName: string;
    redMetric: RedMetricResponse | null;
    rpm: number;
    errRate: number;

    /**
     * @param {Object} params - Initialization parameters object
     */
    constructor({ serviceName = '', redMetric = new RedMetricResponse(), rpm = 0.0, errRate = 0.0 }: { serviceName?: string, redMetric?: RedMetricResponse | any, rpm?: number, errRate?: number } = {}) {
        /**
         * Service name
         */
        this.serviceName = serviceName;

        /**
         * RED metric details
         */
        this.redMetric = redMetric instanceof RedMetricResponse
            ? redMetric
            : (redMetric ? new RedMetricResponse(redMetric) : null);

        // --- calculated fields ---

        /**
         * Requests per minute
         */
        this.rpm = rpm;

        /**
         * Error rate
         */
        this.errRate = errRate;
    }
}

export default ServiceOverviewResponse;
