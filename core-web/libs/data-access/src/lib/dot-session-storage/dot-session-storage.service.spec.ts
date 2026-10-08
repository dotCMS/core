import { createServiceFactory, SpectatorService } from '@openng/spectator/vitest';

import { DotSessionStorageService } from './dot-session-storage.service';

describe('DotSessionStorageService', () => {
    let spectator: SpectatorService<DotSessionStorageService>;

    const createService = createServiceFactory(DotSessionStorageService);

    beforeEach(() => {
        sessionStorage.clear();
        spectator = createService();
    });

    describe('last content type', () => {
        it('should return the remembered content type once, then nothing', () => {
            spectator.service.setLastContentType('anyContentType');

            expect(spectator.service.consumeLastContentType()).toBe('anyContentType');
            expect(spectator.service.consumeLastContentType()).toBeNull();
        });

        it('should return nothing when no content type was remembered', () => {
            expect(spectator.service.consumeLastContentType()).toBeNull();
        });

        it('should keep only the last content type remembered', () => {
            spectator.service.setLastContentType('firstContentType');
            spectator.service.setLastContentType('secondContentType');

            expect(spectator.service.consumeLastContentType()).toBe('secondContentType');
        });

        it('should forget the remembered content type when removed', () => {
            spectator.service.setLastContentType('anyContentType');

            spectator.service.removeLastContentType();

            expect(spectator.service.consumeLastContentType()).toBeNull();
        });

        it('should not touch the variation id', () => {
            spectator.service.setVariationId('aVariation');
            spectator.service.setLastContentType('anyContentType');

            spectator.service.consumeLastContentType();

            expect(spectator.service.getVariationId()).toBe('aVariation');
        });
    });
});
