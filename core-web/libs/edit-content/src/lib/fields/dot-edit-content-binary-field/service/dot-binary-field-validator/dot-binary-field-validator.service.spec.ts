import { createServiceFactory, SpectatorService } from '@ngneat/spectator';

import { DotBinaryFieldValidatorService } from './dot-binary-field-validator.service';

describe('DotBinaryFieldValidatorService', () => {
    let spectator: SpectatorService<DotBinaryFieldValidatorService>;
    const createService = createServiceFactory(DotBinaryFieldValidatorService);

    beforeEach(() => (spectator = createService()));

    it('should validate file type', () => {
        spectator.service.setAccept(['image/*', '.ts']);
        expect(
            spectator.service.isValidType({ extension: 'jpg', mimeType: 'image/jpeg' })
        ).toBeTruthy();
        expect(
            spectator.service.isValidType({ extension: 'ts', mimeType: 'text/typescript' })
        ).toBeTruthy();
        expect(
            spectator.service.isValidType({ extension: 'doc', mimeType: 'application/msword' })
        ).toBeFalsy();
    });

    it('should validate file size', () => {
        spectator.service.setMaxFileSize(5000);
        expect(spectator.service.isValidSize(3000)).toBeTruthy();
        expect(spectator.service.isValidSize(6000)).toBeFalsy();
    });
});
