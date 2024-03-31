import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, Input } from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { MenuModule } from 'primeng/menu';
import { ToolbarModule } from 'primeng/toolbar';

import { EditEmaStoreStateVM } from '../../../dot-ema-shell/store/dot-ema.store';
import { EDITOR_MODE } from '../../../shared/enums';

@Component({
    selector: 'dot-edit-ema-toolbar',
    standalone: true,
    imports: [CommonModule, MenuModule, ButtonModule, ToolbarModule],
    templateUrl: './edit-ema-toolbar.component.html',
    styleUrls: ['./edit-ema-toolbar.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class EditEmaToolbarComponent {
    @Input() es: EditEmaStoreStateVM;
    readonly editorMode = EDITOR_MODE;
}
