import { fromEvent, Observable } from 'rxjs';

import { Injectable } from '@angular/core';

import { map, filter } from 'rxjs/operators';

const getValue = <T>(item: string): T => {
    const isNumber = parseInt(item, 0) as unknown;

    if (isNumber) {
        return isNumber as T;
    }

    try {
        return JSON.parse(item);
    } catch {
        return item as unknown as T;
    }
};

/**
 * A service to wrap the window SessionStorage API
 *
 * @export
 * @class DotSessionstorageService
 */
@Injectable({
    providedIn: 'root'
})
export class DotSessionstorageService {
    /**
     * Save an item to SessionStorage
     *
     * @template T
     * @param {string} key
     * @param {T} value
     * @memberof DotSessionstorageService
     */
    setItem<T = string>(key: string, value: T): void {
        let data;

        if (typeof value === 'object') {
            data = JSON.stringify(value);
        } else {
            data = value;
        }

        sessionStorage.setItem(key, data as string);
    }

    /**
     * Get an item from SessionStorage
     *
     * @template T
     * @param {string} key
     * @returns {T}
     * @memberof DotSessionstorageService
     */
    getItem<T>(key: string): T {
        const item: string = sessionStorage.getItem(key) || '';

        return <T>getValue(item);
    }

    /**
     * Remove an item form the SessionStorage
     *
     * @param {string} key
     * @memberof DotSessionstorageService
     */
    removeItem(key: string): void {
        sessionStorage.removeItem(key);
    }

    /**
     * Remove all items from SessionStorage
     *
     * @memberof DotSessionstorageService
     */
    clear(): void {
        sessionStorage.clear();
    }

    /**
     * Listen for a change of a particular key in the SessionStorage
     *
     * @template T
     * @param {string} filterBy
     * @returns {Observable<T>}
     * @memberof DotSessionstorageService
     */
    listen<T>(filterBy: string): Observable<T> {
        return fromEvent<StorageEvent>(window, 'storage').pipe(
            filter(({ key }: StorageEvent) => key === filterBy),
            map((e: StorageEvent) => <T>getValue(e.newValue || ''))
        );
    }
}
