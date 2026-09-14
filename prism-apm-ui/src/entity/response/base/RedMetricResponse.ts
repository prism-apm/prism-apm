/**
 * RED metric response entity
 */
export class RedMetricResponse {
    avgDuration: number;
    totalCount: number;
    totalError: number;

    /**
     * @param {any} [data] - Original data object
     */
    constructor(data?: any) {
        /**
         * Average duration
         */
        this.avgDuration = data?.avgDuration || 0.0;

        /**
         * Total count
         */
        this.totalCount = data?.totalCount || 0;

        /**
         * Total error count
         */
        this.totalError = data?.totalError || 0;
    }
}
