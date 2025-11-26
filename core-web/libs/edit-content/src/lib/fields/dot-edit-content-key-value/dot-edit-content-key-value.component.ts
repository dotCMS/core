import { Component, ChangeDetectionStrategy, input, inject } from '@angular/core';
import { ControlContainer, ReactiveFormsModule } from '@angular/forms';

import { DotCMSContentlet, DotCMSContentTypeField } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';

import { DotKeyValueFieldComponent } from './components/key-value-field/key-value-field.component';

import { DotCardFieldContentComponent } from '../dot-card-field/components/dot-card-field-content.component';
import { DotCardFieldFooterComponent } from '../dot-card-field/components/dot-card-field-footer.component';
import { DotCardFieldLabelComponent } from '../dot-card-field/components/dot-card-field-label/dot-card-field-label.component';
import { DotCardFieldComponent } from '../dot-card-field/dot-card-field.component';
import { BaseWrapperField } from '../shared/base-wrapper-field';

@Component({
    selector: 'dot-edit-content-key-value',
    imports: [
        ReactiveFormsModule,
        DotCardFieldComponent,
        DotKeyValueFieldComponent,
        DotCardFieldContentComponent,
        DotCardFieldFooterComponent,
        DotCardFieldLabelComponent,
        DotMessagePipe
    ],
    templateUrl: './dot-edit-content-key-value.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    viewProviders: [
        {
            provide: ControlContainer,
            useFactory: () => inject(ControlContainer, { skipSelf: true })
        }
    ]
})
export class DotEditContentKeyValueComponent extends BaseWrapperField {
    /**
     * A signal that holds the field.
     * It is used to display the field in the key value field component.
     */
    $field = input.required<DotCMSContentTypeField>({ alias: 'field' });
    /**
     * A signal that holds the contentlet.
     * It is used to display the contentlet in the key value field component.
     */
    $contentlet = input.required<DotCMSContentlet>({ alias: 'contentlet' });
}
