import { DotPageSelectorService } from './dot-page-selector.service';
import { DOTTestBed } from '../../../../../test/dot-test-bed';
import { ConnectionBackend, ResponseOptions, Response } from '@angular/http';
import { MockBackend } from '@angular/http/testing';
import {
    mockDotPageSelectorResults,
    mockDotSiteSelectorResults,
    mockEmptyDotSiteSelectorResults
} from '../dot-page-selector.component.spec';

const MAX_RESULTS_SIZE = 20;

const emptyHostQuery = {
    query: {
        query_string: {
            query: `+contenttype:Host -identifier:SYSTEM_HOST +host.hostName:**`
        }
    },
    size: MAX_RESULTS_SIZE
};

const hostQuery = {
    query: {
        query_string: {
            query: `+contenttype:Host -identifier:SYSTEM_HOST +host.hostName:*demo.dotcms.com*`
        }
    }
};

const hostSpecificQuery = {
    query: {
        query_string: {
            query: `+contenttype:Host -identifier:SYSTEM_HOST +host.hostName:demo.dotcms.com`
        }
    }
};

export const mockEmptyHostDotSiteSelectorResults = Object.assign({}, mockDotSiteSelectorResults);
mockEmptyHostDotSiteSelectorResults.query = '';

describe('Service: DotPageSelector', () => {
    beforeEach(() => {
        this.injector = DOTTestBed.resolveAndCreate([DotPageSelectorService]);
        this.dotPageSelectorService = this.injector.get(DotPageSelectorService);
        this.backend = this.injector.get(ConnectionBackend) as MockBackend;
        this.backend.connections.subscribe((connection: any) => (this.lastConnection = connection));
    });

    it('should get a page by identifier', () => {
        let result;
        const searchParam = 'fdeb07ff-6fc3-4237-91d9-728109bc621d';
        const query = {
            query: {
                query_string: {
                    query: `+basetype:5 +identifier:*${searchParam}*`
                }
            }
        };

        this.dotPageSelectorService.getPageById(searchParam).subscribe(res => {
            result = res;
        });

        this.lastConnection.mockRespond(
            new Response(
                new ResponseOptions({
                    body: {
                        contentlets: [mockDotPageSelectorResults.data[0].payload]
                    }
                })
            )
        );

        expect(result).toEqual(mockDotPageSelectorResults.data[0]);
        expect(this.lastConnection.request.url).toContain('es/search');
        expect(this.lastConnection.request.method).toEqual(1);
        expect(this.lastConnection.request._body).toEqual(query);
    });

    it('should make page search', () => {
        let result;
        this.dotPageSelectorService.currentHost = 'randomHost';
        this.dotPageSelectorService.search('about-us').subscribe(res => {
            result = res;
        });

        this.lastConnection.mockRespond(
            new Response(
                new ResponseOptions({
                    body: {
                        entity: [mockDotPageSelectorResults.data[0].payload]
                    }
                })
            )
        );
        expect(result).toEqual(mockDotPageSelectorResults);
        expect(this.dotPageSelectorService.currentHost).toEqual(null);
        expect(this.lastConnection.request.url).toContain(
            `v1/page/search?path=about-us&onlyLiveSites=true&live=false`
        );
        expect(this.lastConnection.request.method).toEqual(0);
    });

    it('should make a host search', () => {
        let result;
        this.dotPageSelectorService.search('//demo.dotcms.com').subscribe(res => {
            result = res;
        });

        this.lastConnection.mockRespond(
            new Response(
                new ResponseOptions({
                    body: {
                        contentlets: [mockDotSiteSelectorResults.data[0].payload]
                    }
                })
            )
        );
        expect(result).toEqual(mockDotSiteSelectorResults);
        expect(this.lastConnection.request.url).toContain('es/search');
        expect(this.lastConnection.request.method).toEqual(1);
        expect(this.lastConnection.request._body).toEqual(hostQuery);
    });

    it('should make a host search but limit to MAX_RESULTS_SIZE if string is empty', () => {
        let result;
        this.dotPageSelectorService.search('//').subscribe(res => {
            result = res;
        });

        this.lastConnection.mockRespond(
            new Response(
                new ResponseOptions({
                    body: {
                        contentlets: [mockDotSiteSelectorResults.data[0].payload]
                    }
                })
            )
        );
        expect(result).toEqual(mockEmptyHostDotSiteSelectorResults);
        expect(this.lastConnection.request.url).toContain('es/search');
        expect(this.lastConnection.request.method).toEqual(1);
        expect(this.lastConnection.request._body).toEqual(emptyHostQuery);
    });
    it('should make host and page search (Full Search)', () => {
        const connections = [];
        this.backend.connections.subscribe((connection: any) => connections.push(connection));
        let result;
        this.dotPageSelectorService.search('//demo.dotcms.com/about-us').subscribe(res => {
            result = res;
        });
        connections[0].mockRespond(
            new Response(
                new ResponseOptions({
                    body: {
                        contentlets: [mockDotSiteSelectorResults.data[0].payload]
                    }
                })
            )
        );
        connections[1].mockRespond(
            new Response(
                new ResponseOptions({
                    body: {
                        entity: [mockDotPageSelectorResults.data[0].payload]
                    }
                })
            )
        );

        expect(result).toEqual(mockDotPageSelectorResults);
        expect(this.dotPageSelectorService.currentHost).toEqual(
            mockDotSiteSelectorResults.data[0].payload
        );
        expect(connections[0].request._body).toEqual(hostSpecificQuery);
        expect(connections[0].request.url).toEqual('es/search');
        expect(connections[1].request.url).toEqual(
            'v1/page/search?path=//demo.dotcms.com/about-us&onlyLiveSites=true&live=false'
        );
    });

    it('should return empty results on Full Search if host is invalid', () => {
        let result;
        this.dotPageSelectorService.search('//demo.dotcms.com/about-us').subscribe(res => {
            result = res;
        });

        this.lastConnection.mockRespond(
            new Response(
                new ResponseOptions({
                    body: {
                        contentlets: []
                    }
                })
            )
        );
        expect(result).toEqual({
            data: [],
            query: 'demo.dotcms.com',
            type: 'site'
        });
        expect(this.lastConnection.request.url).toContain('es/search');
        expect(this.lastConnection.request._body).toEqual(hostSpecificQuery);
    });

    it('should return empty results when host is invalid', () => {
        let result;
        this.dotPageSelectorService.search('//demo.dotcms.com').subscribe(res => {
            result = res;
        });
        this.lastConnection.mockRespond(
            new Response(
                new ResponseOptions({
                    body: {
                        contentlets: []
                    }
                })
            )
        );
        expect(result).toEqual({
            data: [],
            query: 'demo.dotcms.com',
            type: 'site'
        });
        expect(this.lastConnection.request.url).toContain('es/search');
        expect(this.lastConnection.request.method).toEqual(1);
        expect(this.lastConnection.request._body).toEqual(hostQuery);
    });

    it('should return empty results when page is invalid', () => {
        let result;
        const searchParam = 'invalidPage';
        this.dotPageSelectorService.search(searchParam).subscribe(res => {
            result = res;
        });

        this.lastConnection.mockRespond(
            new Response(
                new ResponseOptions({
                    body: {
                        entity: []
                    }
                })
            )
        );
        expect(result).toEqual({
            data: [],
            query: 'invalidPage',
            type: 'page'
        });
        expect(this.lastConnection.request.url).toContain(
            'v1/page/search?path=invalidPage&onlyLiveSites=true&live=false'
        );
        expect(this.lastConnection.request.method).toEqual(0);
    });
});
