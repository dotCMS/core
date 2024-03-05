import { of } from 'rxjs';

import { HttpClientTestingModule } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { PushPublishService } from '@dotcms/data-access';

import { DotPushPublishEnvironmentsResolver } from './dot-push-publish-enviroments-resolver.service';

import { MockPushPublishService } from '../dot-content-types-listing/dot-content-types.component.spec';

describe('DotPushPublishEnvironmentsResolver', () => {
    let service: DotPushPublishEnvironmentsResolver;
    let pushPublishService: PushPublishService;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [HttpClientTestingModule],
            providers: [
                DotPushPublishEnvironmentsResolver,
                {
                    provide: PushPublishService,
                    useClass: MockPushPublishService
                }
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
