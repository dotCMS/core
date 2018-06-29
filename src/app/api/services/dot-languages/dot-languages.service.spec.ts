import { DotLanguagesService } from './dot-languages.service';
import { DOTTestBed } from '../../../test/dot-test-bed';
import { ConnectionBackend, ResponseOptions, Response } from '@angular/http';
import { MockBackend } from '@angular/http/testing';
import { mockDotLanguage } from '../../../test/dot-language.mock';

describe('DotLanguagesService', () => {
    beforeEach(() => {
        this.injector = DOTTestBed.resolveAndCreate([DotLanguagesService]);
        this.dotLanguagesService = this.injector.get(DotLanguagesService);
        this.backend = this.injector.get(ConnectionBackend) as MockBackend;
        this.backend.connections.subscribe((connection: any) => (this.lastConnection = connection));
    });

    it('should get Languages', () => {
        let result;

        this.dotLanguagesService.get().subscribe(res => {
            result = res;
        });

        this.lastConnection.mockRespond(
            new Response(
                new ResponseOptions({
                    body: {
                        entity: [mockDotLanguage]
                    }
                })
            )
        );

        expect(result).toEqual([mockDotLanguage]);
        expect(this.lastConnection.request.url).toContain('v2/languages');
    });

    it('should get Languages by content indode', () => {
        let result;

        this.dotLanguagesService.get('2').subscribe(res => {
            result = res;
        });

        this.lastConnection.mockRespond(
            new Response(
                new ResponseOptions({
                    body: {
                        entity: [mockDotLanguage]
                    }
                })
            )
        );

        expect(result).toEqual([mockDotLanguage]);
        expect(this.lastConnection.request.url).toContain('v2/languages?contentInode=2');
    });
});
