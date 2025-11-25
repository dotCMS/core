import readme from './readme.md';

export default {
    title: 'Components/Content Types Fields',
    parameters: {
        docs: {
            page: readme
        }
    },
    args: {
        value: 'Value',
        name: 'Name',
        label: 'Date',
        hint: 'This is helpful information',
        required: false,
        requiredMessage: 'Required Message',
        disabled: false,
        min: '',
        max: '',
        step: ''
    }
};

const Template = (args) => {
    const date = document.createElement('dot-date');

    for (const item in args) {
        date[item] = args[item];
    }

    return date;
};

export const Date = Template.bind({});
