import { ChangeDetectionStrategy, Component, Input, inject, signal } from '@angular/core';
import { AbstractControl, ControlContainer, ReactiveFormsModule } from '@angular/forms';

import { TreeNode } from 'primeng/api';
import { TreeSelectModule } from 'primeng/treeselect';

import { DotCMSContentTypeField } from '@dotcms/dotcms-models';

import { DotEditContentFieldSingleSelectableDataTypes } from '../../models/dot-edit-content-field.type';

const files: TreeNode[] = [
    {
        label: 'Documents',
        data: 'Documents Folder',
        expandedIcon: 'pi pi-folder-open',
        collapsedIcon: 'pi pi-folder',
        children: [
            {
                label: 'Work',
                data: 'Work Folder',
                expandedIcon: 'pi pi-folder-open',
                collapsedIcon: 'pi pi-folder',
                children: [
                    {
                        label: 'Expenses.doc',
                        icon: 'pi pi-file',
                        data: 'Expenses Document'
                    },
                    { label: 'Resume.doc', icon: 'pi pi-file', data: 'Resume Document' }
                ]
            },
            {
                label: 'Home',
                data: 'Home Folder',
                expandedIcon: 'pi pi-folder-open',
                collapsedIcon: 'pi pi-folder',
                children: [
                    {
                        label: 'Invoices.txt',
                        icon: 'pi pi-file',
                        data: 'Invoices for this month'
                    }
                ]
            }
        ]
    }
];

@Component({
    selector: 'dot-edit-content-host-folder-field',
    standalone: true,
    imports: [TreeSelectModule, ReactiveFormsModule],
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
export class DotEditContentHostFolderFieldComponent {
    @Input() field!: DotCMSContentTypeField;
    private readonly controlContainer = inject(ControlContainer);

    options = signal<TreeNode[]>(files);

    /**
     * Returns the form control for the select field.
     * @returns {AbstractControl} The form control for the select field.
     */
    get formControl(): AbstractControl {
        return this.controlContainer.control.get(
            this.field.variable
        ) as AbstractControl<DotEditContentFieldSingleSelectableDataTypes>;
    }
}
