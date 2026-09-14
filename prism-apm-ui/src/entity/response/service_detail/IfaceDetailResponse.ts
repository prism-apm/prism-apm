import { RedMetricResponse } from '../base/RedMetricResponse';

export class IfaceDetailResponse {
    time: string;
    redMetric: RedMetricResponse;

    /**
     * @param {any} [data] - Original data object
     */
    constructor(data?: any) {
        this.time = data?.time || '';
        this.redMetric = new RedMetricResponse(data?.redMetric);
    }
}
