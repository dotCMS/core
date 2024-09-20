import { HttpMethod } from '@ngneat/spectator';
import { createHttpFactory, SpectatorHttp } from '@ngneat/spectator/jest';

import { CONFIG_PATH, DotWysiwygTinymceService } from './dot-wysiwyg-tinymce.service';

describe('DotWysiwygTinyceService', () => {
    let spectator: SpectatorHttp<DotWysiwygTinymceService>;
    const createHttp = createHttpFactory(DotWysiwygTinymceService);

    beforeEach(() => (spectator = createHttp()));

    it('should do the configuration healthcheck', () => {
        spectator.service.getProps().subscribe();
        spectator.expectOne(`${CONFIG_PATH}/tinymceprops`, HttpMethod.GET);
    });

    it('should return null if HTTP request fails with status 400', () => {
        spectator.service.getProps().subscribe((response) => {
            expect(response).toBeNull();
        });

        spectator.expectOne(`${CONFIG_PATH}/tinymceprops`, HttpMethod.GET).flush(null, {
            status: 400,
            statusText: 'Bad Request'
        });
    });
});
