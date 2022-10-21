/* eslint-disable @typescript-eslint/no-explicit-any */

import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { DotContainerCreateEditResolver } from './dot-container-create.resolver';
import { DotRouterService } from '@services/dot-router/dot-router.service';
import { MockDotRouterService } from '@tests/dot-router-service.mock';
import { DotContainersService } from '@dotcms/app/api/services/dot-containers/dot-containers.service';

describe('DotContainerService', () => {
    let service: DotContainerCreateEditResolver;
    let containersService: DotContainersService;

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [
                DotContainerCreateEditResolver,
                { provide: DotRouterService, useClass: MockDotRouterService },
                {
                    provide: DotContainersService,
                    useValue: {
                        getById: jasmine.createSpy().and.returnValue(
                            of({
                                this: {
                                    is: 'a page'
                                }
                            })
                        ),
                        getFiltered: () => {
                            //
                        }
                    }
                }
            ]
        });
        service = TestBed.inject(DotContainerCreateEditResolver);
        containersService = TestBed.inject(DotContainersService);
    });

    it('should return page by id from router', (done) => {
        service
            .resolve(
                {
                    paramMap: {
                        get(param) {
                            return param === 'inode' ? null : 'ID';
                        }
                    }
                } as any,
                null
            )
            .subscribe((res) => {
                expect(containersService.getById).toHaveBeenCalledWith('ID');
                expect<any>(res).toEqual({ this: { is: 'a page' } });
                done();
            });
    });
});
