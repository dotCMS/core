import { pluck, take } from 'rxjs/operators';
import { Injectable } from '@angular/core';
import { CoreWebService } from '@dotcms/dotcms-js';
import { DotLocalstorageService } from '@services/dot-localstorage/dot-localstorage.service';
import { formatMessage } from '@shared/dot-utils';

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
        private coreWebService: CoreWebService,
        private dotLocalstorageService: DotLocalstorageService
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
            this.getAll(params.language);

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
                this.getAll(params?.language);
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
        this.coreWebService
            .requestView({
                url: this.geti18nURL(lang)
            })
            .pipe(take(1), pluck('entity'))
            .subscribe((messages: { [key: string]: string }) => {
                this.messageMap = messages;
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
