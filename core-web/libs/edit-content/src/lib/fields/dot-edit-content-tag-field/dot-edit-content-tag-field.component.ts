import { ChangeDetectionStrategy, Component, inject, input } from '@angular/core';
import { ControlContainer, FormsModule, ReactiveFormsModule } from '@angular/forms';

import { AutoCompleteModule } from 'primeng/autocomplete';

import { DotCMSContentlet, DotCMSContentTypeField } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';

import { DotTagFieldComponent } from './components/tag-field/tag-field.component';

import { DotCardFieldContentComponent } from '../dot-card-field/components/dot-card-field-content.component';
import { DotCardFieldFooterComponent } from '../dot-card-field/components/dot-card-field-footer.component';
import { DotCardFieldLabelComponent } from '../dot-card-field/components/dot-card-field-label/dot-card-field-label.component';
import { DotCardFieldComponent } from '../dot-card-field/dot-card-field.component';
import { BaseWrapperField } from '../shared/base-wrapper-field';

/**
 * Component that handles tag field input using PrimeNG's AutoComplete.
 * It provides tag suggestions as the user types with a minimum of 2 characters.
 * Implements ControlValueAccessor for seamless form integration.
 */
@Component({
    selector: 'dot-edit-content-tag-field',
    imports: [
        AutoCompleteModule,
        FormsModule,
        ReactiveFormsModule,
        DotCardFieldComponent,
        DotCardFieldContentComponent,
        DotCardFieldFooterComponent,
        DotCardFieldLabelComponent,
        DotTagFieldComponent,
        DotMessagePipe
    ],
    templateUrl: './dot-edit-content-tag-field.component.html',
    styleUrl: './dot-edit-content-tag-field.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush,
    viewProviders: [
        {
            provide: ControlContainer,
            useFactory: () => inject(ControlContainer, { skipSelf: true, optional: true })
        }
    ]
})
export class DotEditContentTagFieldComponent extends BaseWrapperField {
    /**
     * Required input that defines the field configuration
     */
    $field = input.required<DotCMSContentTypeField>({ alias: 'field' });

    /**
     * Required input that defines the contentlet
     */
    $contentlet = input.required<DotCMSContentlet>({ alias: 'contentlet' });
}
