import { DotMessageService } from '@services/dot-message/dot-messages.service';
import { CoreWebService } from 'dotcms-js';
import { of } from 'rxjs';
import { RequestMethod } from '@angular/http';
import { FormatDateService } from '@services/format-date-service';
import { DOTTestBed } from '@tests/dot-test-bed';
import { DotLocalstorageService } from '@services/dot-localstorage/dot-localstorage.service';

describe('DotMessageService', () => {
    let service: DotMessageService;
    let coreWebService: CoreWebService;
    let formatDateService: FormatDateService;
    let dotLocalstorageService: DotLocalstorageService;
    const MESSAGES_LOCALSTORAGE_KEY = 'dotMessagesKeys';

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
        this.injector = DOTTestBed.resolveAndCreate([
            DotMessageService,
            FormatDateService,
            DotLocalstorageService
        ]);
        service = this.injector.get(DotMessageService);
        coreWebService = this.injector.get(CoreWebService);
        formatDateService = this.injector.get(FormatDateService);
        dotLocalstorageService = this.injector.get(DotLocalstorageService);

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
            service.init(true);
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
            service.init(false);
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
            service.init(true, 'en_US');
            expect(coreWebService.requestView).toHaveBeenCalledWith({
                method: RequestMethod.Get,
                url: '/api/v2/languages/en_US/keys'
            });
        });
        it('should read messages from local storage', () => {
            spyOn(dotLocalstorageService, 'getItem').and.callThrough();
            dotLocalstorageService.setItem(MESSAGES_LOCALSTORAGE_KEY, messages);
            service.init(false);
            expect(dotLocalstorageService.getItem).toHaveBeenCalledWith('dotMessagesKeys');
            expect(coreWebService.requestView).not.toHaveBeenCalled();
        });
    });

    describe('get', () => {
        beforeEach(() => {
            service.init(true);
        });
        it('should return message', () => {
            const label = service.get('dot.common.cancel');
            expect(label).toEqual('Cancel');
        });
        it('should return key if message not found', () => {
            const label = service.get('dot.unknown');
            expect(label).toEqual('dot.unknown');
        });
        it('should replace text in message', () => {
            const label = service.get('dot.common.accept', 'data');
            expect(label).toEqual('Accept data');
        });
    });

    it('should set relative date messages', () => {
        spyOn(formatDateService, 'setLang');
        service.init(true);
        service.setRelativeDateMessages('en_US');
        expect(formatDateService.setLang).toHaveBeenCalledWith('en', relativeDateMessages);
    });
});
