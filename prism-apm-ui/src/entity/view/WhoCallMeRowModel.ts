import i18n from '@/locale';
import { WhoCallMeResponse } from '../response/service_detail/WhoCallMeResponse';

export default class WhoCallMeRowModel {
    _origin: WhoCallMeResponse;
    callerServiceName: string;
    ifaceCaller: string;
    totalTime: number;
    avgDuration: number;
    totalCount: number;

    /**
     * @param {WhoCallMeResponse} dto - Original response entity
     */
    constructor(dto: WhoCallMeResponse) {
        this._origin = dto;

        this.callerServiceName = dto.callerServiceName;
        this.ifaceCaller = dto.ifaceCaller;
        this.totalTime = dto.totalTime;
        this.avgDuration = dto.redMetric?.avgDuration || 0;
        this.totalCount = dto.redMetric?.totalCount || 0;
    }

    static getByServiceColumn(): any[] {
        return [
            {
                'title': (i18n as any).t('table.callerService'),
                'width': 150,
                'align': 'center',
                'key': "callerServiceName"
            },
            {
                'title': (i18n as any).t('table.totalDurationSeconds'),
                'align': 'center',
                'key': 'totalTime',
                'sortable': true,
            },
            {
                'title': (i18n as any).t('table.totalCallCount'),
                'align': 'center',
                'key': 'totalCount',
                'sortable': true,
            },
            {
                'title': (i18n as any).t('table.avgDurationMs'),
                'align': 'center',
                'key': 'avgDuration',
                'sortable': true,
            }
        ];
    }

    static getByIfaceColumn(): any[] {
        return [
            {
                'title': (i18n as any).t('table.callerService'),
                'width': 150,
                'align': 'center',
                'key': "callerServiceName"
            },
            {
                'title': (i18n as any).t('table.callerMethod'),
                'width': 500,
                'align': 'center',
                'key': 'ifaceCaller',
                render: (h: any, params: any) => {
                    return h('Tooltip', {
                        props: {
                            content: params.row.ifaceCaller,
                            transfer: true,
                            maxWidth: 500,
                            theme: 'dark',
                            placement: 'top'
                        },
                        style: {
                            width: '100%',
                            overflow: 'hidden',
                            textOverflow: 'ellipsis',
                            whiteSpace: 'nowrap',
                            display: 'block'
                        }
                    }, params.row.ifaceCaller);
                }
            },
            {
                'title': (i18n as any).t('table.totalDurationSeconds'),
                'align': 'center',
                'key': 'totalTime',
                'sortable': 'custom',
            },
            {
                'title': (i18n as any).t('table.totalCallCount'),
                'align': 'center',
                'key': 'totalCount',
                'sortable': 'custom',
            },
            {
                'title': (i18n as any).t('table.avgDurationMs'),
                'align': 'center',
                'key': 'avgDuration',
                'sortable': 'custom',
            }
        ];
    }
}
