import { GridItemHTMLElement } from 'gridstack';

import { NgClass, NgFor, NgIf } from '@angular/common';
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
import { DropdownModule } from 'primeng/dropdown';
import { ScrollPanelModule } from 'primeng/scrollpanel';

import { DotMessageService } from '@dotcms/data-access';
import { DotContainer, DotContainerMap } from '@dotcms/dotcms-models';
import { DotContainerOptionsDirective } from '@dotcms/ui';

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
        NgFor,
        NgIf,
        NgClass,
        ButtonModule,
        ScrollPanelModule,
        RemoveConfirmDialogComponent,
        DropdownModule,
        DotContainerOptionsDirective,
        ReactiveFormsModule
    ]
})
export class TemplateBuilderBoxComponent implements OnChanges {
    protected readonly templateBuilderSizes = TemplateBuilderBoxSize;

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

    boxVariant = TemplateBuilderBoxSize.small;
    dropdownLabel: string | null = null;
    formControl = new FormControl(null); // used to programmatically set dropdown value, so that the same value can be selected twice consecutively

    constructor(private el: ElementRef, private dotMessage: DotMessageService) {}

    get nativeElement(): GridItemHTMLElement {
        return this.el.nativeElement;
    }

    ngOnChanges(): void {
        this.boxVariant = getBoxVariantByWidth(this.width);
        this.dropdownLabel =
            this.boxVariant === TemplateBuilderBoxSize.large
                ? this.dotMessage.get('dot.template.builder.add.container')
                : null;
    }

    onContainerSelect({ value }: { value: DotContainer }) {
        this.addContainer.emit(value);
        this.formControl.setValue(null);
    }
}
