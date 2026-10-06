import { ChangeDetectionStrategy, Component, input } from '@angular/core';

import { DotCMSContentTypeField } from '@dotcms/dotcms-models';

@Component({
    selector: 'dot-edit-content-line-divider-field',
    templateUrl: './dot-edit-content-line-divider-field.component.html',
    styleUrls: ['./dot-edit-content-line-divider-field.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    host: {
        class: 'dot-edit-content-line-divider-field block'
    }
})
export class DotEditContentLineDividerFieldComponent {
    /**
     * The line divider field metadata used to render the section title.
     */
    $field = input.required<DotCMSContentTypeField>({ alias: 'field' });
}
