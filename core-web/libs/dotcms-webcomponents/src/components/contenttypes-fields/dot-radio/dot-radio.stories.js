import readme from './readme.md';

export default {
    title: 'Components/Content Types Fields',
    parameters: {
        docs: {
            page: readme
        }
    },
    args: {
        name: '',
        label: 'Radio',
        hint: 'Hello I am a hint',
        options: 'Pizza|pizza,Burguer|burguer,Sushi|sushi',
        required: false,
        disabled: false,
        requiredMessage: 'This field is required',
        value: ''
    }
};

const Template = (args) => {
    const checkbox = document.createElement('dot-radio');

    for (const item in args) {
        checkbox[item] = args[item];
    }

    return checkbox;
};

export const Radio = Template.bind({});
