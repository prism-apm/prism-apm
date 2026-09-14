import { formatTinyNumber } from '@/utils/number-utils';
import i18n from '@/locale';
import { IfaceGatherInfoResponse } from '../response/service_detail/IfaceGatherInfoResponse';

export default class GatherRowModel {
    _origin: IfaceGatherInfoResponse;
    ifaceName: string;
    systemType: string;
    percent: number;
    totalTime: number;
    totalCount: number;

    /**
     * @param {IfaceGatherInfoResponse} dto - Original response entity
     */
    constructor(dto: IfaceGatherInfoResponse) {
        this._origin = dto;

        this.ifaceName = dto.ifaceName;
        this.systemType = dto.systemType;
        this.percent = dto.percent;
        this.totalTime = dto.totalTime;

        this.totalCount = dto.redMetric?.totalCount || 0;
    }

    static getColumn(): any[] {
        return [
            {
                'title': (i18n as any).t('table.requestCategory'),
                'width': 700,
                'key': 'ifaceName',
                'tooltip': true,
                'align': 'center'
            },
            {
                'title': (i18n as any).t('table.performanceCategory'),
                'align': 'center',
                'key': 'systemType'
            },
            {
                'title': (i18n as any).t('table.timePercent'),
                'align': 'center',
                'key': 'percent',
                render: (h: any, params: any) => {
                    return h('span', formatTinyNumber(2, true, params.row.percent * 100));
                }
            },
            {
                'title': (i18n as any).t('table.totalDurationSeconds'),
                'align': 'center',
                'key': 'totalTime',
                render: (h: any, params: any) => {
                    return h('span', formatTinyNumber(2, false, params.row.totalTime));
                }
            },
            {
                'title': (i18n as any).t('table.callCount'),
                'align': 'center',
                'key': 'totalCount',
                render: (h: any, params: any) => {
                    if (params.row.totalCount === -1) {
                        return h('span', '-');
                    }
                    return h('span', params.row.totalCount);
                }
            },
        ];
    }
}
