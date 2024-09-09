import { ChangeDetectionStrategy, Component } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
    selector: 'dot-uve-edit-style-form',
    standalone: true,
    imports: [CommonModule],
    templateUrl: './uve-edit-style-form.component.html',
    styleUrl: './uve-edit-style-form.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class UveEditStyleFormComponent {}
