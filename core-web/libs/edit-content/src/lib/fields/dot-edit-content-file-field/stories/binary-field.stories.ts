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

import { BINARY_FIELD_MOCK } from '../../../utils/mocks';
import { DotEditContentFileFieldComponent } from '../dot-edit-content-file-field.component';
import { FileFieldStore } from '../store/file-field.store';
import { MessageServiceMock } from '../utils/mocks';

type Args = DotEditContentFileFieldComponent & {
    field: DotCMSContentTypeField;
    value: string;
};

const meta: Meta<Args> = {
    title: 'Library / Edit Content / New Binary Field',
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

export const BinaryField: Story = {
    args: {
        value: '',
        field: { ...BINARY_FIELD_MOCK }
    }
};

export const ResposiveBinaryField: Story = {
    args: {
        value: '',
        field: { ...BINARY_FIELD_MOCK }
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