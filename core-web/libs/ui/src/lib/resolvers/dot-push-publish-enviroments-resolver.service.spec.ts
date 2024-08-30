import { of } from 'rxjs';

import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { PushPublishService } from '@dotcms/data-access';
import { MockPushPublishService } from '@dotcms/utils-testing';

import { DotPushPublishEnvironmentsResolver } from './dot-push-publish-enviroments-resolver.service';

describe('DotPushPublishEnvironmentsResolver', () => {
    let service: DotPushPublishEnvironmentsResolver;
    let pushPublishService: PushPublishService;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [],
            providers: [
                DotPushPublishEnvironmentsResolver,
                {
                    provide: PushPublishService,
                    useClass: MockPushPublishService
                },
                provideHttpClient(withInterceptorsFromDi()),
                provideHttpClientTesting()
            ]
        });

        service = TestBed.inject(DotPushPublishEnvironmentsResolver);
        pushPublishService = TestBed.inject(PushPublishService);
    });

    it('should get a list of environments', () => {
        spyOn(pushPublishService, 'getEnvironments').and.returnValue(of([]));
        service.resolve().subscribe(() => {
            expect(pushPublishService.getEnvironments).toHaveBeenCalled();
        });
    });
});
