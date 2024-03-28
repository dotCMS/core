import { ChangeDetectionStrategy, Component, Input, OnInit, inject } from '@angular/core';
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
export class DotEditContentMultiSelectFieldComponent implements OnInit {
    @Input() field!: DotCMSContentTypeField;
    options = [];

    ngOnInit() {
        this.options = getSingleSelectableFieldOptions(
            this.field.values || '',
            this.field.dataType
        );
    }
}
