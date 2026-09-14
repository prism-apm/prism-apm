import { RedMetricResponse } from '../base/RedMetricResponse';

export class IfaceGatherInfoResponse {
    ifaceName: string;
    systemType: string;
    totalTime: number;
    percent: number;
    redMetric: RedMetricResponse;

    /**
     * @param {any} [data] - Original data object
     */
    constructor(data?: any) {
        /**
         * Interface name
         */
        this.ifaceName = data?.ifaceName || '';

        /**
         * System type
         */
        this.systemType = data?.systemType || '';

        this.totalTime = data?.totalTime || 0.0;
        this.percent = data?.percent || 0.0;
        this.redMetric = new RedMetricResponse(data?.redMetric);
    }
}
