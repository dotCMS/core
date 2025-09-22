import {
    ChangeDetectionStrategy,
    Component,
    computed,
    forwardRef,
    inject,
    input
} from '@angular/core';
import { ControlContainer, NG_VALUE_ACCESSOR, ReactiveFormsModule } from '@angular/forms';

import { RadioButtonModule } from 'primeng/radiobutton';

import { DotCMSContentTypeField } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';

import { getSingleSelectableFieldOptions } from '../../utils/functions.util';
import { DotCardFieldContentComponent } from '../dot-card-field/components/dot-card-field-content.component';
import { DotCardFieldFooterComponent } from '../dot-card-field/components/dot-card-field-footer.component';
import { DotCardFieldComponent } from '../dot-card-field/dot-card-field.component';
import { BaseFieldComponent } from '../shared/base-field.component';

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
        DotMessagePipe
    ],
    templateUrl: './dot-edit-content-radio-field.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    viewProviders: [
        {
            provide: ControlContainer,
            useFactory: () => inject(ControlContainer, { skipSelf: true })
        }
    ],
    providers: [
        {
            multi: true,
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => DotEditContentRadioFieldComponent)
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

    writeValue(_: unknown): void {
        // noop
    }
}
