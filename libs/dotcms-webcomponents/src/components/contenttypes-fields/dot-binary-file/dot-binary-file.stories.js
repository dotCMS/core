import { withKnobs, text, boolean } from '@storybook/addon-knobs';
import readme from './readme.md';

export default {
    title: 'Components/Content Types Fields',
    decorators: [withKnobs],
    parameters: {
        notes: readme
    }
};

export const BinaryField = () => {
    const props = [
        {
            name: 'name',
            content: text('Name', 'field-name')
        },
        {
            name: 'label',
            content: text('Label', 'Label')
        },
        {
            name: 'placeholder',
            content: text('Placeholder', 'This is a placeholder')
        },
        {
            name: 'hint',
            content: text('Hint', 'Hello Im a hint')
        },
        {
            name: 'required',
            content: boolean('Required', false)
        },
        {
            name: 'requiredMessage',
            content: text('Required Message', '')
        },
        {
            name: 'validationMessage',
            content: text('Validation Message', '')
        },
        {
            name: 'URLValidationMessage',
            content: text('URL Validation Message', '')
        },
        {
            name: 'disabled',
            content: boolean('Disabled', false)
        },
        {
            name: 'maxFileLength',
            content: text('Max File Length', '')
        },
        {
            name: 'buttonLabel',
            content: text('Button Label', 'Label of the button')
        },
        {
            name: 'errorMessage',
            content: text('Error Message', '')
        },
        {
            name: 'previewImageName',
            content: text('Preview Image Name', '')
        },
        {
            name: 'previewImageUrl',
            content: text('Preview Image Url', '')
        }
    ];

    const binaryfield = document.createElement('dot-binary-file');

    props.forEach(({ name, content }) => {
        binaryfield[name] = content;
    });

    return binaryfield;
};
