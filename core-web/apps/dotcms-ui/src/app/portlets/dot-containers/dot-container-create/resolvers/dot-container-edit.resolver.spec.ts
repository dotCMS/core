/* eslint-disable @typescript-eslint/no-explicit-any */

import { of } from 'rxjs';

import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { DotRouterService, DotSystemConfigService } from '@dotcms/data-access';
import { GlobalStore } from '@dotcms/store';
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
                        getById: jest.fn().mockReturnValue(
                            of({
                                container: {
                                    identifier: 'test-id',
                                    title: 'Test Container'
                                },
                                this: {
                                    is: 'a page'
                                }
                            })
                        ),
                        getFiltered: () => {
                            //
                        }
                    }
                },
                {
                    provide: DotSystemConfigService,
                    useValue: { getSystemConfig: () => of({}) }
                },
                {
                    provide: GlobalStore,
                    useValue: { addNewBreadcrumb: jest.fn() }
                },
                provideHttpClient(),
                provideHttpClientTesting()
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
            .subscribe(
                (_res) => {
                    expect(containersService.getById).toHaveBeenCalledWith('ID', 'working', true);
                    expect(containersService.getById).toHaveBeenCalledTimes(1);
                    done();
                },
                (_error) => {
                    done();
                }
            );
    });
});
