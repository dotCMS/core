/* eslint-disable @typescript-eslint/no-explicit-any */

import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { DotContainerEditResolver } from './dot-container-edit.resolver';
import { DotRouterService } from '@services/dot-router/dot-router.service';
import { MockDotRouterService } from '@dotcms/utils-testing';
import { DotContainersService } from '@dotcms/app/api/services/dot-containers/dot-containers.service';

describe('DotContainerService', () => {
    let service: DotContainerEditResolver;
    let containersService: DotContainersService;

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [
                DotContainerEditResolver,
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
        service = TestBed.inject(DotContainerEditResolver);
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
                expect(containersService.getById).toHaveBeenCalledWith('ID', 'working', true);
                expect<any>(res).toEqual({ this: { is: 'a page' } });
                done();
            });
    });
});
