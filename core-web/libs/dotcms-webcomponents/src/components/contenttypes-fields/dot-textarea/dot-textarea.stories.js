import readme from './readme.md';

export default {
    title: 'Components/Content Types Fields',
    parameters: {
        docs: {
            page: readme
        }
    },
    args: {
        value: '',
        name: '',
        label: 'Textarea',
        hint: 'Hello I am a hint',
        required: false,
        requireMessage: 'This field is required',
        validationMessage: 'This field is invalid',
        disabled: false,
        regexCheck: ''
    }
};

const Template = (args) => {
    const textarea = document.createElement('dot-textarea');

    for (const item in args) {
        textarea[item] = args[item];
    }

    return textarea;
};

export const TextArea = Template.bind({});
