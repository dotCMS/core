import { TestBed } from '@angular/core/testing';
import { DotCMSClientService } from './dotcms-client.service';
import { DotCMSClientConfig } from '@dotcms/client/next';

describe('DotCMSClientService', () => {
  const mockConfig: DotCMSClientConfig = {
    dotcmsUrl: 'http://localhost:8080',
    siteId: 'test-site'
  };

  it('should be created with a valid config', () => {
    const service = new DotCMSClientService(mockConfig);
    expect(service).toBeTruthy();
    expect(service.client).toBeTruthy();
  });

  it('should expose the dotcms client instance', () => {
    const service = new DotCMSClientService(mockConfig);
    expect(service.client.config.dotcmsUrl).toBe('http://localhost:8080');
    expect(service.client.config.siteId).toBe('test-site');
  });
});
