import { signalMethod } from '@ngrx/signals';

import {
    ChangeDetectionStrategy,
    Component,
    inject,
    input,
    viewChild,
    OnInit
} from '@angular/core';
import {
    FormControl,
    ReactiveFormsModule,
    FormsModule,
    ControlContainer,
    FormGroup,
    Validators
} from '@angular/forms';

import { TreeSelect, TreeSelectModule } from 'primeng/treeselect';

import { DotCMSContentTypeField } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';

import { HostFolderFiledStore } from './store/host-folder-field.store';

import {
    TreeNodeItem,
    TreeNodeSelectItem
} from '../../models/dot-edit-content-host-folder-field.interface';
import { TruncatePathPipe } from '../../pipes/truncate-path.pipe';
import { DotCardFieldContentComponent } from '../dot-card-field/components/dot-card-field-content.component';
import { DotCardFieldFooterComponent } from '../dot-card-field/components/dot-card-field-footer.component';
import { DotCardFieldComponent } from '../dot-card-field/dot-card-field.component';
import { BaseFieldComponent } from '../shared/base-field.component';

/**
 * Component for editing content site or folder field.
 *
 * @export
 * @class DotEditContentHostFolderFieldComponent
 */
@Component({
    selector: 'dot-edit-content-host-folder-field',
    imports: [
        TreeSelectModule,
        ReactiveFormsModule,
        TruncatePathPipe,
        FormsModule,
        DotCardFieldComponent,
        DotCardFieldContentComponent,
        DotCardFieldFooterComponent,
        DotMessagePipe
    ],
    templateUrl: './dot-edit-content-host-folder-field.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [HostFolderFiledStore],
    viewProviders: [
        {
            provide: ControlContainer,
            useFactory: () => inject(ControlContainer, { skipSelf: true })
        }
    ]
})
export class DotEditContentHostFolderFieldComponent extends BaseFieldComponent {
    $field = input.required<DotCMSContentTypeField>({ alias: 'field' });
    $treeSelect = viewChild<TreeSelect>(TreeSelect);
    readonly store = inject(HostFolderFiledStore);
    readonly controlContainer = inject(ControlContainer);

    pathControl = new FormControl(null);

    constructor() {
        super();
        this.handleNodeExpanedChange(this.store.nodeExpaned);
        this.handleNodeSelectedChange(this.store.nodeSelected);
        this.handlePathToSaveChange(this.store.pathToSave);
    }

    /**
     * Set the value of the field.
     * If the value is empty, nothing happens.
     * If the value is not empty, the store is called to get the asset data.
     *
     * @param value the value to set
     */
    writeValue(currentPath: string): void {
        this.store.loadSites({
            path: currentPath,
            isRequired: this.hasRequiredValidator
        });
    }

    readonly handlePathToSaveChange = signalMethod<string>((pathToSave) => {
        if (pathToSave === null || pathToSave === undefined || !this.onChange || !this.onTouched) {
            return;
        }

        this.onChange(pathToSave);
        this.onTouched();
    });

    readonly handleNodeSelectedChange = signalMethod<TreeNodeItem>((nodeSelected) => {
        if (!nodeSelected) {
            return;
        }

        this.pathControl.setValue(nodeSelected);
    });

    readonly handleNodeExpanedChange = signalMethod<TreeNodeSelectItem['node']>((nodeExpaned) => {
        if (!nodeExpaned) {
            return;
        }

        const treeSelect = this.$treeSelect();
        if (treeSelect.treeViewChild) {
            treeSelect.treeViewChild.updateSerializedValue();
            treeSelect.cd.detectChanges();
        }
    });

    /**
     * Sets the disabled state of the control.
     *
     * @param isDisabled The disabled state to set.
     */
    setDisabledState(isDisabled: boolean): void {
        if (isDisabled) {
            this.pathControl.disable({ emitEvent: false });
        } else {
            this.pathControl.enable({ emitEvent: false });
        }
    }

    get control(): FormGroup {
        return this.controlContainer.control.get(this.$field().variable) as FormGroup;
    }

    get hasRequiredValidator(): boolean {
        const control = this.control;
        return control.hasValidator(Validators.required);
    }
}
