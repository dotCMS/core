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
        label: 'Tags',
        hint: 'Hello I am a hint',
        placeholder: 'The Placeholder',
        required: false,
        requireMessage: 'This field is required',
        disabled: false,
        threshold: 0,
        debounce: 300
    }
};

const Template = (args) => {
    const tags = document.createElement('dot-tags');

    for (const item in args) {
        tags[item] = args[item];
    }

    return tags;
};

export const Tags = Template.bind({});
