import { ChangeDetectionStrategy, Component, Input, OnInit, inject, signal } from '@angular/core';
import { AbstractControl, ControlContainer, ReactiveFormsModule } from '@angular/forms';

import { TreeNode } from 'primeng/api';
import { TreeSelectModule } from 'primeng/treeselect';

import { DotCMSContentTypeField } from '@dotcms/dotcms-models';

import { DotEditContentFieldSingleSelectableDataTypes } from '../../models/dot-edit-content-field.type';
import { TruncatePathPipe } from '../../pipes/truncate-path.pipe';

const files: TreeNode[] = [
    {
        label: 'demo.dotcms.com',
        data: 'demo.dotcms.com',
        expandedIcon: 'pi pi-folder-open',
        collapsedIcon: 'pi pi-folder',
        children: [
            {
                label: 'demo.dotcms.com/activities',
                data: 'activities',
                expandedIcon: 'pi pi-folder-open',
                collapsedIcon: 'pi pi-folder',
                children: [
                    {
                        label: 'demo.dotcms.com/activities/themes',
                        data: 'themes',
                        icon: 'pi pi-folder-open'
                    }
                ]
            },
            {
                label: 'demo.dotcms.com/home',
                data: 'home',
                icon: 'pi pi-folder-open'
            }
        ]
    }
];

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
    private readonly controlContainer = inject(ControlContainer);

    options = signal<TreeNode[]>(files);

    ngOnInit() {
        const options = this.options();
        this.formControl.patchValue(options[0].children[0]);
    }

    /**
     * Returns the form control for the select field.
     * @returns {AbstractControl} The form control for the select field.
     */
    get formControl(): AbstractControl {
        return this.controlContainer.control.get(
            this.field.variable
        ) as AbstractControl<DotEditContentFieldSingleSelectableDataTypes>;
    }

    /**
    onNodeSelect(event: TreeNodeSelectEvent) {
        console.log(event.node);
    }
    */
}
