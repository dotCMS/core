import { ChangeDetectionStrategy, Component, forwardRef, inject, input } from '@angular/core';
import { ControlContainer, NG_VALUE_ACCESSOR, ReactiveFormsModule } from '@angular/forms';

import { BlockEditorModule } from '@dotcms/block-editor';
import { DotCMSContentlet, DotCMSContentTypeField } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';

import { DotCardFieldContentComponent } from '../dot-card-field/components/dot-card-field-content.component';
import { DotCardFieldFooterComponent } from '../dot-card-field/components/dot-card-field-footer.component';
import { DotCardFieldComponent } from '../dot-card-field/dot-card-field.component';
import { BaseFieldComponent } from '../shared/base-field.component';

@Component({
    selector: 'dot-edit-content-block-editor',
    imports: [
        ReactiveFormsModule,
        DotCardFieldComponent,
        DotCardFieldContentComponent,
        DotCardFieldFooterComponent,
        DotMessagePipe,
        BlockEditorModule
    ],
    templateUrl: './dot-edit-content-block-editor.component.html',
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
            useExisting: forwardRef(() => DotEditContentBlockEditorComponent)
        }
    ]
})
export class DotEditContentBlockEditorComponent extends BaseFieldComponent {
    $field = input.required<DotCMSContentTypeField>({
        alias: 'field'
    });
    $contentlet = input.required<DotCMSContentlet>({
        alias: 'contentlet'
    });

    writeValue(_: unknown): void {
        // Do nothing
    }
}
