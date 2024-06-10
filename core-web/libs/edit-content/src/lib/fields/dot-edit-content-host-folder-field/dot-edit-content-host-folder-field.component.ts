import { NgClass } from '@angular/common';
import {
    ChangeDetectionStrategy,
    Component,
    Input,
    OnInit,
    ViewChild,
    computed,
    inject,
    signal
} from '@angular/core';
import { ControlContainer, FormControl, ReactiveFormsModule } from '@angular/forms';

import { TreeSelect, TreeSelectModule } from 'primeng/treeselect';

import { DotCMSContentTypeField } from '@dotcms/dotcms-models';

import { DotEditContentFieldSingleSelectableDataTypes } from '../../models/dot-edit-content-field.type';
import {
    StatusRequest,
    TreeNodeItem,
    TreeNodeSelectItem
} from '../../models/dot-edit-content-host-folder-field.interface';
import { TruncatePathPipe } from '../../pipes/truncate-path.pipe';
import { DotEditContentService } from '../../services/dot-edit-content.service';
import { createPaths } from '../../utils/functions.util';

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
    ]
})
export class DotEditContentHostFolderFieldComponent implements OnInit {
    @Input() field!: DotCMSContentTypeField;
    @ViewChild(TreeSelect) treeSelect!: TreeSelect;
    readonly #controlContainer = inject(ControlContainer);
    readonly #editContentService = inject(DotEditContentService);

    options = signal<TreeNodeItem[]>([]);
    sitesStatus = signal<StatusRequest>('init');
    pathControl = new FormControl();
    iconClasses = computed(() => {
        const status = this.sitesStatus();

        return {
            'pi-spin pi-spinner': status === 'loading',
            'pi-chevron-down': status !== 'loading'
        };
    });

    ngOnInit() {
        this.sitesStatus.set('loading');
        this.#editContentService.getSitesTreePath().subscribe({
            next: (options) => {
                this.options.set(options);
                this.setInitialValue();
                this.sitesStatus.set('success');
            },
            error: () => {
                this.sitesStatus.set('failed');
            }
        });
    }

    get formControl(): FormControl {
        return this.#controlContainer.control.get(
            this.field.variable
        ) as FormControl<DotEditContentFieldSingleSelectableDataTypes>;
    }

    onNodeSelect(event: TreeNodeSelectItem) {
        const data = event.node.data;
        if (!data) {
            return;
        }

        const path = `${data.hostname}:${data.path ? data.path : '/'}`;
        this.formControl.setValue(path);
    }

    onNodeExpand(event: TreeNodeSelectItem) {
        const { node } = event;
        const { children, icon } = node;
        if ((children && children.length > 0) || icon?.includes('spinner')) {
            return;
        }

        const { hostname, path } = node.data;
        node.icon = 'pi pi-spin pi-spinner';
        this.#editContentService.getFoldersTreeNode(hostname, path).subscribe((children) => {
            node.leaf = true;
            node.icon = 'pi pi-folder-open';
            node.children = children;
            this.treeSelect.cd.detectChanges();
        });
    }

    setInitialValue() {
        const value = this.formControl.value as string;
        if (value) {
            const hasPaths = value.includes('/');
            if (hasPaths) {
                this.buildTreeByPaths(value);
            } else {
                const options = this.options();
                this.pathControl.setValue(options.find((item) => item.key === value));
            }
        }
    }

    private buildTreeByPaths(path: string) {
        const paths = createPaths(path);
        this.#editContentService.buildTreeByPaths(paths).subscribe((rta) => {
            const sitePath = rta.tree.path;
            this.options.update((options) => {
                return options.map((item) => {
                    if (item.key === sitePath) {
                        return {
                            ...item,
                            children: [...rta.tree.folders]
                        };
                    }

                    return item;
                });
            });
            this.pathControl.setValue(rta.node);
        });
    }
}
