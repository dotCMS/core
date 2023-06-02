import { ChangeDetectionStrategy, Component, EventEmitter, Output } from '@angular/core';

import { ButtonModule } from 'primeng/button';

import { RemoveRowComponent } from '../remove-row/remove-row.component';

@Component({
    selector: 'dotcms-template-builder-row',
    standalone: true,
    templateUrl: './template-builder-row.component.html',
    styleUrls: ['./template-builder-row.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    imports: [ButtonModule, RemoveRowComponent]
})
export class TemplateBuilderRowComponent {
    @Output()
    editStyleClasses: EventEmitter<void> = new EventEmitter<void>();
    @Output()
    deleteRow: EventEmitter<void> = new EventEmitter<void>();
}
