import { Injectable, NgModule } from '@angular/core';

/**
 * LocalStoreService. Basic wraper for localStorage
 */
@Injectable()
export class LocalStoreService {
    /**
     * Stores Value in localstorage
     * @param key
     * @param value
     */
    storeValue(key: string, value: any): void {
        localStorage.setItem(key, value);
        // storage.set(key, value, callback);
    }

    /**
     * Gets a value from localstorage
     * @param key
     * @returns any
     */
    getValue(key: string): any {
        return localStorage.getItem(key);
        // return storage.get(key, callback);
    }

    /**
     * Clears a value from localstorage
     * @param key
     */
    clearValue(key: string): void {
        localStorage.removeItem(key);
    }

    /**
     * Clears all localstorage
     */
    clear(): void {
        localStorage.clear();
    }
}

@NgModule({
    providers: [LocalStoreService]
})
export class DotLocalStoreModule {}
