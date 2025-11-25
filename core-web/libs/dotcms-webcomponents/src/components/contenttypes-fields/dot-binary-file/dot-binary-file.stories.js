import readme from './readme.md';

export default {
    title: 'Components/Content Types Fields',
    parameters: {
        docs: {
            page: readme
        }
    },
    args: {
        label: 'Label',
        placeholder: 'Placeholder',
        hint: 'Hint',
        required: false,
        requiredMessage: 'Required Message',
        validationMessage: 'Validation Message',
        URLValidationMessage: 'URL Validation Message',
        disabled: false,
        maxFileLength: '',
        buttonLabel: 'Button Label',
        errorMessage: 'There is some error',
        previewImageName: 'Preview Image Name',
        previewImageUrl: ''
    }
};

const Template = (args) => {
    const binaryfield = document.createElement('dot-binary-file');

    for (const item in args) {
        binaryfield[item] = args[item];
    }

    return binaryfield;
};

export const BinaryField = Template.bind({});
