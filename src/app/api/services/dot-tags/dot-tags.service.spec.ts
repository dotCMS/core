import { DOTTestBed } from '@tests/dot-test-bed';
import { ConnectionBackend, Response, ResponseOptions } from '@angular/http';
import { MockBackend } from '@angular/http/testing';
import { DotTagsService } from '@services/dot-tags/dot-tags.service';

describe('DotTagsService', () => {
    let dotTagsService: DotTagsService;
    const mockResponse = {
        test: { label: 'test', siteId: '1', siteName: 'Site', persona: false },
        united: { label: 'united', siteId: '1', siteName: 'Site', persona: false }
    };

    beforeEach(() => {
        this.injector = DOTTestBed.resolveAndCreate([DotTagsService]);
        dotTagsService = this.injector.get(DotTagsService);
        this.backend = this.injector.get(ConnectionBackend) as MockBackend;
        this.backend.connections.subscribe((connection: any) => (this.lastConnection = connection));
    });

    it('should get Tags', () => {
        let result;

        dotTagsService.getSuggestions().subscribe(res => {
            result = res;
        });

        this.lastConnection.mockRespond(
            new Response(
                new ResponseOptions({
                    body: mockResponse
                })
            )
        );

        expect(result).toEqual([mockResponse.test, mockResponse.united]);
        expect(this.lastConnection.request.url).toEqual('v1/tags');
    });

    it('should get Tags filtered by name ', () => {
        let result;

        dotTagsService.getSuggestions('test').subscribe(res => {
            result = res;
        });

        this.lastConnection.mockRespond(
            new Response(
                new ResponseOptions({
                    body: mockResponse
                })
            )
        );

        expect(result).toEqual([mockResponse.test, mockResponse.united]);
        expect(this.lastConnection.request.url).toEqual('v1/tags?name=test');
    });
});
