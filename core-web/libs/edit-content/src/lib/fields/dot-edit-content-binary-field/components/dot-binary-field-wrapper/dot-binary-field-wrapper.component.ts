import {
    ChangeDetectionStrategy,
    Component,
    input,
    output,
    OnInit,
    forwardRef,
    inject
} from '@angular/core';
import { ControlContainer, NG_VALUE_ACCESSOR, ReactiveFormsModule } from '@angular/forms';

import { DotCMSContentlet, DotCMSContentTypeField } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';

import { DotCardFieldContentComponent } from '../../../dot-card-field/components/dot-card-field-content.component';
import { DotCardFieldFooterComponent } from '../../../dot-card-field/components/dot-card-field-footer.component';
import { DotCardFieldComponent } from '../../../dot-card-field/dot-card-field.component';
import { BaseFieldComponent } from '../../../shared/base-field.component';
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
        DotMessagePipe,
        DotEditContentBinaryFieldComponent
    ],
    templateUrl: './dot-binary-field-wrapper.component.html',
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
            useExisting: forwardRef(() => DotBinaryFieldWrapperComponent)
        }
    ]
})
export class DotBinaryFieldWrapperComponent extends BaseFieldComponent implements OnInit {
    $field = input.required<DotCMSContentTypeField>({
        alias: 'field'
    });
    $contentlet = input.required<DotCMSContentlet>({
        alias: 'contentlet'
    });
    valueUpdated = output<{ value: string; fileName: string }>();

    writeValue(_: unknown): void {
        // Do nothing
    }

    ngOnInit(): void {
        this.statusChanges$.subscribe(() => {
            this.changeDetectorRef.detectChanges();
        });
    }
}
