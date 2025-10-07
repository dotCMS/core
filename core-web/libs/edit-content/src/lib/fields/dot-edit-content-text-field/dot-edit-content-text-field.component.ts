import { ChangeDetectionStrategy, Component, inject, input, computed } from '@angular/core';
import { ReactiveFormsModule, FormsModule, ControlContainer } from '@angular/forms';

import { InputTextModule } from 'primeng/inputtext';

import { DotCMSContentlet, DotCMSContentTypeField } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';

import { INPUT_TEXT_OPTIONS } from './utils';

import { DotCardFieldContentComponent } from '../dot-card-field/components/dot-card-field-content.component';
import { DotCardFieldFooterComponent } from '../dot-card-field/components/dot-card-field-footer.component';
import { DotCardFieldLabelComponent } from '../dot-card-field/components/dot-card-field-label/dot-card-field-label.component';
import { DotCardFieldComponent } from '../dot-card-field/dot-card-field.component';
import { BaseWrapperField } from '../shared/base-wrapper-field';

@Component({
    selector: 'dot-edit-content-text-field',
    templateUrl: './dot-edit-content-text-field.component.html',
    imports: [
        ReactiveFormsModule,
        FormsModule,
        DotMessagePipe,
        InputTextModule,
        DotCardFieldComponent,
        DotCardFieldContentComponent,
        DotCardFieldFooterComponent,
        DotCardFieldLabelComponent
    ],
    changeDetection: ChangeDetectionStrategy.OnPush,
    viewProviders: [
        {
            provide: ControlContainer,
            useFactory: () => inject(ControlContainer, { skipSelf: true })
        }
    ]
})
export class DotEditContentTextFieldComponent extends BaseWrapperField {
    /**
     * A signal that holds the field.
     * It is used to display the field in the text field component.
     */
    $field = input.required<DotCMSContentTypeField>({ alias: 'field' });
    /**
     * A signal that holds the contentlet.
     * It is used to display the contentlet in the text field component.
     */
    $contentlet = input.required<DotCMSContentlet>({ alias: 'contentlet' });
    /**
     * A computed signal that holds the initial value of the text field.
     * It is used to display the initial value in the text field component.
     */
    $initValue = computed(() => {
        const contentlet = this.$contentlet();
        const field = this.$field();
        const value = contentlet
            ? (contentlet[field.variable] ?? field.defaultValue)
            : field.defaultValue;

        const shouldRemoveLeadingSlash =
            contentlet?.baseType === 'HTMLPAGE' &&
            field.variable === 'url' &&
            typeof value === 'string' &&
            value.startsWith('/');

        return shouldRemoveLeadingSlash ? value.substring(1) : value;
    });
    /**
     * A readonly field that holds the input text options.
     */
    readonly inputTextOptions = INPUT_TEXT_OPTIONS;
}
