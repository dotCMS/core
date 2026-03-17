import { signalMethod } from '@ngrx/signals';

import {
    ChangeDetectionStrategy,
    Component,
    forwardRef,
    inject,
    input,
    viewChild
} from '@angular/core';
import { FormControl, ReactiveFormsModule, FormsModule, NG_VALUE_ACCESSOR } from '@angular/forms';

import { TreeSelect, TreeSelectModule } from 'primeng/treeselect';

import { TreeNodeItem, TreeNodeSelectItem } from '@dotcms/dotcms-models';
import { DotTruncatePathPipe } from '@dotcms/ui';

import { BaseControlValueAccessor } from '../../../shared/base-control-value-accesor';
import { HostFolderFiledStore } from '../../store/host-folder-field.store';

/**
 * Component for editing content site or folder field.
 *
 * @export
 * @class DotEditContentHostFolderFieldComponent
 */
@Component({
    selector: 'dot-host-folder-field',
    imports: [TreeSelectModule, ReactiveFormsModule, DotTruncatePathPipe, FormsModule],
    templateUrl: './host-folder-field.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [
        HostFolderFiledStore,
        {
            multi: true,
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => DotHostFolderFieldComponent)
        }
    ]
})
export class DotHostFolderFieldComponent extends BaseControlValueAccessor<string> {
    /**
     * A signal that holds the error state of the field.
     * It is used to display the error state of the field.
     */
    $hasError = input.required<boolean>({ alias: 'hasError' });
    /**
     * A signal that holds the required state of the field.
     * It is used to display the required state of the field.
     */
    $isRequired = input.required<boolean>({ alias: 'isRequired' });
    /**
     * A signal that holds the tree select.
     * It is used to display the tree select in the component.
     */
    $treeSelect = viewChild<TreeSelect>(TreeSelect);
    /**
     * A readonly instance of the HostFolderFiledStore injected into the component.
     * This store is used to manage the state and actions related to the host folder field.
     */
    readonly store = inject(HostFolderFiledStore);
    /**
     * A FormControl instance that holds the path of the field.
     */
    pathControl = new FormControl(null);

    constructor() {
        super();
        this.handleNodeExpanedChange(this.store.nodeExpaned);
        this.handleNodeSelectedChange(this.store.nodeSelected);
        this.handlePathToSaveChange(this.store.pathToSave);
        this.handleDisabledChange(this.$isDisabled);
        this.handleChangeValue(this.$value);
    }

    /**
     * A signal that handles the path to save change of the field.
     * It is used to save the path to the field.
     */
    readonly handlePathToSaveChange = signalMethod<string>((pathToSave) => {
        if (pathToSave === null || pathToSave === undefined || !this.onChange || !this.onTouched) {
            return;
        }

        this.onChange(pathToSave);
        this.onTouched();
    });

    /**
     * A signal that handles the node selected change of the field.
     * It is used to update the path control.
     */
    readonly handleNodeSelectedChange = signalMethod<TreeNodeItem>((nodeSelected) => {
        if (!nodeSelected) {
            return;
        }

        this.pathControl.setValue(nodeSelected);
    });

    /**
     * A signal that handles the node expanded change of the field.
     * It is used to update the serialized value of the tree select.
     */
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
     * A signal that handles the disabled change of the field.
     * It is used to disable the path control.
     */
    readonly handleDisabledChange = signalMethod<boolean>((isDisabled) => {
        if (isDisabled) {
            this.pathControl.disable({ emitEvent: false });
        } else {
            this.pathControl.enable({ emitEvent: false });
        }
    });

    /**
     * A signal that handles the change value of the field.
     * It is used to load the sites based on the current path.
     */
    readonly handleChangeValue = signalMethod<string>((currentPath) => {
        this.store.loadSites({
            path: currentPath,
            isRequired: this.$isRequired()
        });
    });
}
