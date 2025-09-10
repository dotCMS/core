import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';

import { CheckboxModule } from 'primeng/checkbox';

import { DotCMSContentTypeField } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';

import { getSingleSelectableFieldOptions } from '../../utils/functions.util';
import { DotCardFieldContentComponent } from '../dot-card-field/components/dot-card-field-content.component';
import { DotCardFieldFooterComponent } from '../dot-card-field/components/dot-card-field-footer.component';
import { DotCardFieldLabelComponent } from '../dot-card-field/components/dot-card-field-label.component';
import { DotCardFieldComponent } from '../dot-card-field/dot-card-field.component';
import { BaseFieldComponent } from '../shared/base-field.component';

@Component({
    selector: 'dot-edit-content-checkbox-field',
    imports: [
        CheckboxModule,
        ReactiveFormsModule,
        FormsModule,
        DotCardFieldComponent,
        DotCardFieldLabelComponent,
        DotCardFieldContentComponent,
        DotCardFieldFooterComponent,
        DotMessagePipe
    ],
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './dot-edit-content-checkbox-field.component.html'
})
export class DotEditContentCheckboxFieldComponent extends BaseFieldComponent {
    $field = input.required<DotCMSContentTypeField>({ alias: 'field' });

    $options = computed(() =>
        getSingleSelectableFieldOptions(this.$field().values || '', this.$field().dataType)
    );

    writeValue(_: unknown): void {
        // noop
    }
}
