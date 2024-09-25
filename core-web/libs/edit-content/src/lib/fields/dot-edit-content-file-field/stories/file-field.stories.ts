import {
    moduleMetadata,
    StoryObj,
    Meta,
    applicationConfig,
    argsToTemplate
} from '@storybook/angular';

import { provideHttpClient } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { DotMessageService } from '@dotcms/data-access';
import { DotCMSContentTypeField } from '@dotcms/dotcms-models';

import { FILE_FIELD_MOCK } from '../../../utils/mocks';
import { DotEditContentFileFieldComponent } from '../dot-edit-content-file-field.component';
import { UIMessage } from '../models';
import { FileFieldStore } from '../store/file-field.store';
import { MessageServiceMock } from '../utils/mocks';

type Args = DotEditContentFileFieldComponent & {
    field: DotCMSContentTypeField;
    value: string;
    uiMessage?: UIMessage;
};

const meta: Meta<Args> = {
    title: 'Library / Edit Content / File Field',
    component: DotEditContentFileFieldComponent,
    decorators: [
        applicationConfig({
            providers: [
                provideHttpClient(),
                {
                    provide: DotMessageService,
                    useValue: MessageServiceMock
                }
            ]
        }),
        moduleMetadata({
            imports: [BrowserAnimationsModule, FormsModule],
            providers: [FileFieldStore]
        })
    ],
    render: (args) => ({
        props: args,
        template: `
            <dot-edit-content-file-field ${argsToTemplate(args)} [(ngModel)]="value" />
            <p>Current value: {{ value }}</p>
        `
    })
};
export default meta;

type Story = StoryObj<Args>;

export const FileField: Story = {
    args: {
        value: '',
        field: { ...FILE_FIELD_MOCK }
    }
};

export const ResposiveFileField: Story = {
    args: {
        value: '',
        field: { ...FILE_FIELD_MOCK }
    },
    render: (args) => ({
        props: args,
        template: `
            <div class="w-20rem">
                <dot-edit-content-file-field ${argsToTemplate(args)} [(ngModel)]="value" />
                <p>Current value: {{ value }}</p>
            </div>
        `
    })
};

export const InvalidFile: Story = {
    args: {
        value: '',
        field: { ...FILE_FIELD_MOCK },
        uiMessage: {
            message: 'dot.file.field.drag.and.drop.error.file.not.supported.message',
            severity: 'error',
            icon: 'pi pi-exclamation-triangle'
        }
    },
    render: (args) => ({
        props: args,
        template: `
            <dot-edit-content-file-field ${argsToTemplate(args)} [(ngModel)]="value" />
            <p>Current value: {{ value }}</p>
        `
    })
};