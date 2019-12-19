import { Injectable } from '@angular/core';
import { fromEvent, Observable } from 'rxjs';
import { map, filter } from 'rxjs/operators';

const getValue = <T>(item): T => {
    const isNumber = parseInt(item, 0) as unknown;

    if (isNumber) {
        return isNumber as T;
    }

    try {
        return JSON.parse(item);
    } catch {
        return (item as unknown) as T;
    }
};
/**
 * A service to wrap the window localstorage API
 *
 * @export
 * @class DotLocalstorageService
 */
@Injectable({
    providedIn: 'root'
})
export class DotLocalstorageService {
    constructor() {}

    /**
     * Save an item to localstorage
     *
     * @template T
     * @param {string} key
     * @param {T} value
     * @memberof DotLocalstorageService
     */
    setItem<T>(key: string, value: T): void {
        let data;

        if (typeof value === 'object') {
            data = JSON.stringify(value);
        } else {
            data = value;
        }

        localStorage.setItem(key, data);
    }

    /**
     * Get an item from localstorage
     *
     * @template T
     * @param {string} key
     * @returns {T}
     * @memberof DotLocalstorageService
     */
    getItem<T>(key: string): T {
        const item: string = localStorage.getItem(key);

        return <T>getValue(item);
    }

    /**
     * Remove an item form the localstorage
     *
     * @param {string} key
     * @memberof DotLocalstorageService
     */
    removeItem(key: string): void {
        localStorage.removeItem(key);
    }

    /**
     * Remove all items from localstorage
     *
     * @memberof DotLocalstorageService
     */
    clear(): void {
        localStorage.clear();
    }

    /**
     * Listen for a change of a particular key in the localstorage
     *
     * @template T
     * @param {string} filterBy
     * @returns {Observable<T>}
     * @memberof DotLocalstorageService
     */
    listen<T>(filterBy: string): Observable<T> {
        return fromEvent(window, 'storage').pipe(
            filter(({ key }: StorageEvent) => key === filterBy),
            map((e: StorageEvent) => <T>getValue(e.newValue))
        );
    }
}
