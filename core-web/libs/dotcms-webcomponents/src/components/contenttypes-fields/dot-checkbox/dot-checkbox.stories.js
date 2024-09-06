import readme from './readme.md';

export default {
    title: 'Components/Content Types Fields',
    parameters: {
        docs: {
            page: readme
        }
    },
    args: {
        name: 'Name',
        label: 'Select your food',
        hint: 'This is help text',
        options: 'Pizza|pizza,Burguer|burguer,Sushi|sushi',
        required: false,
        disabled: false,
        requiredMessage: 'Required Message',
        value: ''
    }
};

const Template = (args) => {
    const checkbox = document.createElement('dot-checkbox');

    for (const item in args) {
        checkbox[item] = args[item];
    }

    return checkbox;
};

export const Checkbox = Template.bind({});
