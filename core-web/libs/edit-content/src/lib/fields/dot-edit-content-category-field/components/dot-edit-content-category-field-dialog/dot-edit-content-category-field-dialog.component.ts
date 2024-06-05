import { ChangeDetectionStrategy, Component, inject } from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { DynamicDialogRef } from 'primeng/dynamicdialog';

import { DotMessagePipe } from '@dotcms/ui';

@Component({
    selector: 'dot-edit-content-category-field-dialog',
    standalone: true,
    imports: [DialogModule, ButtonModule, DotMessagePipe],
    templateUrl: './dot-edit-content-category-field-dialog.component.html',
    styleUrl: './dot-edit-content-category-field-dialog.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotEditContentCategoryFieldDialogComponent {
    protected dialogRef: DynamicDialogRef = inject(DynamicDialogRef);
}
