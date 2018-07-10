import { DotThemesService } from './dot-themes.service';
import { DOTTestBed } from '../../../test/dot-test-bed';
import { ConnectionBackend, ResponseOptions, Response } from '@angular/http';
import { MockBackend } from '@angular/http/testing';
import { mockDotThemes } from '../../../test/dot-themes.mock';
import { DotTheme } from '../../../portlets/dot-edit-page/shared/models/dot-theme.model';

describe('DotThemesService', () => {
    beforeEach(() => {
        this.injector = DOTTestBed.resolveAndCreate([DotThemesService]);
        this.dotThemesService = this.injector.get(DotThemesService);
        this.backend = this.injector.get(ConnectionBackend) as MockBackend;
        this.backend.connections.subscribe((connection: any) => (this.lastConnection = connection));
    });

    it('should get Themes', () => {
        this.dotThemesService.get('inode').subscribe((themes: DotTheme) => {
            expect(themes).toEqual(mockDotThemes[0]);
        });

        this.lastConnection.mockRespond(
            new Response(
                new ResponseOptions({
                    body: {
                        entity: mockDotThemes[0]
                    }
                })
            )
        );
    });
});
