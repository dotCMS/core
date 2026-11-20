/* eslint-disable @typescript-eslint/no-explicit-any */

import { firstValueFrom, of, Subject } from 'rxjs';
import { vi } from 'vitest';

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

        vi.spyOn<any, any>(http, 'get').mockImplementation(() => {
            return of({
                entity: messages
            });
        });
    });

    describe('init', () => {
        it('should call languages endpoint with default language and set them in local storage', async () => {
            vi.spyOn(dotLocalstorageService, 'setItem');
            vi.spyOn(dotLocalstorageService, 'getItem').mockReturnValue(null);
            await firstValueFrom(dotMessageService.init());
            expect(http.get).toHaveBeenCalledWith('/api/v2/languages/default/keys');
            expect(dotLocalstorageService.setItem).toHaveBeenCalledWith(
                'dotMessagesKeys',
                messages
            );
        });

        // TODO: fix the core-web.service mock
        it.skip('should try to load messages otherwise get the default one and set them in local storage', () => {
            vi.spyOn(dotLocalstorageService, 'setItem');
            vi.spyOn(dotLocalstorageService, 'getItem');
            dotMessageService.init();
            expect(dotLocalstorageService.getItem).toHaveBeenCalledWith('dotMessagesKeys');
            expect(http.get).toHaveBeenCalledWith('/api/v2/languages/default/keys');
        });

        it('should call languages endpoint with passed language', async () => {
            await firstValueFrom(dotMessageService.init({ language: 'en_US' }));
            expect(http.get).toHaveBeenCalledWith('/api/v2/languages/en_US/keys');
        });

        it('should read messages from local storage', async () => {
            vi.spyOn(dotLocalstorageService, 'getItem');
            dotLocalstorageService.setItem(MESSAGES_LOCALSTORAGE_KEY, messages);
            dotLocalstorageService.setItem(LANGUAGE_LOCALSTORAGE_KEY, DEFAULT_LANG);
            dotLocalstorageService.setItem(BUILDATE_LOCALSTORAGE_KEY, '2020-01-01');
            await firstValueFrom(dotMessageService.init());
            expect(dotLocalstorageService.getItem).toHaveBeenCalledWith('dotMessagesKeys');
            expect(http.get).not.toHaveBeenCalled();
        });
    });

    /**
     * #38303, T175: the messages arrive after the app's first screens are built (a new build date
     * fetches them again), so text built before then reads as raw keys. `ready$` tells when they
     * are there.
     */
    describe('ready$', () => {
        it('emits once the messages arrive over HTTP, and replays it to later subscribers', () => {
            const response = new Subject<{ entity: Record<string, string> }>();
            vi.spyOn<any, any>(http, 'get').mockReturnValue(response);
            vi.spyOn(dotLocalstorageService, 'getItem').mockReturnValue(null);
            const labels: string[] = [];
            dotMessageService.ready$.subscribe(() =>
                labels.push(dotMessageService.get('dot.common.cancel'))
            );

            dotMessageService.init();
            expect(labels).toEqual([]);

            response.next({ entity: messages });
            expect(labels).toEqual(['Cancel']);

            const late = vi.fn();
            dotMessageService.ready$.subscribe(late);
            expect(late).toHaveBeenCalledTimes(1);
        });

        it('emits at once when the messages come from local storage', () => {
            dotLocalstorageService.setItem(MESSAGES_LOCALSTORAGE_KEY, messages);
            dotLocalstorageService.setItem(LANGUAGE_LOCALSTORAGE_KEY, DEFAULT_LANG);
            dotLocalstorageService.setItem(BUILDATE_LOCALSTORAGE_KEY, '2020-01-01');
            const ready = vi.fn();
            dotMessageService.ready$.subscribe(ready);

            dotMessageService.init();

            expect(ready).toHaveBeenCalledTimes(1);
            expect(http.get).not.toHaveBeenCalled();
        });

        it('emits nothing before the messages are asked for', () => {
            const ready = vi.fn();

            dotMessageService.ready$.subscribe(ready);

            expect(ready).not.toHaveBeenCalled();
        });
    });

    describe('get', () => {
        beforeEach(async () => {
            await firstValueFrom(dotMessageService.init());
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
