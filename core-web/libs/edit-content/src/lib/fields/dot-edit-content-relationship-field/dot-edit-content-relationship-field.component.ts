import {
    ChangeDetectionStrategy,
    Component,
    CUSTOM_ELEMENTS_SCHEMA,
    inject,
    input
} from '@angular/core';
import { ControlContainer, ReactiveFormsModule } from '@angular/forms';

import { DotCMSContentlet, DotCMSContentTypeField } from '@dotcms/dotcms-models';

import { DotRelationshipFieldComponent } from './components/dot-relationship-field/dot-relationship-field.component';

import { DotCardFieldContentComponent } from '../dot-card-field/components/dot-card-field-content.component';
import { DotCardFieldLabelComponent } from '../dot-card-field/components/dot-card-field-label.component';
import { DotCardFieldComponent } from '../dot-card-field/dot-card-field.component';
import { BaseWrapperField } from '../shared/base-wrapper-field';

@Component({
    selector: 'dot-edit-content-relationship-field',
    imports: [
        ReactiveFormsModule,
        DotCardFieldComponent,
        DotCardFieldContentComponent,
        DotCardFieldLabelComponent,
        DotRelationshipFieldComponent
    ],
    templateUrl: './dot-edit-content-relationship-field.component.html',
    styleUrls: ['./dot-edit-content-relationship-field.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    schemas: [CUSTOM_ELEMENTS_SCHEMA],
    viewProviders: [
        {
            provide: ControlContainer,
            useFactory: () => inject(ControlContainer, { skipSelf: true })
        }
    ]
})
export class DotEditContentRelationshipFieldComponent extends BaseWrapperField {
    /**
     * DotCMS Content Type Field
     *
     * @memberof DotEditContentFileFieldComponent
     */
    $field = input.required<DotCMSContentTypeField>({ alias: 'field' });

    /**
     * DotCMS Contentlet
     *
     * @memberof DotEditContentRelationshipFieldComponent
     */
    $contentlet = input.required<DotCMSContentlet>({ alias: 'contentlet' });
}
