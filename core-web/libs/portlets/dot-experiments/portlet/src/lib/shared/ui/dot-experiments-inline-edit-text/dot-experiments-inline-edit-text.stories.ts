import { action } from '@storybook/addon-actions';
import { Meta, moduleMetadata, StoryObj, argsToTemplate } from '@storybook/angular';

import { DotMessageService } from '@dotcms/data-access';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotExperimentsInlineEditTextComponent } from './dot-experiments-inline-edit-text.component';

const messageServiceMock = new MockDotMessageService({
    'dot.common.inplace.empty.text': 'default message',
    'error.form.validator.maxlength': 'Max length message validator',
    'error.form.validator.required': 'Required message validator'
});

const meta: Meta<DotExperimentsInlineEditTextComponent> = {
    title: 'Library/Experiments/Components/InlineEditText',
    component: DotExperimentsInlineEditTextComponent,
    decorators: [
        moduleMetadata({
            providers: [
                {
                    provide: DotMessageService,
                    useValue: messageServiceMock
                }
            ]
        })
    ],
    args: {
        maxCharacterLength: 10,
        text: 'Lorem ipsum dolor sit amet'
    },
    argTypes: {
        text: {
            control: { type: 'text' },
            description: 'Text to be edited'
        },
        inputSize: {
            options: ['small', 'large'],
            control: { type: 'radio' },
            description: 'Size of the input field',
            defaultValue: 'small'
        },
        isLoading: {
            control: { type: 'boolean', default: false },
            description: 'Show loading/saving spinner and disable input',
            defaultValue: false
        },
        emptyTextMessage: {
            control: { type: 'text' },
            description: 'Text to be shown when the text is empty',
            defaultValue: 'dot.common.inplace.empty.text'
        },
        disabled: {
            control: { type: 'boolean' },
            description: 'Flag to disable the inplace',
            defaultValue: false
        },
        required: {
            control: { type: 'boolean' },
            description: 'Flag to make the text required',
            defaultValue: false
        },
        showErrorMsg: {
            control: { type: 'boolean' },
            description: 'Flag to hide the error message, only make the border red',
            defaultValue: true
        },
        textChanged: { action: 'textChanged' }
    },
    render: (args) => ({
        props: {
            ...args,
            textChanged: action('textChanged')
        },
        template: `<dot-experiments-inline-edit-text ${argsToTemplate(args)} />`
    })
};
export default meta;

type Story = StoryObj<DotExperimentsInlineEditTextComponent>;

export const Default: Story = {};
