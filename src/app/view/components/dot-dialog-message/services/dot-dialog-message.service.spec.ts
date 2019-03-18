import { DotDialogMessageService } from './dot-dialog-message.service';
import { DotcmsEventsServiceMock } from '@tests/dotcms-events-service.mock';
import { DOTTestBed } from '@tests/dot-test-bed';
import { DotcmsEventsService } from 'dotcms-js';

describe('DotDialogMessageService', () => {
    const mockDotcmsEventsService: DotcmsEventsServiceMock = new DotcmsEventsServiceMock();
    let dotDialogMessageService;

    const message: any = {
        data: {
            code: { lang: 'eng', content: 'Code Test' },
            width: '100',
            body: 'Body Test',
            title: 'testTitle',
            lang: 'eng',
            height: '200'
        }
    };

    beforeEach(() => {
        const injector = DOTTestBed.resolveAndCreate([
            { provide: DotcmsEventsService, useValue: mockDotcmsEventsService },
            DotDialogMessageService
        ]);

        dotDialogMessageService = injector.get(DotDialogMessageService);
    });

    it('should emit a message', (done) => {
        mockDotcmsEventsService.triggerSubscribeTo('LARGE_MESSAGE', message);
        dotDialogMessageService.sub().subscribe((msg) => {
            const { code, width, body, title, lang, height } = message.data;
            const emittedMsg = {
                title,
                height,
                width,
                body,
                code: { lang, content: code }
            };

            expect(msg).toEqual(emittedMsg);
            done();
        });
    });
});
