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

import { DotEditContentFileFieldComponent } from './dot-edit-content-file-field.component';
import { FileFieldStore } from './store/file-field.store';
import { MessageServiceMock } from './utils/mocks';

import { FILE_FIELD_MOCK, IMAGE_FIELD_MOCK, BINARY_FIELD_MOCK } from '../../utils/mocks';

type Args = DotEditContentFileFieldComponent & {
    field: DotCMSContentTypeField;
    value: string;
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

export const ImageField: Story = {
    args: {
        value: '',
        field: { ...IMAGE_FIELD_MOCK }
    }
};

export const BinaryField: Story = {
    args: {
        value: '',
        field: { ...BINARY_FIELD_MOCK }
    }
};
