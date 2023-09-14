import { SpectatorHost, createHostFactory } from '@ngneat/spectator';

import { CommonModule } from '@angular/common';
import { DomSanitizer } from '@angular/platform-browser';

import { DotMessageService } from '@dotcms/data-access';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotDropZoneMessageComponent } from './dot-drop-zone-message.component';

export const MESSAGES_MOCK = {
    'dot.test.action.example.action.choose.file': '<a data-id="choose-file">Choose File</a>'
};

export const DOT_MESSAGE_SERVICE_TB_MOCK = new MockDotMessageService(MESSAGES_MOCK);

describe('DotDropZoneMessageComponent', () => {
    let spectator: SpectatorHost<DotDropZoneMessageComponent>;

    const createHost = createHostFactory({
        component: DotDropZoneMessageComponent,
        imports: [CommonModule],
        providers: [
            {
                provide: DomSanitizer,
                useValue: {
                    bypassSecurityTrustHtml: (val: string) => val
                }
            },
            {
                provide: DotMessageService,
                useValue: DOT_MESSAGE_SERVICE_TB_MOCK
            }
        ]
    });

    beforeEach(async () => {
        spectator = createHost(
            `<dot-drop-zone-message></dot-drop-zone-message>`,

            {
                hostProps: {
                    message: 'dot.test.action.example.action.choose.file',
                    icon: 'icon',
                    severity: 'severity',
                    messageArgs: ['messageArgs']
                }
            }
        );
        spectator.detectChanges();
    });

    it('should create', () => {
        expect(spectator.component).toBeTruthy();
    });
});
