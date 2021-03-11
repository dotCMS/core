import readme from './readme.md';
import { text } from '@storybook/addon-knobs';
export default {
    title: 'Elements',
    parameters: {
        notes: readme
    }
};

export const MaterialIconPicker = () => {
    const props = [
        {
            name: 'name',
            content: text('Name', 'dotMaterialIconPicker')
        },
        {
            name: 'value',
            content: text('value', 'account_balance')
        },
        {
            name: 'size',
            content: text('Font Size', '16px')
        },
        {
            name: 'showColor',
            content: text('Show Color Picker', 'true')
        }
    ];

    const dotMaterialIconPicker = document.createElement('dot-material-icon-picker');

    props.forEach(({ name, content }) => {
        dotMaterialIconPicker[name] = content;
    });

    return dotMaterialIconPicker;
};
