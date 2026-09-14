import { formatTinyNumber } from '@/utils/number-utils';
import TransactionInfoResponse from '../response/service_detail/TransactionInfoResponse';
import { RedMetricResponse } from '../response/base/RedMetricResponse';

/**
 * ViewModel wrapping a TransactionInfoResponse for sorted display.
 * All fields from the original response are promoted as direct properties,
 * along with computed sorting/percentage fields.
 */
export class InterfaceSortedItem {
    ifaceName: string;
    ifaceCaller: string;
    ifaceNameHashKey: string;
    ifaceCallerHashKey: string;
    uniqueId: string;
    method: string;
    redMetric: RedMetricResponse | null;
    sortByPercent: number;
    percentFormatted: string;

    /**
     * @param {TransactionInfoResponse} data
     */
    constructor(data: TransactionInfoResponse) {
        // -- Fields from TransactionInfoResponse --

        this.ifaceName = data.ifaceName;
        this.ifaceCaller = data.ifaceCaller;
        this.ifaceNameHashKey = data.ifaceNameHashKey;
        this.ifaceCallerHashKey = data.ifaceCallerHashKey;
        this.uniqueId = data.uniqueId;
        this.method = data.method;
        this.redMetric = data.redMetric;

        // -- Computed sorting fields --

        /** Percentage value (0-100) used for progress bar width */
        this.sortByPercent = 0;

        /** Formatted percentage string for display */
        this.percentFormatted = '0.00';
    }
}

/**
 * Encapsulates the sorting and percentage calculation logic
 * for interface lists based on RED metric values.
 *
 * Used by transaction.vue and db.vue to sort interfaces by
 * totalCount, avgDuration, or totalError, and compute each
 * item's share as a percentage of the total.
 */
export default class InterfaceSortedList {

    /**
     * Sort the data list by a given RED metric key and compute percentages.
     *
     * @param dataList - Source data
     * @param {string} sortKey - RED metric key to sort by (e.g. 'totalCount', 'avgDuration', 'totalError')
     * @returns {InterfaceSortedItem[]} Sorted list of wrapped items
     */
    sort(dataList: TransactionInfoResponse[], sortKey: keyof RedMetricResponse): InterfaceSortedItem[] {
        if (!dataList || dataList.length === 0) {
            return [];
        }

        // 1. Shallow-copy and sort descending by the metric value
        const sorted = [...dataList].sort((a, b) => {
            const valA = a.redMetric ? parseFloat(a.redMetric[sortKey] as any) : 0;
            const valB = b.redMetric ? parseFloat(b.redMetric[sortKey] as any) : 0;
            return valB - valA;
        });

        // 2. Compute the total for percentage calculation
        const total = sorted.reduce((sum, item) => {
            const val = item.redMetric ? parseFloat(item.redMetric[sortKey] as any) : 0;
            return sum + val;
        }, 0);

        // 3. Wrap each item into a ViewModel and compute its percentage
        return sorted.map(original => {
            const val = original.redMetric ? parseFloat(original.redMetric[sortKey] as any) : 0;
            const percent = total === 0 ? 0 : (val * 100) / total;

            const item = new InterfaceSortedItem(original);
            item.sortByPercent = percent;
            item.percentFormatted = formatTinyNumber(2, false, percent);

            return item;
        });
    }
}
