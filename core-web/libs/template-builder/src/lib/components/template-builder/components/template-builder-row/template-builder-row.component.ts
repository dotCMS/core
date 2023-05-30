import { ChangeDetectionStrategy, Component, EventEmitter, Output } from '@angular/core';

import { ButtonModule } from 'primeng/button';

import { DotIconModule } from '@dotcms/ui';

@Component({
    selector: 'dotcms-template-builder-row',
    standalone: true,
    imports: [DotIconModule, ButtonModule],
    templateUrl: './template-builder-row.component.html',
    styleUrls: ['./template-builder-row.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class TemplateBuilderRowComponent {
    @Output()
    editStyleClasses: EventEmitter<void> = new EventEmitter<void>();
    @Output()
    deleteRow: EventEmitter<void> = new EventEmitter<void>();
}
