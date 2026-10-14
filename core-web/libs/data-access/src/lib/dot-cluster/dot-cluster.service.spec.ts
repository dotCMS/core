import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { DotClusterNodes } from '@dotcms/dotcms-models';

import { DOT_CLUSTER_NODES_MOCK } from './dot-cluster.mock';
import { DotClusterService } from './dot-cluster.service';

describe('DotClusterService', () => {
    let service: DotClusterService;
    let httpMock: HttpTestingController;

    beforeEach(() => {
        vi.useFakeTimers();
        TestBed.configureTestingModule({
            providers: [provideHttpClient(), provideHttpClientTesting()]
        });

        service = TestBed.inject(DotClusterService);
        httpMock = TestBed.inject(HttpTestingController);
    });

    afterEach(() => {
        httpMock.verify();
        vi.useRealTimers();
    });

    it('should serve the mocked cluster without any HTTP call', () => {
        let result: DotClusterNodes | undefined;
        service.getNodes().subscribe((nodes) => (result = nodes));

        expect(result).toBeUndefined();
        vi.runAllTimers();

        expect(result).toBe(DOT_CLUSTER_NODES_MOCK);
    });
});

describe('DOT_CLUSTER_NODES_MOCK', () => {
    const { nodes, currentServerId } = DOT_CLUSTER_NODES_MOCK;

    it('should contain the node serving the request', () => {
        expect(nodes.some((node) => node.serverId === currentServerId)).toBe(true);
    });

    it('should cover every node status', () => {
        expect(new Set(nodes.map((node) => node.status))).toEqual(
            new Set(['UP', 'LAGGING', 'DOWN'])
        );
    });

    it('should send null sections for a node that did not answer, as the backend does', () => {
        const down = nodes.find((node) => !node.responded);

        expect(down?.status).toBe('DOWN');
        expect(down?.cache).toBeNull();
        expect(down?.search).toBeNull();
        expect(down?.assets).toBeNull();
    });
});
