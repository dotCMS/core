import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';

import { DropdownModule } from 'primeng/dropdown';

import { DotCMSContentTypeField } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';

import { getSingleSelectableFieldOptions } from '../../utils/functions.util';
import { DotCardFieldContentComponent } from '../dot-card-field/components/dot-card-field-content.component';
import { DotCardFieldFooterComponent } from '../dot-card-field/components/dot-card-field-footer.component';
import { DotCardFieldComponent } from '../dot-card-field/dot-card-field.component';
import { BaseFieldComponent } from '../shared/base-field.component';

@Component({
    selector: 'dot-edit-content-select-field',
    imports: [
        DropdownModule,
        ReactiveFormsModule,
        DotCardFieldComponent,
        DotCardFieldContentComponent,
        DotCardFieldFooterComponent,
        DotMessagePipe
    ],
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './dot-edit-content-select-field.component.html'
})
export class DotEditContentSelectFieldComponent extends BaseFieldComponent {
    $field = input.required<DotCMSContentTypeField>({ alias: 'field' });

    $options = computed(() => {
        const field = this.$field();

        return getSingleSelectableFieldOptions(field?.values || '', field.dataType);
    });

    writeValue(_: unknown): void {
        // noop
    }
}
