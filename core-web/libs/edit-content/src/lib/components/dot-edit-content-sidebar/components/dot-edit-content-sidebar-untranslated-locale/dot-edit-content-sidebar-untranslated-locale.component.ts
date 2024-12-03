import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component } from '@angular/core';

import { RadioButtonModule } from 'primeng/radiobutton';

@Component({
    selector: 'dot-edit-content-sidebar-untranslated-locale',
    standalone: true,
    imports: [CommonModule, RadioButtonModule],
    templateUrl: './dot-edit-content-sidebar-untranslated-locale.component.html',
    styleUrl: './dot-edit-content-sidebar-untranslated-locale.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotEditContentSidebarUntranslatedLocaleComponent {}
