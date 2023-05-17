/* eslint-disable @typescript-eslint/no-explicit-any */

import { of } from 'rxjs';

import { TestBed } from '@angular/core/testing';

import { DotCacheService } from '@services/dot-containers/dot-cache.service';
import { MockDotRouterService } from '@dotcms/utils-testing';
import { DotRouterService } from '@services/dot-router/dot-router.service';

import { DotContainerEditResolver } from './dot-container-edit.resolver';

describe('DotContainerService', () => {
    let service: DotContainerEditResolver;
    let containersService: DotCacheService;

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [
                DotContainerEditResolver,
                { provide: DotRouterService, useClass: MockDotRouterService },
                {
                    provide: DotCacheService,
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
        service = TestBed.inject(DotContainerEditResolver);
        containersService = TestBed.inject(DotCacheService);
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
                expect(containersService.getById).toHaveBeenCalledWith('ID', 'working', true);
                expect<any>(res).toEqual({ this: { is: 'a page' } });
                done();
            });
    });
});
