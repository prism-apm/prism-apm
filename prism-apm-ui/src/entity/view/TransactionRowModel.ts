import { formatTinyNumber } from "@/utils/number-utils";
import { orderBy } from "@/utils/order-utils";
import TransactionInfoResponse from '../response/service_detail/TransactionInfoResponse';

export default class TransactionRowModel {
    _origin: TransactionInfoResponse;
    ifaceName: string;
    ifaceCaller: string;
    method: string;
    totalCount: number;
    totalError: number;
    avgDuration: number;
    maxDuration: number;
    minDuration: number;
    p90: number;
    p95: number;
    p99: number;

    /**
     * @param {TransactionInfoResponse} dto - Original response entity
     */
    constructor(dto: TransactionInfoResponse) {
        this._origin = dto;

        this.ifaceName = dto.ifaceName;
        this.ifaceCaller = dto.ifaceCaller;
        this.method = dto.method;

        this.totalCount = dto.redMetric?.totalCount || 0;
        this.totalError = dto.redMetric?.totalError || 0;
        this.avgDuration = dto.redMetric?.avgDuration || 0.0;

        this.maxDuration = dto.ifaceMetric?.maxDuration || 0.0;
        this.minDuration = dto.ifaceMetric?.minDuration || 0.0;
        this.p90 = dto.ifaceMetric?.p90 || 0.0;
        this.p95 = dto.ifaceMetric?.p95 || 0.0;
        this.p99 = dto.ifaceMetric?.p99 || 0.0;
    }

    static getColumn(): any[] {
        return [
            {
                'title': 'Endpoint Name',
                'key': 'ifaceName',
                'width': 400,
                'align': 'left',
                'sortable': true
            },
            {
                'title': 'Total Count',
                'key': 'totalCount',
                'align': 'center',
                'sortType': 'desc',
                'width': 150,
                'sortable': true,
                sortMethod: (a: any, b: any, type: 'asc' | 'desc') => orderBy(a, b, type)
            },
            {
                'title': 'Error Count',
                'key': 'totalError',
                'align': 'center',
                'width': 150,
                'sortable': true,
                sortMethod: (a: any, b: any, type: 'asc' | 'desc') => orderBy(a, b, type)
            },
            {
                'title': 'Avg (ms)',
                'key': 'avgDuration',
                'align': 'center',
                'sortable': true,
                sortMethod: (a: any, b: any, type: 'asc' | 'desc') => orderBy(a, b, type),
                render: (h: any, params: any) => {
                    return h('span', formatTinyNumber(2, false, params.row.avgDuration));
                }
            },
            {
                'title': 'Max (ms)',
                'key': 'maxDuration',
                'align': 'center',
                'sortable': true,
                sortMethod: (a: any, b: any, type: 'asc' | 'desc') => orderBy(a, b, type),
                render: (h: any, params: any) => {
                    return h('span', formatTinyNumber(2, false, params.row.maxDuration));
                }
            },
            {
                'title': 'Min (ms)',
                'key': 'minDuration',
                'align': 'center',
                'sortable': true,
                sortMethod: (a: any, b: any, type: 'asc' | 'desc') => orderBy(a, b, type),
                render: (h: any, params: any) => {
                    return h('span', formatTinyNumber(2, false, params.row.minDuration));
                }
            },
            {
                'title': 'P90 (ms)',
                'key': 'p90',
                'align': 'center',
                'sortable': true,
                sortMethod: (a: any, b: any, type: 'asc' | 'desc') => orderBy(a, b, type),
                render: (h: any, params: any) => {
                    return h('span', formatTinyNumber(2, false, params.row.p90));
                }
            },
            {
                'title': 'P95 (ms)',
                'key': 'p95',
                'align': 'center',
                'sortable': true,
                sortMethod: (a: any, b: any, type: 'asc' | 'desc') => orderBy(a, b, type),
                render: (h: any, params: any) => {
                    return h('span', formatTinyNumber(2, false, params.row.p95));
                }
            },
            {
                'title': 'P99 (ms)',
                'key': 'p99',
                'align': 'center',
                'sortable': true,
                sortMethod: (a: any, b: any, type: 'asc' | 'desc') => orderBy(a, b, type),
                render: (h: any, params: any) => {
                    return h('span', formatTinyNumber(2, false, params.row.p99));
                }
            }
        ];
    }
}
