import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { ReactiveFormsModule, FormsModule } from '@angular/forms';

import { InputTextModule } from 'primeng/inputtext';

import { DotCMSContentTypeField } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';

import { INPUT_TEXT_OPTIONS } from './utils';

import { DotCardFieldContentComponent } from '../dot-card-field/components/dot-card-field-content.component';
import { DotCardFieldFooterComponent } from '../dot-card-field/components/dot-card-field-footer.component';
import { DotCardFieldLabelComponent } from '../dot-card-field/components/dot-card-field-label.component';
import { DotCardFieldComponent } from '../dot-card-field/dot-card-field.component';
import { BaseFieldComponent } from '../shared/base-field.component';

@Component({
    selector: 'dot-edit-content-text-field',
    templateUrl: './dot-edit-content-text-field.component.html',
    styleUrls: ['./dot-edit-content-text-field.component.scss'],
    imports: [
        ReactiveFormsModule,
        FormsModule,
        InputTextModule,
        DotMessagePipe,
        DotCardFieldComponent,
        DotCardFieldLabelComponent,
        DotCardFieldContentComponent,
        DotCardFieldFooterComponent
    ],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotEditContentTextFieldComponent extends BaseFieldComponent {
    /**
     * The field configuration from DotCMS
     */
    $field = input.required<DotCMSContentTypeField>({ alias: 'field' });

    readonly inputTextOptions = INPUT_TEXT_OPTIONS;

    constructor() {
        super();
    }

    writeValue(_: unknown): void {
        // Do nothing
    }
}
