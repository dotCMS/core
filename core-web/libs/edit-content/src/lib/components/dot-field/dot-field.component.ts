import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, Input, inject } from '@angular/core';
import { ControlContainer, ReactiveFormsModule } from '@angular/forms';

import { InputTextModule } from 'primeng/inputtext';

import { DotField } from '../../interfaces/dot-form.interface';

@Component({
    selector: 'dot-field',
    standalone: true,
    imports: [CommonModule, ReactiveFormsModule, InputTextModule],
    templateUrl: './dot-field.component.html',
    styleUrls: ['./dot-field.component.scss'],
    viewProviders: [
        {
            provide: ControlContainer,
            useFactory: () => inject(ControlContainer, { skipSelf: true })
        }
    ],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotFieldComponent {
    @Input() field!: DotField;
}
