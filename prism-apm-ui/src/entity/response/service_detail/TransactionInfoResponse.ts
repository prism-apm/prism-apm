import { IfaceMetricResponse } from '../base/IfaceMetricResponse';
import { RedMetricResponse } from '../base/RedMetricResponse';

export default class TransactionInfoResponse {
    ifaceName: string;
    ifaceCaller: string;
    ifaceNameHashKey: string;
    ifaceCallerHashKey: string;
    redMetric: RedMetricResponse | null;
    ifaceMetric: IfaceMetricResponse | null;
    uniqueId: string;
    method: string;

    /**
     * @param {any} [data] - Original data object
     */
    constructor(data?: any) {
        /**
         * Interface name
         */
        this.ifaceName = data?.ifaceName || '-';

        /**
         * Interface caller
         */
        this.ifaceCaller = data?.ifaceCaller || '-';

        /**
         * Interface hashKey
         */
        this.ifaceNameHashKey = (data && data.ifaceNameHashKey) ? data.ifaceNameHashKey : '';

        /**
         * Interface caller hashKey
         */
        this.ifaceCallerHashKey = (data && data.ifaceCallerHashKey) ? data.ifaceCallerHashKey : '';

        /**
         * Red metric object
         */
        this.redMetric = data?.redMetric ? new RedMetricResponse(data.redMetric) : null;

        /**
         * Interface metric object
         */
        this.ifaceMetric = data?.ifaceMetric ? new IfaceMetricResponse(data.ifaceMetric) : null;

        /**
         * uniqueId
         */
        this.uniqueId = data?.uniqueId || '';

        /**
         * Method name
         */
        this.method = data?.method || '';
    }
}
