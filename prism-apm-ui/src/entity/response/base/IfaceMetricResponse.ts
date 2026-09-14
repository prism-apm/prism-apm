export class IfaceMetricResponse {
    maxDuration: number;
    minDuration: number;
    p90: number;
    p95: number;
    p99: number;

    /**
     * IfaceMetricResponse instance
     */
    constructor({ maxDuration = 0.0, minDuration = 0.0, p90 = 0.0, p95 = 0.0, p99 = 0.0 }: { maxDuration?: number, minDuration?: number, p90?: number, p95?: number, p99?: number } = {}) {
        /**
         * Maximum duration
         */
        this.maxDuration = maxDuration;

        /**
         * Minimum duration
         */
        this.minDuration = minDuration;

        /**
         * P90 duration
         */
        this.p90 = p90;

        /**
         * P95 duration
         */
        this.p95 = p95;

        /**
         * P99 duration
         */
        this.p99 = p99;
    }
}
