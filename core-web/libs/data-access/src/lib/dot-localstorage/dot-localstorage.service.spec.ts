import { TestBed } from '@angular/core/testing';

import { DotLocalstorageService } from './dot-localstorage.service';

describe('DotLocalstorageService', () => {
    let service: DotLocalstorageService;
    beforeEach(() => {
        TestBed.configureTestingModule({});

        service = TestBed.inject(DotLocalstorageService);
    });

    describe('setItem', () => {
        it('should set string', () => {
            service.setItem<string>('hello', 'world');
            expect(window.localStorage.getItem('hello')).toBe('world');
        });

        it('should set object', () => {
            service.setItem<{
                [key: string]: string;
            }>('hello', {
                hola: 'mundo'
            });
            expect(window.localStorage.getItem('hello')).toBe('{"hola":"mundo"}');
        });
    });

    describe('getItem', () => {
        it('should get string', () => {
            window.localStorage.setItem('hello', 'Hola Mundo');

            const result = service.getItem<string>('hello');
            expect(result).toBe('Hola Mundo');
        });

        it('should get an array', () => {
            window.localStorage.setItem('hello', '["1", "2"]');

            const result = service.getItem<string[]>('hello');
            expect(result).toEqual(['1', '2']);
        });

        it('should get a boolean', () => {
            window.localStorage.setItem('hello', 'true');

            const result = service.getItem<boolean>('hello');
            expect(result).toEqual(true);
        });
    });

    describe('removeItem', () => {
        beforeEach(() => {
            jest.spyOn(Storage.prototype, 'removeItem');
        });

        it('should remove', () => {
            service.removeItem('hello');
            expect(window.localStorage.removeItem).toHaveBeenCalledWith('hello');
        });
    });

    describe('clear', () => {
        beforeEach(() => {
            jest.spyOn(Storage.prototype, 'clear');
        });

        it('should clean', () => {
            service.clear();
            expect(window.localStorage.clear).toHaveBeenCalledTimes(1);
        });
    });

    describe('listen', () => {
        it('should listen', () => {
            service.listen('hola').subscribe((res: string) => {
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
