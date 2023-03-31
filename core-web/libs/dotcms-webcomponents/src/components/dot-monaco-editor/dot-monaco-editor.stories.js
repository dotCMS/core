import readme from './readme.md';

export default {
    title: 'Components',
    parameters: {
        docs: {
            page: readme
        }
    },
    arg: {}
};

const Template = (args) => {
    const dotMonacoEidtor = document.createElement('dot-monaco-editor');

    for (const item in args) {
        dotMonacoEidtor[item] = args[item];
    }

    return dotMonacoEidtor;
};

export const dotMonacoEidtor = Template.bind({});
