import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject, input, signal } from '@angular/core';
import { AbstractControl, ControlContainer, ReactiveFormsModule } from '@angular/forms';

import { AutoCompleteModule } from 'primeng/autocomplete';

import { DotCMSContentTypeField } from '@dotcms/dotcms-models';
import { DotSelectItemDirective } from '@dotcms/ui';

import { AutoCompleteCompleteEvent } from '../../models/dot-edit-content-tag.interface';
import { DotEditContentService } from '../../services/dot-edit-content.service';

@Component({
    selector: 'dot-edit-content-tag-field',
    standalone: true,
    imports: [CommonModule, AutoCompleteModule, ReactiveFormsModule, DotSelectItemDirective],
    changeDetection: ChangeDetectionStrategy.OnPush,
    viewProviders: [
        {
            provide: ControlContainer,
            useFactory: () => inject(ControlContainer, { skipSelf: true })
        }
    ],
    template: `
        <p-autoComplete
            (completeMethod)="getTags($event)"
            [formControlName]="$field().variable"
            [id]="'tag-id-' + $field().variable"
            [inputId]="$field().variable"
            [attr.data-testId]="$field().variable"
            [suggestions]="$options()"
            [multiple]="true"
            [unique]="true"
            [showClear]="true"
            dotSelectItem />
    `
})
export class DotEditContentTagFieldComponent {
    $field = input.required<DotCMSContentTypeField>({ alias: 'field' });
    $options = signal<string[] | null>(null);
    private readonly editContentService = inject(DotEditContentService);
    private readonly controlContainer = inject(ControlContainer);

    /**
     * Returns the form control for the select field.
     * @returns {AbstractControl} The form control for the select field.
     */
    get formControl() {
        const field = this.$field();

        return this.controlContainer.control.get(field.variable) as AbstractControl<string>;
    }

    /**
     * Retrieves tags based on the provided query.
     * @param event - The AutoCompleteCompleteEvent object containing the query.
     */
    getTags({ query }: AutoCompleteCompleteEvent) {
        if (query.length < 3) {
            return;
        }

        this.editContentService.getTags(query).subscribe((tags) => {
            this.$options.set(tags);
        });
    }
}
