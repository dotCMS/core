import {
    ChangeDetectionStrategy,
    Component,
    Input,
    OnInit,
    ViewChild,
    inject,
    signal
} from '@angular/core';
import { ControlContainer, FormControl, ReactiveFormsModule } from '@angular/forms';

import { TreeSelect, TreeSelectModule } from 'primeng/treeselect';

import { Site } from '@dotcms/dotcms-js';
import { DotCMSContentTypeField } from '@dotcms/dotcms-models';

import { DotEditContentFieldSingleSelectableDataTypes } from '../../models/dot-edit-content-field.type';
import {
    TreeNodeItem,
    TreeNodeSelectEvent,
    TreeNodeSelectItem
} from '../../models/dot-edit-content-host-folder-field.interface';
import { TruncatePathPipe } from '../../pipes/truncate-path.pipe';
import { DotEditContentService } from '../../services/dot-edit-content.service';

@Component({
    selector: 'dot-edit-content-host-folder-field',
    standalone: true,
    imports: [TreeSelectModule, ReactiveFormsModule, TruncatePathPipe],
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
    @ViewChild(TreeSelect) tree!: TreeSelect;
    readonly #controlContainer = inject(ControlContainer);
    readonly #editContentService = inject(DotEditContentService);

    options = signal<TreeNodeItem[]>([]);
    pathControl = new FormControl();

    ngOnInit() {
        this.#editContentService.getSitesTreePath().subscribe((options) => {
            this.options.set(options);
            this.getInitialValue();
        });
    }

    get formControl(): FormControl {
        return this.#controlContainer.control.get(
            this.field.variable
        ) as FormControl<DotEditContentFieldSingleSelectableDataTypes>;
    }

    onNodeSelect(event: TreeNodeSelectEvent<Site>) {
        this.formControl.setValue(event.node.label);
    }

    onNodeExpand(event: TreeNodeSelectItem) {
        if (!event.node.children) {
            this.#editContentService.getFoldersTreeNode(event.node.label).subscribe((children) => {
                if (children.length > 0) {
                    event.node.children = children;
                } else {
                    event.node.leaf = true;
                    event.node.icon = 'pi pi-folder-open';
                }

                this.tree.cd.detectChanges();
            });
        }
    }

    getInitialValue() {
        const value = this.formControl.value as string;
        if (value) {
            const split = value.split('/');
            const paths = split.reduce((array, item, index) => {
                const prev = array[index - 1];
                let path = `${item}/`;
                if (prev) {
                    path = `${prev}${path}`;
                }

                array.push(path);

                return array;
            }, []);
            this.#editContentService.buildTreeByPaths(paths).subscribe((rta) => {
                const sitePath = rta.tree.path.replace('/', '');
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
}
