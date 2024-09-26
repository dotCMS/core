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

import { DotEditContentService } from '../../../services/dot-edit-content.service';
import { IMAGE_FIELD_MOCK, NEW_FILE_MOCK } from '../../../utils/mocks';
import { DotEditContentFileFieldComponent } from '../dot-edit-content-file-field.component';
import { DotFileFieldUploadService } from '../services/upload-file/upload-file.service';
import { FileFieldStore } from '../store/file-field.store';
import { MessageServiceMock } from '../utils/mocks';

type Args = DotEditContentFileFieldComponent & {
    field: DotCMSContentTypeField;
    value: string;
};

const meta: Meta<Args> = {
    title: 'Library / Edit Content / Image Field',
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

export const ImageField: Story = {
    args: {
        value: '',
        field: { ...IMAGE_FIELD_MOCK }
    }
};

export const ResposiveImageField: Story = {
    args: {
        value: '',
        field: { ...IMAGE_FIELD_MOCK }
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
