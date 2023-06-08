import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component } from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { DividerModule } from 'primeng/divider';

import { DotMessagePipeModule } from '@dotcms/ui';

@Component({
    selector: 'dotcms-template-builder-actions',
    standalone: true,
    imports: [CommonModule, ButtonModule, DividerModule, DotMessagePipeModule],
    templateUrl: './template-builder-actions.component.html',
    styleUrls: ['./template-builder-actions.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class TemplateBuilderActionsComponent {}
