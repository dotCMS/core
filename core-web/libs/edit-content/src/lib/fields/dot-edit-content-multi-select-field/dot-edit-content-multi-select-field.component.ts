import {
    ChangeDetectionStrategy,
    Component,
    inject,
    Input,
    OnChanges,
    SimpleChanges
} from '@angular/core';
import { ControlContainer, ReactiveFormsModule } from '@angular/forms';

import { MultiSelectModule } from 'primeng/multiselect';

import { DotCMSContentTypeField } from '@dotcms/dotcms-models';

import { getSingleSelectableFieldOptions } from '../../utils/functions.util';

@Component({
    selector: 'dot-edit-content-multi-select-field',
    standalone: true,
    imports: [MultiSelectModule, ReactiveFormsModule],
    templateUrl: './dot-edit-content-multi-select-field.component.html',
    styleUrls: ['./dot-edit-content-multi-select-field.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    viewProviders: [
        {
            provide: ControlContainer,
            useFactory: () => inject(ControlContainer, { skipSelf: true })
        }
    ]
})
export class DotEditContentMultiSelectFieldComponent implements OnChanges {
    @Input() field!: DotCMSContentTypeField;
    options = [];

    ngOnChanges(changes: SimpleChanges) {
        if (changes.field && changes.field.currentValue) {
            const { values, dataType } = changes.field.currentValue;
            this.options = getSingleSelectableFieldOptions(values || '', dataType);
        }
    }
}
