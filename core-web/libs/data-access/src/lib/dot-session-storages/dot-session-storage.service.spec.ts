import { createServiceFactory, SpectatorService } from '@ngneat/spectator/jest';

import { DotSessionstorageService } from './dot-session-storage.service';

describe('DotSessionstorageService', () => {
    let spectator: SpectatorService<DotSessionstorageService>;
    const createService = createServiceFactory(DotSessionstorageService);

    beforeEach(() => {
        spectator = createService();
    });

    describe('setItem', () => {
        it('should set string', () => {
            spectator.service.setItem<string>('hello', 'world');
            expect(window.sessionStorage.getItem('hello')).toBe('world');
        });

        it('should set object', () => {
            spectator.service.setItem<{
                [key: string]: string;
            }>('hello', {
                hola: 'mundo'
            });
            expect(window.sessionStorage.getItem('hello')).toBe('{"hola":"mundo"}');
        });
    });

    describe('getItem', () => {
        it('should get string', () => {
            window.sessionStorage.setItem('hello', 'Hola Mundo');

            const result = spectator.service.getItem<string>('hello');
            expect(result).toBe('Hola Mundo');
        });

        it('should get an array', () => {
            window.sessionStorage.setItem('hello', '["1", "2"]');

            const result = spectator.service.getItem<string[]>('hello');
            expect(result).toEqual(['1', '2']);
        });

        it('should get a boolean', () => {
            window.sessionStorage.setItem('hello', 'true');

            const result = spectator.service.getItem<boolean>('hello');
            expect(result).toEqual(true);
        });
    });

    describe('removeItem', () => {
        beforeEach(() => {
            jest.spyOn(Storage.prototype, 'removeItem');
        });

        it('should remove', () => {
            spectator.service.removeItem('hello');
            expect(window.sessionStorage.removeItem).toHaveBeenCalledWith('hello');
        });
    });

    describe('clear', () => {
        beforeEach(() => {
            jest.spyOn(Storage.prototype, 'clear');
        });

        it('should clean', () => {
            spectator.service.clear();
            expect(window.sessionStorage.clear).toHaveBeenCalledTimes(1);
        });
    });

    describe('listen', () => {
        it('should listen', () => {
            spectator.service.listen<string>('hola').subscribe((res: string) => {
                expect(res).toBe('this is the new value');
            });
            window.dispatchEvent(
                new StorageEvent('storage', {
                    key: 'hola',
                    newValue: 'this is the new value'
                })
            );
        });
    });
});
