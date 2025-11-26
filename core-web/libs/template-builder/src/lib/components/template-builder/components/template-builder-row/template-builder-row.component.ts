import { GridItemHTMLElement } from 'gridstack';

import { NgStyle } from '@angular/common';
import { ChangeDetectionStrategy, Component, ElementRef, Input, inject } from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { DialogService } from 'primeng/dynamicdialog';

import { filter, take } from 'rxjs/operators';

import { DotMessageService } from '@dotcms/data-access';

import { DotGridStackWidget } from '../../models/models';
import { DotTemplateBuilderStore } from '../../store/template-builder.store';
import { AddStyleClassesDialogComponent } from '../add-style-classes-dialog/add-style-classes-dialog.component';
import { RemoveConfirmDialogComponent } from '../remove-confirm-dialog/remove-confirm-dialog.component';
import { TemplateBuilderBackgroundColumnsComponent } from '../template-builder-background-columns/template-builder-background-columns.component';

@Component({
    selector: 'dotcms-template-builder-row',
    templateUrl: './template-builder-row.component.html',
    styleUrls: ['./template-builder-row.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    imports: [
        ButtonModule,
        RemoveConfirmDialogComponent,
        TemplateBuilderBackgroundColumnsComponent,
        NgStyle
    ]
})
export class TemplateBuilderRowComponent {
    private el = inject(ElementRef);
    private store = inject(DotTemplateBuilderStore);
    private dialogService = inject(DialogService);
    private dotMessage = inject(DotMessageService);

    @Input() row: DotGridStackWidget;

    @Input() isResizing = false;

    get nativeElement(): GridItemHTMLElement {
        return this.el.nativeElement;
    }

    /**
     * @description Open the dialog to edit the classes of the row
     *
     * @memberof TemplateBuilderRowComponent
     */
    editClasses() {
        const ref = this.dialogService.open(AddStyleClassesDialogComponent, {
            header: this.dotMessage.get('dot.template.builder.classes.dialog.header.label'),
            data: {
                selectedClasses: this.row.styleClass || []
            },
            resizable: false
        });

        ref.onClose
            .pipe(
                take(1),
                filter((styleClasses) => styleClasses)
            )
            .subscribe((styleClasses) => {
                this.store.updateRow({ id: this.row.id as string, styleClass: styleClasses });
            });
    }

    /**
     * @description Delete the row from the store
     *
     * @memberof TemplateBuilderRowComponent
     */
    deleteRow() {
        this.store.removeRow(this.row.id as string);
    }
}
