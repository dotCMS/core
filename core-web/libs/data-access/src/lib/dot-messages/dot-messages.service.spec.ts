/* eslint-disable @typescript-eslint/no-explicit-any */

import { of } from 'rxjs';

import { HttpClient } from '@angular/common/http';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { getTestBed, TestBed } from '@angular/core/testing';

import { DotMessageService } from './dot-messages.service';

import { DotLocalstorageService } from '../dot-localstorage/dot-localstorage.service';

const DEFAULT_LANG = 'default';
const LANGUAGE_LOCALSTORAGE_KEY = 'dotMessagesKeys-lang';
const MESSAGES_LOCALSTORAGE_KEY = 'dotMessagesKeys';
const BUILDATE_LOCALSTORAGE_KEY = 'buildDate';

describe('DotMessageService', () => {
    let dotMessageService: DotMessageService;
    let http: HttpClient;
    let dotLocalstorageService: DotLocalstorageService;
    let injector: TestBed;

    const messages = {
        'dot.common.cancel': 'Cancel',
        'dot.common.accept': 'Accept {0}',
        'relativetime.future': 'a',
        'relativetime.past': 'b',
        'relativetime.s': 'c',
        'relativetime.m': 'd',
        'relativetime.mm': 'e',
        'relativetime.h': 'f',
        'relativetime.hh': 'g',
        'relativetime.d': 'h',
        'relativetime.dd': 'i',
        'relativetime.M': 'j',
        'relativetime.MM': 'k',
        'relativetime.y': 'l',
        'relativetime.yy': 'n'
    };

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [HttpClientTestingModule],
            providers: [DotMessageService, DotLocalstorageService]
        });
        injector = getTestBed();
        dotMessageService = injector.inject(DotMessageService);
        http = injector.inject(HttpClient);
        dotLocalstorageService = injector.inject(DotLocalstorageService);

        jest.spyOn<any, any>(http, 'get').mockImplementation(() => {
            return of({
                entity: messages
            });
        });
    });

    describe('init', () => {
        it('should call languages endpoint with default language and set them in local storage', () => {
            jest.spyOn(dotLocalstorageService, 'setItem');
            jest.spyOn(dotLocalstorageService, 'getItem').mockReturnValue(null);
            dotMessageService.init();
            expect(http.get).toHaveBeenCalledWith('/api/v2/languages/default/keys');
            expect(dotLocalstorageService.setItem).toHaveBeenCalledWith(
                'dotMessagesKeys',
                messages
            );
        });

        // TODO: fix the core-web.service mock
        xit('should try to load messages otherwise get the default one and set them in local storage', () => {
            jest.spyOn(dotLocalstorageService, 'setItem');
            jest.spyOn(dotLocalstorageService, 'getItem');
            dotMessageService.init();
            expect(dotLocalstorageService.getItem).toHaveBeenCalledWith('dotMessagesKeys');
            expect(http.get).toHaveBeenCalledWith('/api/v2/languages/default/keys');
        });

        it('should call languages endpoint with passed language', () => {
            dotMessageService.init({ language: 'en_US' });
            expect(http.get).toHaveBeenCalledWith('/api/v2/languages/en_US/keys');
        });

        it('should read messages from local storage', () => {
            jest.spyOn(dotLocalstorageService, 'getItem');
            dotLocalstorageService.setItem(MESSAGES_LOCALSTORAGE_KEY, messages);
            dotLocalstorageService.setItem(LANGUAGE_LOCALSTORAGE_KEY, DEFAULT_LANG);
            dotLocalstorageService.setItem(BUILDATE_LOCALSTORAGE_KEY, '2020-01-01');
            dotMessageService.init();
            expect(dotLocalstorageService.getItem).toHaveBeenCalledWith('dotMessagesKeys');
            expect(http.get).not.toHaveBeenCalled();
        });
    });

    describe('get', () => {
        beforeEach(() => {
            dotMessageService.init();
        });

        it('should return message', () => {
            const label = dotMessageService.get('dot.common.cancel');
            expect(label).toEqual('Cancel');
        });

        it('should return key if message not found', () => {
            const label = dotMessageService.get('dot.unknown');
            expect(label).toEqual('dot.unknown');
        });

        it('should replace text in message', () => {
            const label = dotMessageService.get('dot.common.accept', 'data');
            expect(label).toEqual('Accept data');
        });
    });
});
