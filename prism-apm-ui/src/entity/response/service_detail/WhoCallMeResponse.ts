import { RedMetricResponse } from '../base/RedMetricResponse';

export class WhoCallMeResponse {
    callerServiceName: string;
    ifaceCaller: string;
    totalTime: number;
    redMetric: RedMetricResponse;

    /**
     * @param {any} [data] - Original data object
     */
    constructor(data?: any) {
        /**
         * Caller service name
         */
        this.callerServiceName = data?.callerServiceName || 'UNKNOWN';

        /**
         * Caller interface
         */
        this.ifaceCaller = data?.ifaceCaller || 'UNKNOWN';

        /**
         * Total time of the interface (seconds)
         */
        this.totalTime = data?.totalTime || 0.0;

        /**
         * RED metric object
         */
        this.redMetric = new RedMetricResponse(data?.redMetric);
    }
}
