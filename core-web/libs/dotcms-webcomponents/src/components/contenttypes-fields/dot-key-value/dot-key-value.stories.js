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
        label: 'Key and value',
        hint: 'Hello I am a hint',
        required: false,
        disabled: false,
        formKeyPlaceholder: 'Add a key',
        formValuePlaceholder: 'Add a value',
        formKeyLabel: 'Key',
        formValueLabel: 'Value',
        formAddButtonLabel: 'Add',
        listDeleteLabel: 'Remove'
    }
};

const Template = (args) => {
    const keyvalue = document.createElement('dot-key-value');

    for (const item in args) {
        keyvalue[item] = args[item];
    }

    return keyvalue;
};

export const KeyValue = Template.bind({});
