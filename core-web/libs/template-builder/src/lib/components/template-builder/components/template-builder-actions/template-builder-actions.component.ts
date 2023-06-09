import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, EventEmitter, Output } from '@angular/core';

import { ButtonModule } from 'primeng/button';

import { DotMessagePipeModule } from '@dotcms/ui';

@Component({
    selector: 'dotcms-template-builder-actions',
    standalone: true,
    imports: [CommonModule, ButtonModule, DotMessagePipeModule],
    templateUrl: './template-builder-actions.component.html',
    styleUrls: ['./template-builder-actions.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class TemplateBuilderActionsComponent {
    @Output() selectLayout: EventEmitter<void> = new EventEmitter();
    @Output() selectStyles: EventEmitter<void> = new EventEmitter();
}
