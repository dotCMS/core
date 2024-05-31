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
    providers: [DialogService]
})
export class DotEditContentCategoryFieldComponent {
    field = input.required<DotCMSContentTypeField>();
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

    showCategories() {
        this.#dialogService.open(DotEditContentCategoryFieldDialogComponent, {
            showHeader: false,
            styleClass: 'category-field__dialog',
            width: '1000px',
            height: '600px',
            position: 'center'
        });
    }
}
