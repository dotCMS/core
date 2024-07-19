import { NgClass } from '@angular/common';
import {
    AfterViewInit,
    ChangeDetectionStrategy,
    Component,
    Injector,
    Input,
    OnInit,
    ViewChild,
    effect,
    inject
} from '@angular/core';
import { ControlContainer, FormControl, ReactiveFormsModule, Validators } from '@angular/forms';

import { DotCMSContentTypeField } from '@dotcms/dotcms-models';

import { TreeSelect, TreeSelectModule } from './componentes/treeselect.component';
import { HostFolderFiledStore } from './store/host-folder-field.store';

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
export class DotEditContentHostFolderFieldComponent implements OnInit, AfterViewInit {
    @Input() field!: DotCMSContentTypeField;
    @ViewChild(TreeSelect) treeSelect!: TreeSelect;
    readonly #controlContainer = inject(ControlContainer);
    readonly #injector = inject(Injector);
    readonly store = inject(HostFolderFiledStore);

    pathControl = new FormControl();

    constructor() {
        effect(() => {
            const nodeSelected = this.store.nodeSelected();
            this.pathControl.setValue(nodeSelected);
        });

        effect(() => {
            const pathToSave = this.store.pathToSave();
            this.formControl.setValue(pathToSave);
        });
    }

    ngOnInit() {
        const currentPath = this.formControl.value;
        const isRequired = this.formControl.hasValidator(Validators.required);
        this.store.loadSites({
            path: currentPath,
            isRequired
        });
    }

    ngAfterViewInit() {
        effect(
            () => {
                this.store.nodeExpaned();
                if (this.treeSelect.treeViewChild) {
                    this.treeSelect.treeViewChild.updateSerializedValue();
                    this.treeSelect.cd.detectChanges();
                }
            },
            {
                injector: this.#injector
            }
        );
    }

    get formControl(): FormControl {
        return this.#controlContainer.control.get(this.field.variable) as FormControl<string>;
    }
}
