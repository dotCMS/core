import { Observable } from 'rxjs';

import { Component, OnInit, inject } from '@angular/core';

import { DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';

import { DotAddVariableStore } from '@dotcms/app/portlets/dot-containers/dot-container-create/dot-container-code/dot-add-variable/store/dot-add-variable.store';

import { DotVariableContent, DotVariableList } from './dot-add-variable.models';

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

    vm$: Observable<DotVariableList> = this.store.vm$;

    ngOnInit() {
        this.store.getVariables(this.config.data?.contentTypeVariable);
    }

    /**
     * handle save button
     * @param {DotVariableContent} field
     * @returns void
     * @memberof DotAddVariableComponent
     */
    addCustomCode(field: DotVariableContent): void {
        this.config.data?.onSave?.(field.codeTemplate);
        this.ref.close();
    }
}
