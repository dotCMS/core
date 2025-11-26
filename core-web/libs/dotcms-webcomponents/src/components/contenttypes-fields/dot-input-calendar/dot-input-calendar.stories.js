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
        name: 'InputCalendar',
        required: false,
        disabled: false,
        min: '',
        max: '',
        step: '',
        type: 'date'
    }
};

export const InputCalendar = (args) => {
    const inputcalendar = document.createElement('dot-input-calendar');

    for (const item in args) {
        inputcalendar[item] = args[item];
    }

    return inputcalendar;
};
