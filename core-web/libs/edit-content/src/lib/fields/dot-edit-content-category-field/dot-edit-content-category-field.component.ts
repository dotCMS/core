import { ChangeDetectionStrategy, Component, inject, input } from '@angular/core';
import { ControlContainer, ReactiveFormsModule } from '@angular/forms';

import { ButtonModule } from 'primeng/button';

import { DotCMSContentlet, DotCMSContentTypeField } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';

import { DotCategoryFieldComponent } from './components/dot-category-field/dot-category-field.component';

import { DotCardFieldContentComponent } from '../dot-card-field/components/dot-card-field-content.component';
import { DotCardFieldFooterComponent } from '../dot-card-field/components/dot-card-field-footer.component';
import { DotCardFieldLabelComponent } from '../dot-card-field/components/dot-card-field-label/dot-card-field-label.component';
import { DotCardFieldComponent } from '../dot-card-field/dot-card-field.component';
import { BaseWrapperField } from '../shared/base-wrapper-field';

/**
 * @class
 * @name DotEditContentCategoryFieldComponent
 * @description Angular component for editing a content category field.
 *
 * The `DotEditContentCategoryFieldComponent` component provides functionality for editing a content category field.
 * It is responsible for handling user interactions and updating the state of the component.
 */
@Component({
    selector: 'dot-edit-content-category-field',
    imports: [
        ReactiveFormsModule,
        ButtonModule,
        DotMessagePipe,
        DotCardFieldComponent,
        DotCardFieldContentComponent,
        DotCardFieldFooterComponent,
        DotCardFieldLabelComponent,
        DotCategoryFieldComponent,
        DotMessagePipe
    ],
    templateUrl: './dot-edit-content-category-field.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    viewProviders: [
        {
            provide: ControlContainer,
            useFactory: () => inject(ControlContainer, { skipSelf: true })
        }
    ]
})
export class DotEditContentCategoryFieldComponent extends BaseWrapperField {
    /**
     * The `field` variable is of type `DotCMSContentTypeField` and is a required input.
     * @description The variable represents a field of a DotCMS content type and is a required input.
     */
    $field = input.required<DotCMSContentTypeField>({ alias: 'field' });
    /**
     * Represents a DotCMS contentlet and is a required input
     * @description DotCMSContentlet input representing a DotCMS contentlet.
     */
    $contentlet = input.required<DotCMSContentlet>({ alias: 'contentlet' });
}
