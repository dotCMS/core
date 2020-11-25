import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';

import { CoreWebService } from 'dotcms-js';

import { DotHttpErrorManagerService } from '@services/dot-http-error-manager/dot-http-error-manager.service';
import { DotTemplatesService } from './dot-templates.service';
import { CoreWebServiceMock } from '@tests/core-web.service.mock';
import { DotTemplate } from '@models/dot-edit-layout-designer';

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
                        handle() {}
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

        const req = httpMock.expectOne('/api/v1/templates');

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

        const req = httpMock.expectOne('/api/v1/templates/123/working');

        expect(req.request.method).toBe('GET');

        req.flush({
            entity: {
                identifier: '1234',
                name: 'Theme name'
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

        const req = httpMock.expectOne('/api/v1/templates');

        expect(req.request.method).toBe('POST');
        expect(req.request.body).toEqual({ name: '', anonymous: true, friendlyName: '' });

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

        const req = httpMock.expectOne('/api/v1/templates');

        expect(req.request.method).toBe('PUT');
        expect(req.request.body).toEqual({ name: '', anonymous: true, friendlyName: '' });

        req.flush({
            entity: {
                identifier: '1234',
                name: 'Theme name'
            }
        });
    });
});
