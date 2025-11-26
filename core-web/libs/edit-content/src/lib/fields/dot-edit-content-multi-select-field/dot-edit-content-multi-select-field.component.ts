import { ChangeDetectionStrategy, Component, computed, inject, input } from '@angular/core';
import { ControlContainer, ReactiveFormsModule } from '@angular/forms';

import { MultiSelectModule } from 'primeng/multiselect';

import { DotCMSContentlet, DotCMSContentTypeField } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';

import { getSingleSelectableFieldOptions } from '../../utils/functions.util';
import { DotCardFieldContentComponent } from '../dot-card-field/components/dot-card-field-content.component';
import { DotCardFieldFooterComponent } from '../dot-card-field/components/dot-card-field-footer.component';
import { DotCardFieldLabelComponent } from '../dot-card-field/components/dot-card-field-label/dot-card-field-label.component';
import { DotCardFieldComponent } from '../dot-card-field/dot-card-field.component';
import { BaseWrapperField } from '../shared/base-wrapper-field';
@Component({
    selector: 'dot-edit-content-multi-select-field',
    imports: [
        MultiSelectModule,
        ReactiveFormsModule,
        DotCardFieldComponent,
        DotCardFieldContentComponent,
        DotCardFieldFooterComponent,
        DotCardFieldLabelComponent,
        DotMessagePipe
    ],
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './dot-edit-content-multi-select-field.component.html',
    viewProviders: [
        {
            provide: ControlContainer,
            useFactory: () => inject(ControlContainer, { skipSelf: true })
        }
    ]
})
export class DotEditContentMultiSelectFieldComponent extends BaseWrapperField {
    /**
     * Input field DotCMSContentTypeField
     */
    $field = input.required<DotCMSContentTypeField>({ alias: 'field' });
    /**
     * Input contentlet DotCMSContentlet
     */
    $contentlet = input.required<DotCMSContentlet>({ alias: 'contentlet' });
    /**
     * Computed signal that holds the options for the multi select field.
     */
    $options = computed(() => {
        const field = this.$field();

        return getSingleSelectableFieldOptions(field.values || '', field.dataType);
    });
}
