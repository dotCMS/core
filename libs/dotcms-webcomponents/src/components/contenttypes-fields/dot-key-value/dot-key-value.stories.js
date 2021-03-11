import readme from './readme.md';
import { withKnobs, text, boolean } from '@storybook/addon-knobs';

export default {
    title: 'Components/Content Types Fields',
    decorators: [withKnobs],
    parameters: {
        notes: readme
    }
};

export const KeyValue = () => {
    const props = [
        {
            name: 'value',
            content: text('Value', '')
        },
        {
            name: 'name',
            content: text('Name', '')
        },
        {
            name: 'label',
            content: text('Label', 'Label')
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
            name: 'disabled',
            content: boolean('Disabled', false)
        },
        {
            name: 'formKeyPlaceholder',
            content: text('Form Key Placeholder', 'Add a key')
        },
        {
            name: 'formValuePlaceholder',
            content: text('Form Value Placeholder', 'Add a value')
        },
        {
            name: 'formKeyLabel',
            content: text('Form Key Label', 'Key')
        },
        {
            name: 'formValueLabel',
            content: text('Form Value Label', 'Value')
        },
        {
            name: 'formAddButtonLabel',
            content: text('Form Add Button Label', 'Add')
        },
        {
            name: 'listDeleteLabel',
            content: text('List Delete Label', 'Remove')
        }
    ];

    const keyvalue = document.createElement('dot-key-value');

    props.forEach(({ name, content }) => {
        keyvalue[name] = content;
    });

    return keyvalue;
};
