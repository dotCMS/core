import { Observable } from 'rxjs';

import { Component, OnInit, inject } from '@angular/core';

import { DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';

import {
    DotAddVariableState,
    DotAddVariableStore
} from '@dotcms/app/portlets/dot-containers/dot-container-create/dot-container-code/dot-add-variable/store/dot-add-variable.store';

import { DotFieldContent } from './dot-add-variable.models';

@Component({
    selector: 'dot-add-variable',
    templateUrl: './dot-add-variable.component.html',
    styleUrls: ['./dot-add-variable.component.scss'],
    providers: [DotAddVariableStore]
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
