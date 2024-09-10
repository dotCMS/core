import { createServiceFactory, SpectatorService } from '@ngneat/spectator';

import { provideHttpClient } from '@angular/common/http';

import { DotWysiwygTinymceService } from './dot-wysiwyg-tinymce.service';

describe('DotWysiwygTinyceService', () => {
    let spectator: SpectatorService<DotWysiwygTinymceService>;
    const createService = createServiceFactory({
        service: DotWysiwygTinymceService,
        providers: [provideHttpClient()]
    });

    beforeEach(() => (spectator = createService()));

    it('should...', () => {
        expect(spectator.service).toBeTruthy();
    });
});
