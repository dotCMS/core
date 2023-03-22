import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';

import { pluck, take } from 'rxjs/operators';

import { formatMessage } from '@dotcms/utils';

import { DotLocalstorageService } from '../dot-localstorage/dot-localstorage.service';

export interface DotMessageServiceParams {
    buildDate?: string;
    language?: string;
}

@Injectable({
    providedIn: 'root'
})
export class DotMessageService {
    private messageMap: { [key: string]: string } = {};
    private MESSAGES_LOCALSTORAGE_KEY = 'dotMessagesKeys';
    private BUILDATE_LOCALSTORAGE_KEY = 'buildDate';

    constructor(
        private readonly http: HttpClient,
        private readonly dotLocalstorageService: DotLocalstorageService
    ) {}

    /**
     * Get all messages keys form endpoint if they are not set in the localStorage.
     * If a language is passed or if buildDate is different than what is local then
     * key messages in localStorage are replaced
     *
     * @param DotMessageServiceParams [params]
     * @memberof DotMessageService
     */
    init(params?: DotMessageServiceParams): void {
        if (
            params &&
            (this.dotLocalstorageService.getItem(this.BUILDATE_LOCALSTORAGE_KEY) !==
                params?.buildDate ||
                params.language)
        ) {
            this.getAll(params.language || 'default');

            if (params.buildDate) {
                this.dotLocalstorageService.setItem(
                    this.BUILDATE_LOCALSTORAGE_KEY,
                    params.buildDate
                );
            }
        } else {
            const keys: { [key: string]: string } = this.dotLocalstorageService.getItem(
                this.MESSAGES_LOCALSTORAGE_KEY
            );
            if (!keys) {
                this.getAll(params?.language || 'default');
            } else {
                this.messageMap = keys;
            }
        }
    }

    /**
     * Return the message key value, formatted if more values are passed.
     *
     * @param string key
     * @returns string
     * @memberof DotMessageService
     */
    get(key: string, ...args: string[]): string {
        return this.messageMap[key]
            ? args.length
                ? formatMessage(this.messageMap[key], args)
                : this.messageMap[key]
            : key;
    }

    private getAll(lang: string): void {
        this.http
            .get(this.geti18nURL(lang))
            .pipe(take(1), pluck('entity'))
            .subscribe((messages) => {
                this.messageMap = messages as { [key: string]: string };
                this.dotLocalstorageService.setItem(
                    this.MESSAGES_LOCALSTORAGE_KEY,
                    this.messageMap
                );
            });
    }

    private geti18nURL(lang: string): string {
        return `/api/v2/languages/${lang ? lang : 'default'}/keys`;
    }
}
