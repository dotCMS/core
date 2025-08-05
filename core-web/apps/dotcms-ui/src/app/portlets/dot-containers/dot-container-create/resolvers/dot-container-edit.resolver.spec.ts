/* eslint-disable @typescript-eslint/no-explicit-any */

import { of } from 'rxjs';

import { TestBed } from '@angular/core/testing';

import { DotRouterService } from '@dotcms/data-access';
import { MockDotRouterService } from '@dotcms/utils-testing';

import { DotContainerEditResolver } from './dot-container-edit.resolver';

import { DotContainersService } from '../../../../api/services/dot-containers/dot-containers.service';

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
