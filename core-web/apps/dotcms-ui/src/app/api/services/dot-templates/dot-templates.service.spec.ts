/* eslint-disable @typescript-eslint/no-explicit-any */

import { of } from 'rxjs';

import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { DotHttpErrorManagerService } from '@dotcms/data-access';
import { CoreWebService } from '@dotcms/dotcms-js';
import { DotActionBulkResult, DotTemplate } from '@dotcms/dotcms-models';
import { CoreWebServiceMock } from '@dotcms/utils-testing';

import { DotTemplatesService, TEMPLATE_API_URL } from './dot-templates.service';

const mockBulkResponseSuccess: DotActionBulkResult = {
    skippedCount: 0,
    successCount: 1,
    fails: []
};

const mockTemplate: DotTemplate = {
    anonymous: false,
    friendlyName: 'Copy template',
    identifier: '123',
    inode: '1AreSD',
    name: 'Copy template',
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

describe('DotTemplatesService', () => {
    let service: DotTemplatesService;
    let httpMock: HttpTestingController;

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [
                DotTemplatesService,
                {
                    provide: DotHttpErrorManagerService,
                    useValue: {
                        handle() {
                            return of({});
                        }
                    }
                },
                {
                    provide: CoreWebService,
                    useClass: CoreWebServiceMock
                }
            ],
            imports: [HttpClientTestingModule]
        });
        service = TestBed.inject(DotTemplatesService);

        httpMock = TestBed.inject(HttpTestingController);
    });

    it('should get a list of templates', () => {
        service.get().subscribe((template) => {
            expect(template as any).toEqual([
                {
                    identifier: '1234',
                    name: 'Theme name'
                }
            ]);
        });

        const req = httpMock.expectOne(TEMPLATE_API_URL);

        expect(req.request.method).toBe('GET');

        req.flush({
            entity: [
                {
                    identifier: '1234',
                    name: 'Theme name'
                }
            ]
        });
    });

    it('should get a template by id', () => {
        service.getById('123').subscribe((template) => {
            expect(template as any).toEqual({
                identifier: '1234',
                name: 'Theme name'
            });
        });

        const req = httpMock.expectOne(`${TEMPLATE_API_URL}123/working`);

        expect(req.request.method).toBe('GET');

        req.flush({
            entity: {
                identifier: '1234',
                name: 'Theme name'
            }
        });
    });

    it('should get a templates by filter', () => {
        service.getFiltered({ filter: '123' }).subscribe((response) => {
            expect(response.templates as any).toEqual([
                {
                    identifier: '123',
                    name: 'Theme name'
                }
            ]);
            expect(response.totalRecords).toBe(1);
        });

        const req = httpMock.expectOne((request) => {
            return request.url === TEMPLATE_API_URL && request.params.get('filter') === '123';
        });

        expect(req.request.method).toBe('GET');

        req.flush({
            entity: [
                {
                    identifier: '123',
                    name: 'Theme name'
                }
            ],
            pagination: {
                totalEntries: 1
            }
        });
    });

    it('should post to create a template', () => {
        service
            .create({
                name: '',
                anonymous: true,
                friendlyName: ''
            } as DotTemplate)
            .subscribe((template) => {
                expect(template as any).toEqual({
                    identifier: '1234',
                    name: 'Theme name'
                });
            });

        const req = httpMock.expectOne(TEMPLATE_API_URL);

        expect(req.request.method).toBe('POST');
        expect(req.request.body).toEqual({
            name: '',
            anonymous: true,
            friendlyName: ''
        });

        req.flush({
            entity: {
                identifier: '1234',
                name: 'Theme name'
            }
        });
    });

    it('should put to update a template', () => {
        service
            .update({
                name: '',
                anonymous: true,
                friendlyName: ''
            } as DotTemplate)
            .subscribe((template) => {
                expect(template as any).toEqual({
                    identifier: '1234',
                    name: 'Theme name'
                });
            });

        const req = httpMock.expectOne(TEMPLATE_API_URL);

        expect(req.request.method).toBe('PUT');
        expect(req.request.body).toEqual({
            name: '',
            anonymous: true,
            friendlyName: ''
        });

        req.flush({
            entity: {
                identifier: '1234',
                name: 'Theme name'
            }
        });
    });

    it('should put to save and publish a template', () => {
        service
            .saveAndPublish({
                name: '',
                anonymous: true,
                friendlyName: ''
            } as DotTemplate)
            .subscribe((template) => {
                expect(template as any).toEqual({
                    identifier: '1234',
                    name: 'Theme name'
                });
            });

        const req = httpMock.expectOne(`${TEMPLATE_API_URL}_savepublish`);

        expect(req.request.method).toBe('PUT');
        expect(req.request.body).toEqual({
            name: '',
            anonymous: true,
            friendlyName: ''
        });

        req.flush({
            entity: {
                identifier: '1234',
                name: 'Theme name'
            }
        });
    });

    it('should delete a template', () => {
        service.delete(['testId01']).subscribe();
        const req = httpMock.expectOne(TEMPLATE_API_URL);

        expect(req.request.method).toBe('DELETE');
        expect(req.request.body).toEqual(['testId01']);
        req.flush(mockBulkResponseSuccess);
    });
    it('should unArchive a template', () => {
        service.unArchive(['testId01']).subscribe();
        const req = httpMock.expectOne(`${TEMPLATE_API_URL}_unarchive`);

        expect(req.request.method).toBe('PUT');
        expect(req.request.body).toEqual(['testId01']);
        req.flush(mockBulkResponseSuccess);
    });
    it('should archive a template', () => {
        service.archive(['testId01']).subscribe();
        const req = httpMock.expectOne(`${TEMPLATE_API_URL}_archive`);

        expect(req.request.method).toBe('PUT');
        expect(req.request.body).toEqual(['testId01']);
        req.flush(mockBulkResponseSuccess);
    });
    it('should unPublish a template', () => {
        service.unPublish(['testId01']).subscribe();
        const req = httpMock.expectOne(`${TEMPLATE_API_URL}_unpublish`);

        expect(req.request.method).toBe('PUT');
        expect(req.request.body).toEqual(['testId01']);
        req.flush(mockBulkResponseSuccess);
    });
    it('should publish a template', () => {
        service.publish(['testId01']).subscribe();
        const req = httpMock.expectOne(`${TEMPLATE_API_URL}_publish`);

        expect(req.request.method).toBe('PUT');
        expect(req.request.body).toEqual(['testId01']);
        req.flush(mockBulkResponseSuccess);
    });
    it('should copy a template', () => {
        service.copy('testId01').subscribe();
        const req = httpMock.expectOne(`${TEMPLATE_API_URL}testId01/_copy`);

        expect(req.request.method).toBe('PUT');
        req.flush(mockTemplate);
    });
});
