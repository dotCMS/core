import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { ButtonDirective } from 'primeng/button';
import { DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';
import { RadioButtonModule } from 'primeng/radiobutton';

import { DotIsoCodePipe, DotMessagePipe } from '@dotcms/ui';

@Component({
    selector: 'dot-edit-content-sidebar-untranslated-locale',
    imports: [RadioButtonModule, DotMessagePipe, FormsModule, ButtonDirective, DotIsoCodePipe],
    templateUrl: './dot-edit-content-sidebar-untranslated-locale.component.html',
    styleUrl: './dot-edit-content-sidebar-untranslated-locale.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotEditContentSidebarUntranslatedLocaleComponent {
    selectedOption = 'populate';

    dialogRef = inject(DynamicDialogRef);
    config = inject(DynamicDialogConfig);
}
