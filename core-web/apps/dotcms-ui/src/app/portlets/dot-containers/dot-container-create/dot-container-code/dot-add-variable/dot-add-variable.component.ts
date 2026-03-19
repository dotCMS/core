import { Observable } from 'rxjs';

import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { DataViewModule } from 'primeng/dataview';
import { DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';

import { DotMessagePipe } from '@dotcms/ui';

import { DotFieldContent } from './dot-add-variable.models';
import { DotFieldsService } from './services/dot-fields.service';
import { DotAddVariableState, DotAddVariableStore } from './store/dot-add-variable.store';

@Component({
    selector: 'dot-add-variable',
    templateUrl: './dot-add-variable.component.html',
    imports: [CommonModule, ButtonModule, DataViewModule, DotMessagePipe],
    providers: [DotAddVariableStore, DotFieldsService]
})
export class DotAddVariableComponent implements OnInit {
    private readonly store = inject(DotAddVariableStore);
    private readonly config = inject(DynamicDialogConfig);
    private readonly ref = inject(DynamicDialogRef);

    vm$: Observable<DotAddVariableState> = this.store.vm$;

    ngOnInit() {
        this.store.getFields(this.config.data?.contentTypeVariable);
    }

    /**
     * handle save button
     * @param {DotFieldContent} field
     * @returns void
     * @memberof DotAddVariableComponent
     */
    addCustomCode({ codeTemplate }: DotFieldContent): void {
        this.config.data?.onSave?.(codeTemplate);
        this.ref.close();
    }
}
