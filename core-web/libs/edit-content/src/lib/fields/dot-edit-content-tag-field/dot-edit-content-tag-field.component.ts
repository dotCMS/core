import { Observable } from 'rxjs';

import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, Input, inject } from '@angular/core';
import { AbstractControl, ControlContainer, ReactiveFormsModule } from '@angular/forms';

import { AutoCompleteModule } from 'primeng/autocomplete';

import { DotCMSContentTypeField } from '@dotcms/dotcms-models';
import { DotSelectItemDirective } from '@dotcms/ui';

import { AutoCompleteCompleteEvent } from '../../models/dot-edit-content-tag.interface';
import { DotEditContentService } from '../../services/dot-edit-content.service';

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

    private readonly editContentService = inject(DotEditContentService);
    private readonly controlContainer = inject(ControlContainer);

    options$!: Observable<string[]>;

    /**
     * Retrieves tags based on the provided query.
     * @param event - The AutoCompleteCompleteEvent object containing the query.
     */
    getTags({ query }: AutoCompleteCompleteEvent) {
        if (query.length < 3) {
            return;
        }

        this.options$ = this.editContentService.getTags(query);
    }

    /**
     * Returns the form control for the select field.
     * @returns {AbstractControl} The form control for the select field.
     */
    get formControl() {
        return this.controlContainer.control.get(this.field.variable) as AbstractControl<string>;
    }
}
