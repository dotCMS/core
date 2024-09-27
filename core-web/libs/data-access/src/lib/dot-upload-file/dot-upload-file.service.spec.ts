import { createHttpFactory, HttpMethod, SpectatorHttp } from '@ngneat/spectator/jest';

import { DotUploadFileService } from './dot-upload-file.service';

describe('DotUploadFileService', () => {
  let spectator: SpectatorHttp<DotUploadFileService>;
  const createHttp = createHttpFactory({
    service: DotUploadFileService,
    providers: [DotUploadFileService]
  });

  beforeEach(() => spectator = createHttp());

  it('should be created', () => {
    expect(spectator.service).toBeTruthy();
  });

  describe('uploadDotAsset', () => {
    it('should upload a file as a dotAsset', () => {
        const file = new File([''], 'test.png', {
            type: 'image/png'
        });
        
        spectator.service.uploadDotAsset(file).subscribe();

        const req = spectator.expectOne('/api/v1/workflow/actions/default/fire/NEW', HttpMethod.PUT);

        expect(req.request.body.get('json')).toEqual(
            JSON.stringify({
                contentlet: {
                    file: 'test.png',
                    contentType: 'dotAsset'
                }
            })
        );

        req.flush({ entity: { identifier: 'test' } });
    });
  });
});