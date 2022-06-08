import readme from './readme.md';

export default {
    title: 'Components/Content Types Fields',
    parameters: {
        docs: {
            page: readme
        }
    },
    args: {
        label: 'Date Range',
        value: '',
        name: 'Name',
        hint: 'This is a hint',
        min: '',
        max: '',
        required: false,
        requiredMessage: 'This is a required message',
        disabled: false,
        displayFormat: 'MM/DD/YYYY'
    }
};

const Template = (args) => {
    const daterange = document.createElement('dot-date-range');

    for (const item in args) {
        daterange[item] = args[item];
    }

    return daterange;
};

export const DateRange = Template.bind({});
