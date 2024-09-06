import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject } from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';
import { InputTextModule } from 'primeng/inputtext';

import { DotLanguage } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';

export interface DotLocaleConfirmationDialogData {
    acceptLabel: string;
    icon: string;
    ISOCode: string;
    locale: DotLanguage;
    message: string;
}

@Component({
    selector: 'dot-locale-confirmation-dialog',
    standalone: true,
    imports: [CommonModule, DialogModule, ButtonModule, DotMessagePipe, InputTextModule],
    templateUrl: './DotLocaleConfirmationDialog.component.html',
    styleUrl: './DotLocaleConfirmationDialog.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotLocaleConfirmationDialogComponent {
    readonly config: DynamicDialogConfig<DotLocaleConfirmationDialogData> =
        inject(DynamicDialogConfig);
    ref = inject(DynamicDialogRef);

    data: DotLocaleConfirmationDialogData = this.config.data || {
        acceptLabel: '',
        icon: '',
        ISOCode: '',
        locale: {} as DotLanguage,
        message: ''
    };
}
