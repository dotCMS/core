import { createServiceFactory, SpectatorService } from '@ngneat/spectator';

import { DotWysiwygTinymceService } from './dot-wysiwyg-tinymce.service';

describe('DotWysiwygTinyceService', () => {
    let spectator: SpectatorService<DotWysiwygTinymceService>;
    const createService = createServiceFactory(DotWysiwygTinymceService);

    beforeEach(() => (spectator = createService()));

    it('should...', () => {
        expect(spectator.service).toBeTruthy();
    });
});
