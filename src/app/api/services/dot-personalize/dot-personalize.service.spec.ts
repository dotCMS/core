import { DotPersonalizeService } from './dot-personalize.service';
import { DOTTestBed } from '../../../test/dot-test-bed';
import { ConnectionBackend, ResponseOptions, Response, RequestMethod } from '@angular/http';
import { MockBackend } from '@angular/http/testing';

describe('DotPersonalizeService', () => {
    beforeEach(() => {
        this.injector = DOTTestBed.resolveAndCreate([DotPersonalizeService]);
        this.dotPersonalizeService = this.injector.get(DotPersonalizeService);
        this.backend = this.injector.get(ConnectionBackend) as MockBackend;
        this.backend.connections.subscribe((connection: any) => (this.lastConnection = connection));
    });

    it('should set Personalized', () => {
        this.dotPersonalizeService.personalized('a', 'b').subscribe();

        this.lastConnection.mockRespond(
            new Response(
                new ResponseOptions({
                    body: []
                })
            )
        );
        expect(this.lastConnection.request.method).toBe(RequestMethod.Post);
        expect(this.lastConnection.request.url).toContain('/api/v1/personalization/pagepersonas');
        expect(this.lastConnection.request._body).toEqual({ pageId: 'a', personaTag: 'b' });
    });

    it('should despersonalized', () => {
        const pageId = 'a';
        const personaTag = 'b';
        this.dotPersonalizeService.despersonalized(pageId, personaTag).subscribe();

        this.lastConnection.mockRespond(
            new Response(
                new ResponseOptions({
                    body: []
                })
            )
        );
        expect(this.lastConnection.request.method).toBe(RequestMethod.Delete);
        expect(this.lastConnection.request.url).toContain(
            `/api/v1/personalization/pagepersonas/page/${pageId}/personalization/${personaTag}`
        );
    });
});
