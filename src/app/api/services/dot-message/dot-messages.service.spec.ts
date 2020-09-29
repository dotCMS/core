import { DotMessageService } from '@services/dot-message/dot-messages.service';
import { CoreWebService } from 'dotcms-js';
import { of } from 'rxjs';
import { RequestMethod } from '@angular/http';
import { FormatDateService } from '@services/format-date-service';
import { DotLocalstorageService } from '@services/dot-localstorage/dot-localstorage.service';
import { TestBed, getTestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { CoreWebServiceMock } from 'projects/dotcms-js/src/lib/core/core-web.service.mock';

describe('DotMessageService', () => {
    let dotMessageService: DotMessageService;
    let coreWebService: CoreWebService;
    let formatDateService: FormatDateService;
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

    const relativeDateMessages = {
        future: 'a',
        past: 'b',
        s: 'c',
        m: 'd',
        mm: 'e',
        h: 'f',
        hh: 'g',
        d: 'h',
        dd: 'i',
        M: 'j',
        MM: 'k',
        y: 'l',
        yy: 'n'
    };

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [HttpClientTestingModule],
            providers: [
                { provide: CoreWebService, useClass: CoreWebServiceMock },
                DotMessageService,
                FormatDateService,
                DotLocalstorageService
            ]
        });
        injector = getTestBed();
        dotMessageService = injector.get(DotMessageService);
        coreWebService = injector.get(CoreWebService);
        formatDateService = injector.get(FormatDateService);
        dotLocalstorageService = injector.get(DotLocalstorageService);

        spyOn(coreWebService, 'requestView').and.returnValue(
            of({
                entity: messages
            })
        );
    });

    describe('init', () => {
        it('should call languages endpoint with default language and set them in local storage', () => {
            spyOn(dotLocalstorageService, 'setItem');
            spyOn(dotLocalstorageService, 'getItem');
            dotMessageService.init(true);
            expect(dotLocalstorageService.getItem).not.toHaveBeenCalled();
            expect(coreWebService.requestView).toHaveBeenCalledWith({
                method: RequestMethod.Get,
                url: '/api/v2/languages/default/keys'
            });
            expect(dotLocalstorageService.setItem).toHaveBeenCalledWith(
                'dotMessagesKeys',
                messages
            );
        });

        it('should try to laod mesasges otherwise get the default one and set them in local storage', () => {
            spyOn(dotLocalstorageService, 'setItem');
            spyOn(dotLocalstorageService, 'getItem');
            dotMessageService.init(false);
            expect(dotLocalstorageService.getItem).toHaveBeenCalledWith('dotMessagesKeys');
            expect(coreWebService.requestView).toHaveBeenCalledWith({
                method: RequestMethod.Get,
                url: '/api/v2/languages/default/keys'
            });
            expect(dotLocalstorageService.setItem).toHaveBeenCalledWith(
                'dotMessagesKeys',
                messages
            );
        });

        it('should call languages endpoint with passed language', () => {
            dotMessageService.init(true, 'en_US');
            expect(coreWebService.requestView).toHaveBeenCalledWith({
                method: RequestMethod.Get,
                url: '/api/v2/languages/en_US/keys'
            });
        });

        it('should read messages from local storage', () => {
            spyOn(dotLocalstorageService, 'getItem').and.callThrough();
            dotLocalstorageService.setItem(MESSAGES_LOCALSTORAGE_KEY, messages);
            dotMessageService.init(false);
            expect(dotLocalstorageService.getItem).toHaveBeenCalledWith('dotMessagesKeys');
            expect(coreWebService.requestView).not.toHaveBeenCalled();
        });
    });

    describe('get', () => {
        beforeEach(() => {
            dotMessageService.init(true);
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

    it('should set relative date messages', () => {
        spyOn(formatDateService, 'setLang');
        dotMessageService.init(true);
        dotMessageService.setRelativeDateMessages('en_US');
        expect(formatDateService.setLang).toHaveBeenCalledWith('en', relativeDateMessages);
    });
});
