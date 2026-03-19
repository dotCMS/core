import {
    moduleMetadata,
    StoryObj,
    Meta,
    applicationConfig,
    argsToTemplate,
    componentWrapperDecorator
} from '@storybook/angular';
import { of } from 'rxjs';

import { provideHttpClient } from '@angular/common/http';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { DotMessageService } from '@dotcms/data-access';
import { DotCMSContentTypeField } from '@dotcms/dotcms-models';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotEditContentHostFolderFieldComponent } from './dot-edit-content-host-folder-field.component';
import { HostFolderFiledStore } from './store/host-folder-field.store';

import { DotEditContentService } from '../../services/dot-edit-content.service';
import { HOST_FOLDER_TEXT_MOCK, TREE_SELECT_MOCK } from '../../utils/mocks';

type Args = DotEditContentHostFolderFieldComponent & {
    field: DotCMSContentTypeField;
    value: string;
};

const meta: Meta<Args> = {
    title: 'Library / Edit Content / Host Folder Field',
    component: DotEditContentHostFolderFieldComponent,
    decorators: [
        applicationConfig({
            providers: [
                provideHttpClient(),
                {
                    provide: DotMessageService,
                    useValue: new MockDotMessageService({})
                }
            ]
        }),
        moduleMetadata({
            imports: [BrowserAnimationsModule, ReactiveFormsModule],
            providers: [
                HostFolderFiledStore,
                {
                    provide: DotEditContentService,
                    useValue: {
                        getSitesTreePath: () => of(TREE_SELECT_MOCK)
                        // getFoldersTreeNode: () => of([])
                    }
                }
            ]
        }),
        componentWrapperDecorator(
            (story) => `<div class="card flex h-[25rem] justify-center w-full">${story}</div>`
        )
    ],
    render: (args) => ({
        props: {
            ...args,
            form: new FormGroup({
                [args.field.variable]: new FormControl('')
            })
        },
        template: `
            <form [formGroup]="form" class="w-full flex flex-col">
                <div class="flex items-center">
                    <dot-edit-content-host-folder-field
                        [formControlName]="field.variable"
                        ${argsToTemplate(args)} />
                </div>
            </form>
        `
    })
};
export default meta;

type Story = StoryObj<Args>;

export const Default: Story = {
    args: {
        field: { ...HOST_FOLDER_TEXT_MOCK }
    }
};
