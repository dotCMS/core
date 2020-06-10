import { pluck, take } from 'rxjs/operators';
import * as _ from 'lodash';
import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';
import { RequestMethod } from '@angular/http';
import { CoreWebService } from 'dotcms-js';
import { DotLocalstorageService } from '@services/dot-localstorage/dot-localstorage.service';
import { FormatDateService } from '@services/format-date-service';

@Injectable({
    providedIn: 'root'
})
export class DotMessageService {
    private messageMap: { [key: string]: string } = {};
    private MESSAGES_LOCALSTORAGE_KEY = 'dotMessagesKeys';

    constructor(
        private formatDateService: FormatDateService,
        private coreWebService: CoreWebService,
        private dotLocalstorageService: DotLocalstorageService
    ) {}

    /**
     * Get all messages keys form endpoint if they are not set in the localStorage.
     * If a language is passed replace what is in localStorage
     *
     * @param string language
     * @memberof DotMessageService
     */
    init(language?: string): void {
        const keys: { [key: string]: string } = this.dotLocalstorageService.getItem(
            this.MESSAGES_LOCALSTORAGE_KEY
        );
        if (language || !keys) {
            this.getAll(language).subscribe((messages: { [key: string]: string }) => {
                this.messageMap = messages;
                this.dotLocalstorageService.setItem(
                    this.MESSAGES_LOCALSTORAGE_KEY,
                    this.messageMap
                );
            });
        } else {
            this.messageMap = keys;
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
            ? args.length ? this.formatMessage(this.messageMap[key], args) : this.messageMap[key]
            : key;
    }

    /**
     * Get the messages objects as an Observable
     * @returns Observable<any>
     */
    get messageMap$(): Observable<any> {
        return of(this.messageMap);
    }

    /**
     * Public method to get messages, will get from object messageMap in memory.
     * @param keys
     * @returns any
     */
    getMessages(keys: String[]): Observable<any> {
        return Observable.create(observer => {
            observer.next(_.pick(this.messageMap, keys));
            observer.complete();
        });
    }

    setRelativeDateMessages(languageId: string): void {
        const relativeDateKeys = [
            'relativetime.future',
            'relativetime.past',
            'relativetime.s',
            'relativetime.m',
            'relativetime.mm',
            'relativetime.h',
            'relativetime.hh',
            'relativetime.d',
            'relativetime.dd',
            'relativetime.M',
            'relativetime.MM',
            'relativetime.y',
            'relativetime.yy'
        ];
        this.getMessages(relativeDateKeys).subscribe(res => {
            const relativeDateMessages = _.mapKeys(res, (_value, key: string) => {
                return key.replace('relativetime.', '');
            });
            this.formatDateService.setLang(languageId.split('_')[0], relativeDateMessages);
        });
    }

    private getAll(lang: string): Observable<{ [key: string]: string }> {
        return this.coreWebService
            .requestView({
                method: RequestMethod.Get,
                url: this.geti18nURL(lang)
            })
            .pipe(take(1), pluck('entity'));
    }

    private geti18nURL(lang: string): string {
        return `/api/v2/languages/${lang ? lang : 'default'}/keys`;
    }

    // Replace {n} in the string with the strings in the args array
    private formatMessage(message: string, args: string[]): string {
        return message.replace(/{(\d+)}/g, (match, number) => {
            return typeof args[number] !== 'undefined' ? args[number] : match;
        });
    }
}
