import { signalMethod } from '@ngrx/signals';

import { ChangeDetectionStrategy, Component, inject, input, computed } from '@angular/core';
import { ReactiveFormsModule, FormsModule, ControlContainer } from '@angular/forms';

import { DotCMSContentTypeField, DotCMSContentlet } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';

import { DotTextFieldComponent } from './components/text-field/text-field.component';

import { DotCardFieldContentComponent } from '../dot-card-field/components/dot-card-field-content.component';
import { DotCardFieldFooterComponent } from '../dot-card-field/components/dot-card-field-footer.component';
import { DotCardFieldComponent } from '../dot-card-field/dot-card-field.component';
import { BaseWrapperFieldComponent } from '../shared/base-wrapper-field.component';

@Component({
    selector: 'dot-edit-content-text-field',
    templateUrl: './dot-edit-content-text-field.component.html',
    styleUrls: ['./dot-edit-content-text-field.component.scss'],
    imports: [
        ReactiveFormsModule,
        FormsModule,
        DotMessagePipe,
        DotTextFieldComponent,
        DotCardFieldComponent,
        DotCardFieldContentComponent,
        DotCardFieldFooterComponent
    ],
    changeDetection: ChangeDetectionStrategy.OnPush,
    viewProviders: [
        {
            provide: ControlContainer,
            useFactory: () => inject(ControlContainer, { skipSelf: true })
        }
    ]
})
export class DotEditContentTextFieldComponent extends BaseWrapperFieldComponent {
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
}
