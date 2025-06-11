import { TestBed } from '@angular/core/testing';
import { inject } from '@angular/core';
import { provideDotCMSClient } from './dotcms-client.provider';
import { DotCMSClientService } from '../../services/dotcms-client.service';
import { DotCMSClientConfig } from '@dotcms/client/next';

describe('provideDotCMSClient', () => {
  const testConfig: DotCMSClientConfig = {
    dotcmsUrl: 'https://demo.dotcms.com',
    siteId: 'default'
  };

  it('should provide DotCMSClientService with the given config', () => {
    TestBed.configureTestingModule({
      providers: [provideDotCMSClient(testConfig)]
    });

    const clientService = TestBed.inject(DotCMSClientService);
    expect(clientService).toBeTruthy();
    expect(clientService.client).toBeTruthy();
    expect(clientService.client.config.dotcmsUrl).toBe('https://demo.dotcms.com');
    expect(clientService.client.config.siteId).toBe('default');
  });

  it('should allow injecting DotCMSClientService using inject()', () => {
    TestBed.configureTestingModule({
      providers: [provideDotCMSClient(testConfig)]
    });

    const clientService = inject(DotCMSClientService);
    expect(clientService).toBeTruthy();
    expect(clientService.client).toBeTruthy();
    expect(clientService.client.config.dotcmsUrl).toBe('https://demo.dotcms.com');
  });
});
