import { DotLargeMessageDisplayService } from './dot-large-message-display.service';
import { DotcmsEventsServiceMock } from '@tests/dotcms-events-service.mock';
import { DOTTestBed } from '@tests/dot-test-bed';
import { DotcmsEventsService, DotEventData } from 'dotcms-js';

describe('DotLargeMessageDisplayService', () => {
    const mockDotcmsEventsService: DotcmsEventsServiceMock = new DotcmsEventsServiceMock();
    let dotLargeMessageDisplayService;

    const message: any = {
        data: {
            code: { lang: 'eng', content: 'Code Test' },
            width: '100',
            body: 'Body Test',
            title: 'testTitle',
            height: '200'
        }
    };

    beforeEach(() => {
        const injector = DOTTestBed.resolveAndCreate([
            { provide: DotcmsEventsService, useValue: mockDotcmsEventsService },
            DotLargeMessageDisplayService
        ]);
        dotLargeMessageDisplayService = injector.get(DotLargeMessageDisplayService);
    });

    it('should emit a message', (done) => {
        dotLargeMessageDisplayService.sub().subscribe((msg: DotEventData) => {
            expect(msg).toEqual(message.data);
            done();
        });
        mockDotcmsEventsService.triggerSubscribeTo('LARGE_MESSAGE', message);
    });

    it('should clear message content', () => {
        dotLargeMessageDisplayService.sub().subscribe((msg: DotEventData) => {
            expect(msg).toEqual(null);
        });
        dotLargeMessageDisplayService.clear();
    });

});
