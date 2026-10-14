import { HttpErrorResponse, provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { DotClusterNodes } from '@dotcms/dotcms-models';

import { DotClusterService } from './dot-cluster.service';

const URL = '/api/v1/cluster/nodes';

/** A minimal `entity`, shaped as in the backend spec (#38032). */
const ENTITY: DotClusterNodes = {
    currentServerId: 'ed54e574-717a-4b47-ba6c-758ff23b3e1b',
    nodes: [
        {
            serverId: '1c2d3e4f-0000-4000-8000-000000000002',
            displayServerId: '1c2d3e4f',
            name: 'dotcms-node-2',
            ipAddress: '172.17.0.6',
            version: '26.10.01',
            lastContactSeconds: 302,
            responded: false,
            status: 'DOWN',
            cache: null,
            search: null,
            assets: null
        }
    ]
};

describe('DotClusterService', () => {
    let service: DotClusterService;
    let httpMock: HttpTestingController;

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [provideHttpClient(), provideHttpClientTesting()]
        });

        service = TestBed.inject(DotClusterService);
        httpMock = TestBed.inject(HttpTestingController);
    });

    afterEach(() => httpMock.verify());

    it('should load the cluster with one GET to /api/v1/cluster/nodes and return its entity', () => {
        let result: DotClusterNodes | undefined;
        service.getNodes().subscribe((nodes) => (result = nodes));

        const req = httpMock.expectOne(URL);
        expect(req.request.method).toBe('GET');
        req.flush({ entity: ENTITY, errors: [], messages: [] });

        expect(result).toEqual(ENTITY);
    });

    it('should propagate an error response', () => {
        let error: HttpErrorResponse | undefined;
        service.getNodes().subscribe({ error: (e) => (error = e) });

        httpMock.expectOne(URL).flush(null, { status: 401, statusText: 'Unauthorized' });

        expect(error?.status).toBe(401);
    });
});
