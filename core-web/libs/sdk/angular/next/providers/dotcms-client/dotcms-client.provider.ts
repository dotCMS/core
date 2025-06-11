import { makeEnvironmentProviders, EnvironmentProviders } from '@angular/core';
import { DotCMSClientConfig }
from '@dotcms/client/next';
import { DotCMSClientService } from '../../services/dotcms-client.service';

export function provideDotCMSClient(config: DotCMSClientConfig): EnvironmentProviders {
  return makeEnvironmentProviders([
    {
      provide: DotCMSClientService,
      useValue: new DotCMSClientService(config)
    }
  ]);
}
