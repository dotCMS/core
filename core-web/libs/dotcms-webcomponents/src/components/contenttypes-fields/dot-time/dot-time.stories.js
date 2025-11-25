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
        label: 'Time Field',
        hint: 'Hello I am a hint',
        required: false,
        requireMessage: 'This field is required',
        validationMessage: 'This field is invalid',
        disabled: false,
        min: '',
        max: '',
        step: ''
    }
};

const Template = (args) => {
    const time = document.createElement('dot-time');

    for (const item in args) {
        time[item] = args[item];
    }

    return time;
};

export const TimeField = Template.bind({});
