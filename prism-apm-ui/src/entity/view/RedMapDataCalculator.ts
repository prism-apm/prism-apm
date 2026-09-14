export class RedMapDataCalculator {
    durations: number[];
    counts: number[];
    errs: number[];

    constructor() {
        this.durations = [];
        this.counts = [];
        this.errs = [];
    }

    /**
     * Load data entity list
     * @param {any[]} dataList - Data list after Entity encapsulation
     */
    load(dataList: any[]) {
        this.clear();

        if (!dataList || dataList.length === 0) {
            return;
        }

        for (const item of dataList) {
            const metric = item?.redMetric;

            this.durations.push(metric?.avgDuration || 0);
            this.counts.push(metric?.totalCount || 0);
            this.errs.push(metric?.totalError || 0);
        }
    }

    /**
     * Clear all data
     */
    clear() {
        this.durations.splice(0, this.durations.length);
        this.counts.splice(0, this.counts.length);
        this.errs.splice(0, this.errs.length);
    }
}
