import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';

import { CoreWebService } from '@dotcms/dotcms-js';

import { DotHttpErrorManagerService } from '@services/dot-http-error-manager/dot-http-error-manager.service';
import { DotContainersService, CONTAINER_API_URL } from './dot-containers.service';
import { CoreWebServiceMock } from '@tests/core-web.service.mock';
import { DotActionBulkResult } from '@models/dot-action-bulk-result/dot-action-bulk-result.model';
import { of } from 'rxjs';
import {
    CONTAINER_SOURCE,
    DotContainerEntity,
    DotContainerPayload
} from '@models/container/dot-container.model';

const mockBulkResponseSuccess: DotActionBulkResult = {
    skippedCount: 0,
    successCount: 1,
    fails: []
};

const mockContainer: DotContainerEntity = {
    container: {
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
    },
    contentTypes: []
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
        service.get().subscribe((container: DotContainerEntity[]) => {
            expect(container).toEqual([mockContainer]);
        });

        const req = httpMock.expectOne(CONTAINER_API_URL);

        expect(req.request.method).toBe('GET');

        req.flush({
            entity: [mockContainer]
        });
    });

    it('should get a container by id', () => {
        service.getById('123').subscribe((containerEntity: DotContainerEntity) => {
            expect(containerEntity).toEqual(mockContainer);
        });

        const req = httpMock.expectOne(`${CONTAINER_API_URL}working?containerId=123`);

        expect(req.request.method).toBe('GET');

        req.flush({
            entity: {
                ...mockContainer
            }
        });
    });

    it('should get a containers by filter', () => {
        service.getFiltered('123').subscribe((container: DotContainerEntity[]) => {
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
                title: '',
                friendlyName: ''
            } as DotContainerPayload)
            .subscribe((container: DotContainerEntity) => {
                expect(container).toEqual(mockContainer);
            });

        const req = httpMock.expectOne(`${CONTAINER_API_URL}`);

        expect(req.request.method).toBe('POST');
        expect(req.request.body.title).toEqual('');
        expect(req.request.body.friendlyName).toEqual('');

        req.flush({
            entity: mockContainer
        });
    });

    it('should put to update a container', () => {
        service
            .update({
                title: '',
                friendlyName: ''
            } as DotContainerPayload)
            .subscribe((container) => {
                expect(container).toEqual(mockContainer);
            });

        const req = httpMock.expectOne(CONTAINER_API_URL);

        expect(req.request.method).toBe('PUT');
        expect(req.request.body.title).toEqual('');
        expect(req.request.body.friendlyName).toEqual('');

        req.flush({
            entity: mockContainer
        });
    });
    it('should put to save and publish a container', () => {
        service
            .saveAndPublish({
                container: { name: '', friendlyName: '' },
                contentTypes: []
            } as DotContainerEntity)
            .subscribe((container: DotContainerEntity) => {
                expect(container).toEqual(mockContainer);
            });

        const req = httpMock.expectOne(`${CONTAINER_API_URL}_savepublish`);

        expect(req.request.method).toBe('PUT');
        expect(req.request.body.container.name).toEqual('');
        expect(req.request.body.container.friendlyName).toEqual('');

        req.flush({
            entity: mockContainer
        });
    });
    it('should delete a container', () => {
        service.delete(['testId01']).subscribe();
        const req = httpMock.expectOne(`${CONTAINER_API_URL}_bulkdelete`);

        expect(req.request.method).toBe('DELETE');
        expect(req.request.body).toEqual(['testId01']);
        req.flush(mockBulkResponseSuccess);
    });
    it('should unArchive a container', () => {
        service.unArchive(['testId01']).subscribe();
        const req = httpMock.expectOne(`${CONTAINER_API_URL}_bulkunarchive`);

        expect(req.request.method).toBe('PUT');
        expect(req.request.body).toEqual(['testId01']);
        req.flush(mockBulkResponseSuccess);
    });
    it('should archive a container', () => {
        service.archive(['testId01']).subscribe();
        const req = httpMock.expectOne(`${CONTAINER_API_URL}_bulkarchive`);

        expect(req.request.method).toBe('PUT');
        expect(req.request.body).toEqual(['testId01']);
        req.flush(mockBulkResponseSuccess);
    });
    it('should unPublish a container', () => {
        service.unPublish(['testId01']).subscribe();
        const req = httpMock.expectOne(`${CONTAINER_API_URL}_bulkunpublish`);

        expect(req.request.method).toBe('PUT');
        expect(req.request.body).toEqual(['testId01']);
        req.flush(mockBulkResponseSuccess);
    });
    it('should publish a container', () => {
        const identifier = 'testId01';
        service.publish([identifier]).subscribe();
        const req = httpMock.expectOne(`${CONTAINER_API_URL}_bulkpublish`);

        expect(req.request.method).toBe('PUT');
        expect(req.request.body).toEqual([identifier]);
        req.flush(mockBulkResponseSuccess);
    });
    it('should copy a container', () => {
        service.copy('testId01').subscribe();
        const req = httpMock.expectOne(`${CONTAINER_API_URL}testId01/_copy`);

        expect(req.request.method).toBe('PUT');
        req.flush(mockContainer);
    });
});
