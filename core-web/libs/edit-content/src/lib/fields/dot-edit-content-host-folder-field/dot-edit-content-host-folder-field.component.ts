import { signalMethod } from '@ngrx/signals';

import {
    ChangeDetectionStrategy,
    Component,
    inject,
    input,
    viewChild,
    forwardRef
} from '@angular/core';
import {
    FormControl,
    NG_VALUE_ACCESSOR,
    ReactiveFormsModule,
    Validators,
    ControlValueAccessor,
    FormsModule,
    ControlContainer
} from '@angular/forms';

import { TreeSelect, TreeSelectModule } from 'primeng/treeselect';

import { DotCMSContentTypeField } from '@dotcms/dotcms-models';

import { HostFolderFiledStore } from './store/host-folder-field.store';

import {
    TreeNodeItem,
    TreeNodeSelectItem
} from '../../models/dot-edit-content-host-folder-field.interface';
import { TruncatePathPipe } from '../../pipes/truncate-path.pipe';

/**
 * Component for editing content site or folder field.
 *
 * @export
 * @class DotEditContentHostFolderFieldComponent
 */
@Component({
    selector: 'dot-edit-content-host-folder-field',
    imports: [TreeSelectModule, ReactiveFormsModule, TruncatePathPipe, FormsModule],
    templateUrl: './dot-edit-content-host-folder-field.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    viewProviders: [
        {
            provide: ControlContainer,
            useFactory: () => inject(ControlContainer, { skipSelf: true })
        }
    ],
    providers: [
        HostFolderFiledStore,
        {
            multi: true,
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => DotEditContentHostFolderFieldComponent)
        }
    ]
})
export class DotEditContentHostFolderFieldComponent implements ControlValueAccessor {
    $field = input.required<DotCMSContentTypeField>({ alias: 'field' });
    $treeSelect = viewChild<TreeSelect>(TreeSelect);
    readonly store = inject(HostFolderFiledStore);
    readonly #controlContainer = inject(ControlContainer);

    pathControl = new FormControl(null);

    onChange: ((value: string) => void) | null = null;
    onTouched: (() => void) | null = null;

    constructor() {
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
        const isRequired = this.formControl.hasValidator(Validators.required);
        this.store.loadSites({
            path: currentPath,
            isRequired
        });
    }
    /**
     * Registers a callback function that is called when the control's value changes in the UI.
     * This function is passed to the {@link NG_VALUE_ACCESSOR} token.
     *
     * @param fn The callback function to register.
     */
    registerOnChange(fn: (value: string) => void) {
        this.onChange = fn;
    }

    /**
     * Registers a callback function that is called when the control is marked as touched in the UI.
     * This function is passed to the {@link NG_VALUE_ACCESSOR} token.
     *
     * @param fn The callback function to register.
     */
    registerOnTouched(fn: () => void) {
        this.onTouched = fn;
    }

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

    readonly handlePathToSaveChange = signalMethod<string>((pathToSave) => {
        if (this.onChange) {
            this.onChange(pathToSave);
            this.onTouched();
        }
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

    get formControl(): FormControl {
        const field = this.$field();

        return this.#controlContainer.control.get(field.variable) as FormControl<string>;
    }
}
