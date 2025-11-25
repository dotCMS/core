import {
    moduleMetadata,
    StoryObj,
    Meta,
    applicationConfig,
    argsToTemplate
} from '@storybook/angular';
import { of } from 'rxjs';

import { provideHttpClient } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { DotMessageService, DotUploadFileService } from '@dotcms/data-access';
import { DotCMSContentTypeField } from '@dotcms/dotcms-models';

import { DotEditContentFileFieldComponent } from './dot-edit-content-file-field.component';
import { UIMessage } from './models';
import { DotFileFieldUploadService } from './services/upload-file/upload-file.service';
import { FileFieldStore } from './store/file-field.store';
import { MessageServiceMock } from './utils/mocks';

import { DotEditContentService } from '../../services/dot-edit-content.service';
import {
    BINARY_FIELD_MOCK,
    FILE_FIELD_MOCK,
    IMAGE_FIELD_MOCK,
    NEW_FILE_MOCK
} from '../../utils/mocks';

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
            providers: [
                {
                    provide: DotUploadFileService,
                    useValue: {}
                },
                {
                    provide: DotEditContentService,
                    useValue: {}
                },
                {
                    provide: DotFileFieldUploadService,
                    useValue: {
                        uploadDotAsset: () => of(NEW_FILE_MOCK.entity),
                        getContentById: () => of(NEW_FILE_MOCK.entity)
                    }
                },
                FileFieldStore
            ]
        })
    ],
    render: (args) => {
        const { value, ...newArgs } = args;

        return {
            props: {
                ...newArgs,
                value
            },
            template: `
                <dot-edit-content-file-field ${argsToTemplate(newArgs)} [(ngModel)]="value" />
                <p>Current value: {{ value }}</p>
            `
        };
    }
};
export default meta;

type Story = StoryObj<Args>;

export const FileField: Story = {
    args: {
        value: '',
        field: { ...FILE_FIELD_MOCK }
    }
};

export const BinaryField: Story = {
    args: {
        value: '',
        field: { ...BINARY_FIELD_MOCK }
    }
};

export const ImageField: Story = {
    args: {
        value: '',
        field: { ...IMAGE_FIELD_MOCK }
    }
};

export const ResposiveFileField: Story = {
    args: {
        value: '',
        field: { ...FILE_FIELD_MOCK }
    },
    render: (args) => {
        const { value, ...newArgs } = args;

        return {
            props: {
                ...newArgs,
                value
            },
            template: `
                <div class="w-20rem">
                    <dot-edit-content-file-field ${argsToTemplate(newArgs)} [(ngModel)]="value" />
                    <p>Current value: {{ value }}</p>
                </div>
            `
        };
    }
};
