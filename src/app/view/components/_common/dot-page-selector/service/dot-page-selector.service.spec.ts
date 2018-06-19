import { DotPageSelectorService } from './dot-page-selector.service';
import { DOTTestBed } from '../../../../../test/dot-test-bed';
import { ConnectionBackend, ResponseOptions, Response } from '@angular/http';
import { MockBackend } from '@angular/http/testing';
import { mockPageSelector } from '../dot-page-selector.component.spec';

describe('Service: DotPageSelector', () => {
    beforeEach(() => {
        this.injector = DOTTestBed.resolveAndCreate([DotPageSelectorService]);
        this.dotPageSelectorService = this.injector.get(DotPageSelectorService);
        this.backend = this.injector.get(ConnectionBackend) as MockBackend;
        this.backend.connections.subscribe((connection: any) => (this.lastConnection = connection));
    });

    it('should get pages in a folder', () => {
        let result;
        const searchParam = 'about';
        const query = {
            query: {
                query_string: {
                    query: `+basetype:5 +parentpath:*${searchParam}*`
                }
            }
        };

        this.dotPageSelectorService.getPagesInFolder(searchParam).subscribe((res) => {
            result = res;
        });

        this.lastConnection.mockRespond(
            new Response(
                new ResponseOptions({
                    body: {
                        contentlets: [mockPageSelector]
                    }
                })
            )
        );
        expect(result[0]).toEqual(mockPageSelector);
        expect(this.lastConnection.request.url).toContain('es/search');
        expect(this.lastConnection.request.method).toEqual(1);
        expect(this.lastConnection.request._body).toEqual(JSON.stringify(query));
    });

    it('should get a page by identifier', () => {
        let result;
        const searchParam = 'fdeb07ff-6fc3-4237-91d9-728109bc621d';
        const query = {
            query: {
                query_string: {
                    query: `+basetype:5 +identifier:${searchParam}`
                }
            }
        };

        this.dotPageSelectorService.getPage('fdeb07ff-6fc3-4237-91d9-728109bc621d').subscribe((res) => {
            result = res;
        });

        this.lastConnection.mockRespond(
            new Response(
                new ResponseOptions({
                    body: {
                        contentlets: [mockPageSelector[0]]
                    }
                })
            )
        );
        expect(result).toEqual(mockPageSelector[0]);
        expect(this.lastConnection.request.url).toContain('es/search');
        expect(this.lastConnection.request.method).toEqual(1);
        expect(this.lastConnection.request._body).toEqual(JSON.stringify(query));
    });
});
