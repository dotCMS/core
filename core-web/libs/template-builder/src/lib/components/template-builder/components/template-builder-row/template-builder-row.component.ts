import { GridItemHTMLElement } from 'gridstack';

import { ChangeDetectionStrategy, Component, ElementRef, Input } from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { DialogService } from 'primeng/dynamicdialog';

import { filter, take } from 'rxjs/operators';

import { DotMessageService } from '@dotcms/data-access';

import { DotGridStackWidget } from '../../models/models';
import { DotTemplateBuilderStore } from '../../store/template-builder.store';
import { AddStyleClassesDialogComponent } from '../add-style-classes-dialog/add-style-classes-dialog.component';
import { RemoveConfirmDialogComponent } from '../remove-confirm-dialog/remove-confirm-dialog.component';

@Component({
    selector: 'dotcms-template-builder-row',
    standalone: true,
    templateUrl: './template-builder-row.component.html',
    styleUrls: ['./template-builder-row.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    imports: [ButtonModule, RemoveConfirmDialogComponent]
})
export class TemplateBuilderRowComponent {
    @Input() row: DotGridStackWidget;

    constructor(
        private el: ElementRef,
        private store: DotTemplateBuilderStore,
        private dialogService: DialogService,
        private dotMessage: DotMessageService
    ) {}

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
