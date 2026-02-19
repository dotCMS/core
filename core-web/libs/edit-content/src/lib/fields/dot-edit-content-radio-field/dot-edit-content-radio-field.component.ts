import { ChangeDetectionStrategy, Component, computed, inject, input } from '@angular/core';
import { ControlContainer, ReactiveFormsModule } from '@angular/forms';

import { RadioButtonModule } from 'primeng/radiobutton';

import { DotCMSContentlet, DotCMSContentTypeField } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';

import { getSingleSelectableFieldOptions } from '../../utils/functions.util';
import { DotCardFieldContentComponent } from '../dot-card-field/components/dot-card-field-content.component';
import { DotCardFieldFooterComponent } from '../dot-card-field/components/dot-card-field-footer.component';
import { DotCardFieldLabelComponent } from '../dot-card-field/components/dot-card-field-label/dot-card-field-label.component';
import { DotCardFieldComponent } from '../dot-card-field/dot-card-field.component';
import { BaseWrapperField } from '../shared/base-wrapper-field';

/**
 * Component to render a radio field.
 */
@Component({
    selector: 'dot-edit-content-radio-field',
    imports: [
        RadioButtonModule,
        ReactiveFormsModule,
        DotCardFieldComponent,
        DotCardFieldContentComponent,
        DotCardFieldFooterComponent,
        DotCardFieldLabelComponent,
        DotMessagePipe
    ],
    templateUrl: './dot-edit-content-radio-field.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    viewProviders: [
        {
            provide: ControlContainer,
            useFactory: () => inject(ControlContainer, { skipSelf: true })
        }
    ]
})
export class DotEditContentRadioFieldComponent extends BaseWrapperField {
    /**
     * The field to render.
     */
    $field = input.required<DotCMSContentTypeField>({ alias: 'field' });

    /**
     * A signal that holds the contentlet.
     * It is used to display the contentlet in the component.
     */
    $contentlet = input.required<DotCMSContentlet>({ alias: 'contentlet' });

    /**
     * Returns the options for the radio field.
     */
    $options = computed(() =>
        getSingleSelectableFieldOptions(this.$field().values || '', this.$field().dataType)
    );
}
