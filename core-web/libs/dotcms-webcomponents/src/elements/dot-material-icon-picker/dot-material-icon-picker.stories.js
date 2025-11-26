import readme from './readme.md';

export default {
    title: 'Elements',
    parameters: {
        docs: {
            title: 'Material Icon Picker',
            description: {
                component: 'A component to display the thumbnail of a contentlet'
            },
            page: readme
        }
    },
    args: {
        value: 'account_balance',
        size: '16px',
        showColor: ['true', 'false']
    }
};

const Template = (args) => {
    const dotMaterialIconPicker = document.createElement('dot-material-icon-picker');

    for (const item in args) {
        dotMaterialIconPicker[item] = args[item];
    }

    return dotMaterialIconPicker;
};

export const MaterialIconPicker = Template.bind({});
