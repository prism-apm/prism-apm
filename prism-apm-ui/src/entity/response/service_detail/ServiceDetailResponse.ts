import { RedMetricResponse } from '../base/RedMetricResponse';

export class ServiceDetailInfoResponse {
  public time: string;
  public redMetric: RedMetricResponse | null;

  /**
   * @param data - Original data object
   */
  constructor(data?: any) {
    this.time = data?.time || '';
    this.redMetric = data?.redMetric ? new RedMetricResponse(data.redMetric) : null;
  }
}

export default ServiceDetailInfoResponse;