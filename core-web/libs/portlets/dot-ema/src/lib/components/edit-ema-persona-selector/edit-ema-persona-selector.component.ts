import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component } from '@angular/core';

import { AvatarModule } from 'primeng/avatar';
import { ButtonModule } from 'primeng/button';
import { OverlayPanelModule } from 'primeng/overlaypanel';

import { DotAvatarDirective, DotMessagePipe } from '@dotcms/ui';

@Component({
    selector: 'dot-edit-ema-persona-selector',
    standalone: true,
    imports: [
        CommonModule,
        ButtonModule,
        AvatarModule,
        OverlayPanelModule,
        DotAvatarDirective,
        DotMessagePipe
    ],
    templateUrl: './edit-ema-persona-selector.component.html',
    styleUrls: ['./edit-ema-persona-selector.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class EditEmaPersonaSelectorComponent {}
