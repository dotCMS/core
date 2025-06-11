import { Injectable } from '@angular/core';
import { createDotCMSClient, DotCMSClientConfig, DotCMSClient } from '@dotcms/client/next';

@Injectable({
  providedIn: 'root'
})
export class DotCMSClientService {
  public client: DotCMSClient;

  constructor(config: DotCMSClientConfig) {
    this.client = createDotCMSClient(config);
  }
}
