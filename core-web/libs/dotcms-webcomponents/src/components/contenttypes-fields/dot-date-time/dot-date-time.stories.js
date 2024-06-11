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
        name: 'DateTime',
        label: 'DateTime',
        hint: 'This is help information',
        required: false,
        requiredMessage: 'This is a required field',
        validationMessage: 'This is a validation message',
        disabled: false,
        min: '',
        max: '',
        step: '',
        dateLabel: '',
        timeLabel: ''
    }
};

const Template = (args) => {
    const datetime = document.createElement('dot-date-time');

    for (const item in args) {
        datetime[item] = args[item];
    }

    return datetime;
};

export const DateTime = Template.bind({});
