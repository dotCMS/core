import { ChangeDetectionStrategy, Component, computed, inject, input } from '@angular/core';
import { ControlContainer, ReactiveFormsModule } from '@angular/forms';

import { RadioButtonModule } from 'primeng/radiobutton';

import { DotCMSContentTypeField } from '@dotcms/dotcms-models';

import { getSingleSelectableFieldOptions } from '../../utils/functions.util';

import { BaseFieldComponent } from '../shared/base-field.component';

/**
 * Component to render a radio field.
 */
@Component({
    selector: 'dot-edit-content-radio-field',
    imports: [RadioButtonModule, ReactiveFormsModule],
    templateUrl: './dot-edit-content-radio-field.component.html',
    styleUrls: ['./dot-edit-content-radio-field.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    viewProviders: [
        {
            provide: ControlContainer,
            useFactory: () => inject(ControlContainer, { skipSelf: true })
        }
    ]
})
export class DotEditContentRadioFieldComponent extends BaseFieldComponent {
    /**
     * The field to render.
     */
    $field = input.required<DotCMSContentTypeField>({ alias: 'field' });

    /**
     * Returns the options for the radio field.
     */
    $options = computed(() =>
        getSingleSelectableFieldOptions(this.$field().values || '', this.$field().dataType)
    );
}
