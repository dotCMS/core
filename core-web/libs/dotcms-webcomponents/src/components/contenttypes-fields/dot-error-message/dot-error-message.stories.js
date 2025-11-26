import readme from './readme.md';

export default {
    title: 'Components/Content Types Fields',
    parameters: {
        docs: {
            page: readme
        }
    },
    args: {
        content: 'This is the error message'
    }
};

const Template = (args) => {
    const datetime = document.createElement('dot-date-time');

    for (const item in args) {
        datetime[item] = args[item];
    }

    return `<dot-error-message>${args.content}</dot-error-message>`;
};

export const ErrorMessage = Template.bind({});
