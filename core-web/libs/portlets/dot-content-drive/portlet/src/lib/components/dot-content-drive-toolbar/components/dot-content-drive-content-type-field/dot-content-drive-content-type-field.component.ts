import { ChangeDetectionStrategy, Component } from '@angular/core';

import { MultiSelectModule } from 'primeng/multiselect';

@Component({
    selector: 'dot-content-drive-content-type-field',
    imports: [MultiSelectModule],
    templateUrl: './dot-content-drive-content-type-field.component.html',
    styleUrl: './dot-content-drive-content-type-field.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotContentDriveContentTypeFieldComponent {}
