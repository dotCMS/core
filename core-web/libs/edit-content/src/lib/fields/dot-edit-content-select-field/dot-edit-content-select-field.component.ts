import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, Input, OnInit, inject } from '@angular/core';
import { ControlContainer, ReactiveFormsModule } from '@angular/forms';

import { DropdownModule } from 'primeng/dropdown';

import { DotCMSContentTypeField } from '@dotcms/dotcms-models';
import { DotFieldRequiredDirective } from '@dotcms/ui';

import { mapOptions } from '../../utils/functions.util';

@Component({
    selector: 'dot-edit-content-select-field',
    standalone: true,
    imports: [CommonModule, DropdownModule, ReactiveFormsModule, DotFieldRequiredDirective],
    templateUrl: './dot-edit-content-select-field.component.html',
    styleUrls: ['./dot-edit-content-select-field.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    viewProviders: [
        {
            provide: ControlContainer,
            useFactory: () => inject(ControlContainer, { skipSelf: true })
        }
    ]
})
export class DotEditContentSelectFieldComponent implements OnInit {
    @Input() field!: DotCMSContentTypeField;

    options = [];

    ngOnInit() {
        this.options = mapOptions(this.field.values || '', this.field.dataType);
    }
}
