import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';

import { CoreWebService } from '@dotcms/dotcms-js';

import { DotHttpErrorManagerService } from '@services/dot-http-error-manager/dot-http-error-manager.service';
import { DotContainersService, CONTAINER_API_URL } from './dot-containers.service';
import { CoreWebServiceMock } from '@tests/core-web.service.mock';
import { DotActionBulkResult } from '@models/dot-action-bulk-result/dot-action-bulk-result.model';
import { of } from 'rxjs';
import { CONTAINER_SOURCE, DotContainer } from '@models/container/dot-container.model';

const mockBulkResponseSuccess: DotActionBulkResult = {
    skippedCount: 0,
    successCount: 1,
    fails: []
};

const mockContainer: DotContainer = {
    archived: false,
    categoryId: '6e07301c-e6d2-4c1f-9e8e-fcc4a31947d3',
    deleted: false,
    friendlyName: '',
    identifier: '1234',
    live: true,
    name: 'movie',
    parentPermissionable: {
        hostname: 'default'
    },
    path: null,
    source: CONTAINER_SOURCE.DB,
    title: 'movie',
    type: 'containers',
    working: true
};

describe('DotContainersService', () => {
    let service: DotContainersService;
    let httpMock: HttpTestingController;

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [
                DotContainersService,
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
        service = TestBed.inject(DotContainersService);

        httpMock = TestBed.inject(HttpTestingController);
    });

    it('should get a list of containers', () => {
        service.get().subscribe((container: DotContainer[]) => {
            expect(container).toEqual([mockContainer]);
        });

        const req = httpMock.expectOne(CONTAINER_API_URL);

        expect(req.request.method).toBe('GET');

        req.flush({
            entity: [mockContainer]
        });
    });

    it('should get a container by id', () => {
        service.getById('123').subscribe((container: DotContainer) => {
            expect(container).toEqual(mockContainer);
        });

        const req = httpMock.expectOne(`${CONTAINER_API_URL}123/working`);

        expect(req.request.method).toBe('GET');

        req.flush({
            entity: mockContainer
        });
    });

    it('should get a containers by filter', () => {
        service.getFiltered('123').subscribe((container: DotContainer[]) => {
            expect(container).toEqual([mockContainer]);
        });

        const req = httpMock.expectOne(`${CONTAINER_API_URL}?filter=123`);

        expect(req.request.method).toBe('GET');

        req.flush({
            entity: [mockContainer]
        });
    });

    it('should post to create a container', () => {
        service
            .create({
                name: '',
                friendlyName: ''
            } as DotContainer)
            .subscribe((container: DotContainer) => {
                expect(container).toEqual(mockContainer);
            });

        const req = httpMock.expectOne(CONTAINER_API_URL);

        expect(req.request.method).toBe('POST');
        expect(req.request.body.name).toEqual('');
        expect(req.request.body.friendlyName).toEqual('');

        req.flush({
            entity: mockContainer
        });
    });

    it('should put to update a container', () => {
        service
            .update({
                name: '',
                friendlyName: ''
            } as DotContainer)
            .subscribe((container) => {
                expect(container).toEqual(mockContainer);
            });

        const req = httpMock.expectOne(CONTAINER_API_URL);

        expect(req.request.method).toBe('PUT');
        expect(req.request.body.name).toEqual('');
        expect(req.request.body.friendlyName).toEqual('');

        req.flush({
            entity: mockContainer
        });
    });
    it('should put to save and publish a container', () => {
        service
            .saveAndPublish({
                name: '',
                friendlyName: ''
            } as DotContainer)
            .subscribe((container: DotContainer) => {
                expect(container).toEqual(mockContainer);
            });

        const req = httpMock.expectOne(`${CONTAINER_API_URL}_savepublish`);

        expect(req.request.method).toBe('PUT');
        expect(req.request.body.name).toEqual('');
        expect(req.request.body.friendlyName).toEqual('');

        req.flush({
            entity: mockContainer
        });
    });
    it('should delete a container', () => {
        service.delete(['testId01']).subscribe();
        const req = httpMock.expectOne(CONTAINER_API_URL);

        expect(req.request.method).toBe('DELETE');
        expect(req.request.body).toEqual(['testId01']);
        req.flush(mockBulkResponseSuccess);
    });
    it('should unArchive a container', () => {
        service.unArchive(['testId01']).subscribe();
        const req = httpMock.expectOne(`${CONTAINER_API_URL}_unarchive`);

        expect(req.request.method).toBe('PUT');
        expect(req.request.body).toEqual(['testId01']);
        req.flush(mockBulkResponseSuccess);
    });
    it('should archive a container', () => {
        service.archive(['testId01']).subscribe();
        const req = httpMock.expectOne(`${CONTAINER_API_URL}_archive`);

        expect(req.request.method).toBe('PUT');
        expect(req.request.body).toEqual(['testId01']);
        req.flush(mockBulkResponseSuccess);
    });
    it('should unPublish a container', () => {
        service.unPublish(['testId01']).subscribe();
        const req = httpMock.expectOne(`${CONTAINER_API_URL}_unpublish`);

        expect(req.request.method).toBe('PUT');
        expect(req.request.body).toEqual(['testId01']);
        req.flush(mockBulkResponseSuccess);
    });
    it('should publish a container', () => {
        const identifier = 'testId01';
        service.publish(identifier).subscribe();
        const req = httpMock.expectOne(`${CONTAINER_API_URL}_publish?containerId=${identifier}`);

        expect(req.request.method).toBe('PUT');
        expect(req.request.body).toEqual(null);
        req.flush(mockBulkResponseSuccess);
    });
    it('should copy a container', () => {
        service.copy('testId01').subscribe();
        const req = httpMock.expectOne(`${CONTAINER_API_URL}testId01/_copy`);

        expect(req.request.method).toBe('PUT');
        req.flush(mockContainer);
    });
});
