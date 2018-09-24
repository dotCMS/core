import { DotPersonasService } from './dot-personas.service';
import { DOTTestBed } from '../../../test/dot-test-bed';
import { ConnectionBackend, ResponseOptions, Response } from '@angular/http';
import { MockBackend } from '@angular/http/testing';
import { mockDotPersona } from '../../../test/dot-persona.mock';

describe('DotPersonasService', () => {
    beforeEach(() => {
        this.injector = DOTTestBed.resolveAndCreate([DotPersonasService]);
        this.dotPersonasService = this.injector.get(DotPersonasService);
        this.backend = this.injector.get(ConnectionBackend) as MockBackend;
        this.backend.connections.subscribe((connection: any) => (this.lastConnection = connection));
    });

    it('should get Personas', () => {
        let result;
        const url = [`content/render/false/query/+contentType:persona `, `+live:true `, `+deleted:false `, `+working:true`].join('');

        this.dotPersonasService.get().subscribe((res) => {
            result = res;
        });

        this.lastConnection.mockRespond(
            new Response(
                new ResponseOptions({
                    body: {
                        contentlets: [mockDotPersona]
                    }
                })
            )
        );
        expect(result).toEqual(Array.of(mockDotPersona));
        expect(this.lastConnection.request.method).toBe(0); // 0 is GET method
        expect(this.lastConnection.request.url).toContain(url);
    });
});
