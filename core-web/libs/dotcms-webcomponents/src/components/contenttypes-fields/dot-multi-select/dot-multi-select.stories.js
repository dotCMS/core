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
        label: 'Multi Select',
        hint: 'Hello I am a hint',
        options: 'Pizza|pizza,Burguer|burguer,Sushi|sushi',
        required: false,
        disabled: false,
        reuiredMessage: 'This field is required',
        size: ''
    }
};

const Template = (args) => {
    const multiselect = document.createElement('dot-multi-select');

    for (const item in args) {
        multiselect[item] = args[item];
    }

    return multiselect;
};

export const MultiSelect = Template.bind({});
