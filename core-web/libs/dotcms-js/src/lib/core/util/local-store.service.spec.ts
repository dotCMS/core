import { inject, TestBed } from '@angular/core/testing';

import { LocalStoreService } from './local-store.service';

describe('Local Store Service', () => {
    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [LocalStoreService]
        });
    });

    it('should store and get value', inject(
        [LocalStoreService],
        (localStoreService: LocalStoreService) => {
            localStoreService.storeValue('hello', 'world');
            expect(localStoreService.getValue('hello')).toBe('world');
        }
    ));

    it('should store and clear a value', inject(
        [LocalStoreService],
        (localStoreService: LocalStoreService) => {
            localStoreService.storeValue('hello', 'world');
            localStoreService.clearValue('hello');
            expect(localStoreService.getValue('hello')).toBe(null);
        }
    ));

    it('should store and clear all values', inject(
        [LocalStoreService],
        (localStoreService: LocalStoreService) => {
            localStoreService.storeValue('hello', 'world');
            localStoreService.storeValue('foo', 'bar');
            localStoreService.clear();
            expect(localStoreService.getValue('hello')).toBe(null);
            expect(localStoreService.getValue('foo')).toBe(null);
        }
    ));
});
