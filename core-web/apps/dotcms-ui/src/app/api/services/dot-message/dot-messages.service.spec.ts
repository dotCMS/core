/* eslint-disable @typescript-eslint/no-explicit-any */

import { DotMessageService } from '@services/dot-message/dot-messages.service';
import { CoreWebService } from '@dotcms/dotcms-js';
import { of } from 'rxjs';
import { DotFormatDateService } from '@services/dot-format-date-service';
import { DotLocalstorageService } from '@services/dot-localstorage/dot-localstorage.service';
import { TestBed, getTestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { CoreWebServiceMock } from '@tests/core-web.service.mock';
import { DotFormatDateServiceMock } from '@dotcms/app/test/format-date-service.mock';

describe('DotMessageService', () => {
    let dotMessageService: DotMessageService;
    let coreWebService: CoreWebService;
    let dotLocalstorageService: DotLocalstorageService;
    const MESSAGES_LOCALSTORAGE_KEY = 'dotMessagesKeys';
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
            providers: [
                { provide: CoreWebService, useClass: CoreWebServiceMock },
                DotMessageService,
                { provide: DotFormatDateService, useClass: DotFormatDateServiceMock },
                DotLocalstorageService
            ]
        });
        injector = getTestBed();
        dotMessageService = injector.get(DotMessageService);
        coreWebService = injector.get(CoreWebService);
        dotLocalstorageService = injector.get(DotLocalstorageService);

        spyOn<any>(coreWebService, 'requestView').and.returnValue(
            of({
                entity: messages
            })
        );
    });

    describe('init', () => {
        it('should call languages endpoint with default language and set them in local storage', () => {
            spyOn(dotLocalstorageService, 'setItem');
            spyOn(dotLocalstorageService, 'getItem').and.returnValue(null);
            dotMessageService.init();
            expect(coreWebService.requestView).toHaveBeenCalledWith({
                url: '/api/v2/languages/default/keys'
            });
            expect(dotLocalstorageService.setItem).toHaveBeenCalledWith(
                'dotMessagesKeys',
                messages
            );
        });

        it('should try to load messages otherwise get the default one and set them in local storage', () => {
            spyOn(dotLocalstorageService, 'setItem');
            spyOn(dotLocalstorageService, 'getItem');
            dotMessageService.init();
            expect(dotLocalstorageService.getItem).toHaveBeenCalledWith('dotMessagesKeys');
            expect(coreWebService.requestView).toHaveBeenCalledWith({
                url: '/api/v2/languages/default/keys'
            });
            expect(dotLocalstorageService.setItem).toHaveBeenCalledWith(
                'dotMessagesKeys',
                messages
            );
        });

        it('should call languages endpoint with passed language', () => {
            dotMessageService.init({ language: 'en_US' });
            expect(coreWebService.requestView).toHaveBeenCalledWith({
                url: '/api/v2/languages/en_US/keys'
            });
        });

        it('should read messages from local storage', () => {
            spyOn(dotLocalstorageService, 'getItem').and.callThrough();
            dotLocalstorageService.setItem(MESSAGES_LOCALSTORAGE_KEY, messages);
            dotMessageService.init();
            expect(dotLocalstorageService.getItem).toHaveBeenCalledWith('dotMessagesKeys');
            expect(coreWebService.requestView).not.toHaveBeenCalled();
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
