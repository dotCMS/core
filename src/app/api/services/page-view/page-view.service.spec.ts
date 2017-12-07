import { MockBackend } from '@angular/http/testing';
import { ConnectionBackend, Response, ResponseOptions } from '@angular/http';
import { fakeAsync, tick } from '@angular/core/testing';
import { PageViewService } from './page-view.service';
import { DOTTestBed } from './../../../test/dot-test-bed';

describe('PageViewService', () => {
    beforeEach(() => {
        this.injector = DOTTestBed.resolveAndCreate([
            PageViewService
        ]);

        this.pageViewService =  this.injector.get(PageViewService);
        this.backend = this.injector.get(ConnectionBackend) as MockBackend;
        this.backend.connections.subscribe((connection: any) => this.lastConnection = connection);
    });

    it('should do a get request with url param', () => {
        let result: any;
        this.pageViewService.get('about-us').subscribe(items => result = items);

        expect(this.lastConnection.request.url).toContain('v1/page/render/about-us?live=false');
    });

    it('should remove the leading slash if present when calling pageViewService', () => {
        this.pageViewService.get('/aboutUs/index');
        expect(this.lastConnection.request.url).toContain('v1/page/render/aboutUs/index');
    });

    it('should do a get request and return a pageView', fakeAsync(() => {
        let result: any;

        this.pageViewService.get('about-us').subscribe(items => result = items);

        const mockResponse = {
            layout: {
                body: {
                    containers: ['string1', 'string2'],
                    rows: ['column']
                }
            },
            page: {
                identifier: 'test38923-82393842-23823'
            }
        };

        this.lastConnection.mockRespond(new Response(new ResponseOptions({
            body: mockResponse,
        })));

        tick();

        expect(result).toEqual(mockResponse);
    }));

    it('should post data and return an entity', fakeAsync(() => {
        let result;
        const mockDotLayout = {
            body: {
                containers: ['string1', 'string2'],
                rows: ['column']
            }
        };

        const mockResponse = {
            entity: [
                Object.assign({}, mockDotLayout, {
                    'iDate': 1495670226000,
                    'identifier': '1234-id-7890-entifier',
                    'modDate': 1495670226000
                })
            ]
        };

        this.pageViewService.save('test38923-82393842-23823', mockDotLayout).subscribe(res => result = res);
        this.lastConnection.mockRespond(new Response(new ResponseOptions({
            body: JSON.stringify(mockResponse)
        })));

        tick();
        expect(this.lastConnection.request.url).toContain('v1/page/test38923-82393842-23823/layout');
        expect(result).toEqual(mockResponse.entity);
    }));
});
