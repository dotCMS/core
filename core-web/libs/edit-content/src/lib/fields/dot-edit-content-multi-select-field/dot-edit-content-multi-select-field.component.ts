import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component } from '@angular/core';

@Component({
    selector: 'dot-edit-content-multi-select-field',
    standalone: true,
    imports: [CommonModule],
    templateUrl: './dot-edit-content-multi-select-field.component.html',
    styleUrls: ['./dot-edit-content-multi-select-field.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotEditContentMultiSelectFieldComponent {}
