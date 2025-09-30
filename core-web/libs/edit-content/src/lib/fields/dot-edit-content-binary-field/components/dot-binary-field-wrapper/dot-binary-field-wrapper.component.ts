import { ChangeDetectionStrategy, Component, inject, input, output } from '@angular/core';
import { ControlContainer, ReactiveFormsModule } from '@angular/forms';

import { DotCMSContentlet, DotCMSContentTypeField } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';

import { DotCardFieldContentComponent } from '../../../dot-card-field/components/dot-card-field-content.component';
import { DotCardFieldFooterComponent } from '../../../dot-card-field/components/dot-card-field-footer.component';
import { DotCardFieldLabelComponent } from '../../../dot-card-field/components/dot-card-field-label.component';
import { DotCardFieldComponent } from '../../../dot-card-field/dot-card-field.component';
import { BaseWrapperField } from '../../../shared/base-wrapper-field';
import { DotEditContentBinaryFieldComponent } from '../../dot-edit-content-binary-field.component';

/**
 * JSON field editor component that uses Monaco Editor for JSON content editing.
 * Uses DotEditContentMonacoEditorControl for editor functionality with JSON language forced.
 * Supports language variable insertion through DotLanguageVariableSelectorComponent.
 */
@Component({
    selector: 'dot-binary-field-wrapper',
    imports: [
        ReactiveFormsModule,
        DotCardFieldComponent,
        DotCardFieldContentComponent,
        DotCardFieldFooterComponent,
        DotCardFieldLabelComponent,
        DotMessagePipe,
        DotEditContentBinaryFieldComponent
    ],
    templateUrl: './dot-binary-field-wrapper.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    viewProviders: [
        {
            provide: ControlContainer,
            useFactory: () => inject(ControlContainer, { skipSelf: true, optional: true })
        }
    ]
})
export class DotBinaryFieldWrapperComponent extends BaseWrapperField {
    /**
     * A signal that holds the field.
     * It is used to display the field in the binary field wrapper component.
     */
    $field = input.required<DotCMSContentTypeField>({
        alias: 'field'
    });
    /**
     * A signal that holds the contentlet.
     * It is used to display the contentlet in the binary field wrapper component.
     */
    $contentlet = input.required<DotCMSContentlet>({
        alias: 'contentlet'
    });
    /**
     * An output signal that emits when the value is updated.
     * It is used to display the value in the binary field wrapper component.
     */
    valueUpdated = output<{ value: string; fileName: string }>();
}
