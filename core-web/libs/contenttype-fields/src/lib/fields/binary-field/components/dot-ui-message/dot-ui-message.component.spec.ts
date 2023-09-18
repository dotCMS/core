import { SpectatorHost, createHostFactory } from '@ngneat/spectator';

import { CommonModule } from '@angular/common';

import { DotUiMessageComponent } from './dot-ui-message.component';

describe('DotUiMessageComponent', () => {
    let spectator: SpectatorHost<DotUiMessageComponent>;

    const createHost = createHostFactory({
        component: DotUiMessageComponent,
        imports: [CommonModule],
        providers: []
    });

    beforeEach(async () => {
        spectator = createHost(`<dot-ui-message></dot-ui-message>`, {
            hostProps: {
                message: 'Drag and Drop File',
                icon: 'icon',
                severity: 'severity'
            }
        });
        spectator.detectChanges();
    });

    it('should create', () => {
        expect(spectator.component).toBeTruthy();
    });
});
