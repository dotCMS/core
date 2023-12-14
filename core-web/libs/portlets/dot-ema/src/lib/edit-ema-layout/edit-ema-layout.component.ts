import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject } from '@angular/core';

import { TemplateBuilderModule } from '@dotcms/template-builder';

import { EditEmaStore } from '../dot-ema-shell/store/dot-ema.store';

@Component({
    selector: 'dot-edit-ema-layout',
    standalone: true,
    imports: [CommonModule, TemplateBuilderModule],
    templateUrl: './edit-ema-layout.component.html',
    styleUrls: ['./edit-ema-layout.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class EditEmaLayoutComponent {
    private readonly store = inject(EditEmaStore);
    readonly layout$ = this.store.layout$;
}
