import { SpectatorHost, createHostFactory } from '@ngneat/spectator';

import { CommonModule } from '@angular/common';

import { DotDropZoneComponent } from './dot-drop-zone.component';

describe('DotDropZoneComponent', () => {
    let spectator: SpectatorHost<DotDropZoneComponent>;

    const createHost = createHostFactory({
        component: DotDropZoneComponent,
        imports: [CommonModule]
    });

    beforeEach(async () => {
        spectator = createHost(`
            <dot-drop-zone>
                <div id="dot-drop-zone__content" class="dot-drop-zone__content">
                    Content
                </div>
            </dot-drop-zone>
        `);

        spectator.detectChanges();
    });

    it('should create', () => {
        expect(spectator.component).toBeTruthy();
    });

    it('should have content', () => {
        expect(spectator.query('#dot-drop-zone__content')).toBeTruthy();
    });
});
