import { MockBackend } from '@angular/http/testing';
import { ConnectionBackend, Response, ResponseOptions } from '@angular/http';
import { fakeAsync, tick } from '@angular/core/testing';
import { DotPageLayoutService } from './dot-page-layout.service';
import { DOTTestBed } from '../../../test/dot-test-bed';
import { mockDotLayout } from '../../../test/dot-page-render.mock';

describe('PageViewService', () => {
    let service: DotPageLayoutService;
    beforeEach(() => {
        this.injector = DOTTestBed.resolveAndCreate([DotPageLayoutService]);

        service = this.injector.get(DotPageLayoutService);
        this.backend = this.injector.get(ConnectionBackend) as MockBackend;
        this.backend.connections.subscribe((connection: any) => (this.lastConnection = connection));
    });

    it(
        'should post data and return an entity',
        fakeAsync(() => {
            let result;

            const mockResponse = {
                entity: [
                    Object.assign({}, mockDotLayout, {
                        iDate: 1495670226000,
                        identifier: '1234-id-7890-entifier',
                        modDate: 1495670226000
                    })
                ]
            };

            service
                .save('test38923-82393842-23823', mockDotLayout)
                .subscribe((res) => (result = res));
            this.lastConnection.mockRespond(
                new Response(
                    new ResponseOptions({
                        body: JSON.stringify(mockResponse)
                    })
                )
            );

            tick();
            expect(this.lastConnection.request.url).toContain(
                'v1/page/test38923-82393842-23823/layout'
            );
            expect(result.params).toEqual(mockResponse.entity);
        })
    );
});
