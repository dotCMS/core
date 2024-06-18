import { NgClass } from '@angular/common';
import {
    ChangeDetectionStrategy,
    Component,
    Input,
    OnInit,
    ViewChild,
    effect,
    inject
} from '@angular/core';
import { ControlContainer, FormControl, ReactiveFormsModule } from '@angular/forms';

import { DotCMSContentTypeField } from '@dotcms/dotcms-models';

import { TreeSelect, TreeSelectModule } from './componentes/treeselect.component';
import { HostFolderFiledStore } from './store/host-folder-field.store';

import { TreeNodeSelectItem } from '../../models/dot-edit-content-host-folder-field.interface';
import { TruncatePathPipe } from '../../pipes/truncate-path.pipe';

/**
 * Component for editing content site or folder field.
 *
 * @export
 * @class DotEditContentHostFolderFieldComponent
 */
@Component({
    selector: 'dot-edit-content-host-folder-field',
    standalone: true,
    imports: [TreeSelectModule, ReactiveFormsModule, TruncatePathPipe, NgClass],
    templateUrl: './dot-edit-content-host-folder-field.component.html',
    styleUrls: ['./dot-edit-content-host-folder-field.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    viewProviders: [
        {
            provide: ControlContainer,
            useFactory: () => inject(ControlContainer, { skipSelf: true })
        }
    ],
    providers: [HostFolderFiledStore]
})
export class DotEditContentHostFolderFieldComponent implements OnInit {
    @Input() field!: DotCMSContentTypeField;
    @ViewChild(TreeSelect) treeSelect!: TreeSelect;
    readonly #controlContainer = inject(ControlContainer);
    readonly store = inject(HostFolderFiledStore);

    pathControl = new FormControl();

    constructor() {
        effect(() => {
            this.store.nodeExpaned();
            if (this.treeSelect.treeViewChild) {
                this.treeSelect.treeViewChild.updateSerializedValue();
                this.treeSelect.cd.detectChanges();
            }
        });

        effect(() => {
            const nodeSelected = this.store.nodeSelected();
            this.pathControl.setValue(nodeSelected);
        });
    }

    ngOnInit() {
        const currentPath = this.formControl.value;
        this.store.loadSites(currentPath);
    }

    get formControl(): FormControl {
        return this.#controlContainer.control.get(this.field.variable) as FormControl<string>;
    }

    onNodeSelect(event: TreeNodeSelectItem) {
        const data = event.node.data;
        if (!data) {
            return;
        }

        const path = `${data.hostname}:${data.path ? data.path : '/'}`;
        this.formControl.setValue(path);
    }
}
