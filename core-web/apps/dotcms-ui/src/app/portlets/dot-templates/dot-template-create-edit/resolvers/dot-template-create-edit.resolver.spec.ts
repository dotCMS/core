/* eslint-disable @typescript-eslint/no-explicit-any */

import { of } from 'rxjs';

import { TestBed } from '@angular/core/testing';

import { DotTemplatesService } from '@dotcms/app/api/services/dot-templates/dot-templates.service';
import { DotRouterService } from '@dotcms/data-access';
import { DotTemplate } from '@dotcms/dotcms-models';
import { MockDotRouterService } from '@dotcms/utils-testing';

import { DotTemplateCreateEditResolver } from './dot-template-create-edit.resolver';

const templateMock: DotTemplate = {
    anonymous: false,
    friendlyName: 'Published template',
    identifier: '123Published',
    inode: '1AreSD',
    name: 'Published template',
    type: 'type',
    versionType: 'type',
    deleted: false,
    live: true,
    layout: null,
    canEdit: true,
    canWrite: true,
    canPublish: true,
    hasLiveVersion: true,
    working: true
};

describe('DotTemplateDesignerService', () => {
    let service: DotTemplateCreateEditResolver;
    let templateService: DotTemplatesService;
    let dotRouterService: DotRouterService;

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [
                DotTemplateCreateEditResolver,
                { provide: DotRouterService, useClass: MockDotRouterService },
                {
                    provide: DotTemplatesService,
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
        service = TestBed.inject(DotTemplateCreateEditResolver);
        templateService = TestBed.inject(DotTemplatesService);
        dotRouterService = TestBed.inject(DotRouterService);
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
                expect(templateService.getById).toHaveBeenCalledWith('ID');
                expect<any>(res).toEqual({ this: { is: 'a page' } });
                done();
            });
    });

    it('should return page by inode from router', (done) => {
        spyOn(templateService, 'getFiltered').and.returnValue(of([templateMock]));
        service
            .resolve(
                {
                    paramMap: {
                        get(param) {
                            return param === 'inode' ? 'inode123' : 'ID';
                        }
                    }
                } as any,
                null
            )
            .subscribe((res) => {
                expect(templateService.getFiltered).toHaveBeenCalledWith('inode123');
                expect<any>(res).toEqual(templateMock);
                done();
            });
    });

    it('should go to the main portlet if inode is invalid', (done) => {
        spyOn(templateService, 'getFiltered').and.returnValue(of([]));
        service
            .resolve(
                {
                    paramMap: {
                        get(param) {
                            return param === 'inode' ? 'inode123' : 'ID';
                        }
                    }
                } as any,
                null
            )
            .subscribe(() => {
                expect(templateService.getFiltered).toHaveBeenCalledWith('inode123');
                expect(dotRouterService.gotoPortlet).toHaveBeenCalledWith('templates');
                done();
            });
    });
});
