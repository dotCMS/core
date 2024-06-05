import { NgClass } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject, input } from '@angular/core';
import { ControlContainer, ReactiveFormsModule } from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { ChipModule } from 'primeng/chip';
import { ChipsModule } from 'primeng/chips';
import { DialogService } from 'primeng/dynamicdialog';
import { TooltipModule } from 'primeng/tooltip';

import { DotCMSContentTypeField } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';

import { DotEditContentCategoryFieldDialogComponent } from './components/dot-edit-content-category-field-dialog/dot-edit-content-category-field-dialog.component';

/**
 * Component for editing content category field.
 *
 * @class
 * @name DotEditContentCategoryFieldComponent
 */
@Component({
    selector: 'dot-edit-content-category-field',
    standalone: true,
    imports: [
        ChipsModule,
        ReactiveFormsModule,
        ButtonModule,
        ChipModule,
        NgClass,
        TooltipModule,
        DotMessagePipe
    ],
    templateUrl: './dot-edit-content-category-field.component.html',
    styleUrl: './dot-edit-content-category-field.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush,
    viewProviders: [
        {
            provide: ControlContainer,
            useFactory: () => inject(ControlContainer, { skipSelf: true })
        }
    ],
    providers: [DialogService],
    // eslint-disable-next-line @angular-eslint/no-host-metadata-property
    host: {
        '[class.dot-category-field__container--has-categories]': 'hasCategories()',
        '[class.dot-category-field__container]': '!hasCategories()'
    }
})
export class DotEditContentCategoryFieldComponent {
    /**
     * The `field` variable is of type `DotCMSContentTypeField` and is a required input.
     *
     * @name field
     * @description The variable represents a field of a DotCMS content type.
     */
    field = input.required<DotCMSContentTypeField>();

    // TODO: Replace with the content of the selected categories
    // values = [];
    values = [
        { id: 1, value: 'Streetwear' },
        { id: 2, value: 'Boys' },
        { id: 2, value: 'Jeans' },
        { id: 2, value: 'Pants' },
        { id: 2, value: 'Formal' },
        { id: 2, value: 'Plain' },
        { id: 2, value: 'Pants with linen for woman' },
        { id: 2, value: 'Printed' },
        { id: 2, value: 'Formal pants for man' },
        { id: 2, value: 'Kids' },
        { id: 2, value: 'Kids streetwear' }
    ];
    #dialogService = inject(DialogService);

    /**
     * Checks if the object has categories.
     * @returns {boolean} - True if the object has categories, false otherwise.
     */
    hasCategories(): boolean {
        return this.values.length > 0;
    }

    /**
     * Open the "DotEditContentCategoryFieldDialogComponent" dialog to show categories.
     *
     * @returns {void}
     */
    showCategories(): void {
        this.#dialogService.open(DotEditContentCategoryFieldDialogComponent, {
            showHeader: false,
            styleClass: 'category-field__dialog',
            width: '1000px',
            height: '600px',
            position: 'center'
        });
    }
}
