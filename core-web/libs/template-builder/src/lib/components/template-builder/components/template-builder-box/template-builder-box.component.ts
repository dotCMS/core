import { GridItemHTMLElement } from 'gridstack';

import { CommonModule, NgClass } from '@angular/common';
import {
    ChangeDetectionStrategy,
    Component,
    ElementRef,
    EventEmitter,
    Input,
    OnChanges,
    Output
} from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { DropdownModule } from 'primeng/dropdown';
import { ScrollPanelModule } from 'primeng/scrollpanel';

import { DotMessageService } from '@dotcms/data-access';
import { CONTAINER_SOURCE, DotContainer, DotContainerMap } from '@dotcms/dotcms-models';
import { DotContainerOptionsDirective, DotMessagePipe } from '@dotcms/ui';

import { DotTemplateBuilderContainer, TemplateBuilderBoxSize } from '../../models/models';
import { getBoxVariantByWidth } from '../../utils/gridstack-utils';
import { RemoveConfirmDialogComponent } from '../remove-confirm-dialog/remove-confirm-dialog.component';

@Component({
    selector: 'dotcms-template-builder-box',
    templateUrl: './template-builder-box.component.html',
    styleUrls: ['./template-builder-box.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: true,
    imports: [
        NgClass,
        ButtonModule,
        ScrollPanelModule,
        RemoveConfirmDialogComponent,
        DialogModule,
        DropdownModule,
        DotContainerOptionsDirective,
        ReactiveFormsModule,
        CommonModule,
        DotMessagePipe
    ]
})
export class TemplateBuilderBoxComponent implements OnChanges {
    @Output()
    editClasses: EventEmitter<void> = new EventEmitter<void>();
    @Output()
    addContainer: EventEmitter<DotContainer> = new EventEmitter<DotContainer>();
    @Output()
    deleteContainer: EventEmitter<number> = new EventEmitter<number>();
    @Output()
    deleteColumn: EventEmitter<void> = new EventEmitter<void>();
    @Output()
    deleteColumnRejected: EventEmitter<void> = new EventEmitter<void>();
    @Input() items: DotTemplateBuilderContainer[];
    @Input() width = 1;
    @Input() containerMap: DotContainerMap;
    @Input() actions = ['add', 'delete', 'edit'];
    dialogVisible = false;
    boxVariant = TemplateBuilderBoxSize.small;
    formControl = new FormControl(null); // used to programmatically set dropdown value, so that the same value can be selected twice consecutively
    protected readonly templateBuilderSizes = TemplateBuilderBoxSize;

    constructor(
        private el: ElementRef,
        private dotMessage: DotMessageService
    ) {}

    private _dropdownLabel: string | null = null;

    get dropdownLabel(): string {
        return this.boxVariant === this.templateBuilderSizes.large || this.dialogVisible
            ? this._dropdownLabel
            : '';
    }

    get nativeElement(): GridItemHTMLElement {
        return this.el.nativeElement;
    }

    ngOnChanges(): void {
        this.boxVariant = getBoxVariantByWidth(this.width);
        this._dropdownLabel = this.dotMessage.get('dot.template.builder.add.container');
    }

    onContainerSelect({ value }: { value: DotContainer }) {
        this.addContainer.emit({ ...value, identifier: this.getContainerReference(value) });
        this.formControl.setValue(null);
    }

    requestColumnDelete() {
        this.deleteColumn.emit();
    }

    /**
     * Based on the container source, it returns the identifier that should be used as reference.
     *
     * @param dotContainer
     * @returns string
     * @memberof TemplateBuilderBoxComponent
     */
    private getContainerReference(dotContainer: DotContainer): string {
        return dotContainer.source === CONTAINER_SOURCE.FILE
            ? dotContainer.path
            : dotContainer.identifier;
    }
}
