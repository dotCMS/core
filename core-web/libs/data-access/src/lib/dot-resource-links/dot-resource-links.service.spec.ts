import { createHttpFactory, HttpMethod, SpectatorHttp } from '@ngneat/spectator/jest';

import { DotResourceLinks, DotResourceLinksService } from './dot-resource-links.service';

describe('DotResourceLinksService', () => {
    let spectator: SpectatorHttp<DotResourceLinksService>;
    const createHttp = createHttpFactory(DotResourceLinksService);

    beforeEach(() => (spectator = createHttp()));

    it('should get file source links', (done) => {
        const props = {
            fieldVariable: 'testField',
            inodeOrIdentifier: 'testInode'
        };

        const response: DotResourceLinks = {
            configuredImageURL: 'testConfiguredImageURL',
            idPath: 'testIdPath',
            mimeType: 'testMimeType',
            text: 'testText',
            versionPath: 'testVersionPath'
        };

        spectator.service.getFileResourceLinks(props).subscribe((resp) => {
            expect(resp).toEqual(response);
            done();
        });

        const req = spectator.expectOne(
            `/api/v1/content/resourcelinks/field/${props.fieldVariable}?identifier=${props.inodeOrIdentifier}`,
            HttpMethod.GET
        );

        req.flush({ entity: response });
    });
});
