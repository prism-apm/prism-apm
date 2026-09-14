import { IfaceDetailResponse } from './IfaceDetailResponse';

export class IfaceAnalyseMapResponse {
    ifaceName: string;
    ifaceDetail: IfaceDetailResponse[];

    /**
     * @param {any} [data] - Original data object
     */
    constructor(data?: any) {
        /**
         * Interface name
         */
        this.ifaceName = data?.ifaceName || '';

        /**
         * Interface detail list
         */
        this.ifaceDetail = (data?.ifaceDetail || []).map((item: any) => new IfaceDetailResponse(item));
    }
}
