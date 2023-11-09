import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, Input, inject } from '@angular/core';
import { ControlContainer, ReactiveFormsModule } from '@angular/forms';

import { AutoCompleteModule } from 'primeng/autocomplete';

import { DotCMSContentTypeField } from '@dotcms/dotcms-models';
import { DotSelectItemDirective } from '@dotcms/ui';

@Component({
    selector: 'dot-edit-content-tag-field',
    standalone: true,
    imports: [CommonModule, AutoCompleteModule, DotSelectItemDirective, ReactiveFormsModule],
    templateUrl: './dot-edit-content-tag-field.component.html',
    styleUrls: ['./dot-edit-content-tag-field.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    viewProviders: [
        {
            provide: ControlContainer,
            useFactory: () => inject(ControlContainer, { skipSelf: true })
        }
    ]
})
export class DotEditContentTagFieldComponent {
    @Input() field: DotCMSContentTypeField;
}
