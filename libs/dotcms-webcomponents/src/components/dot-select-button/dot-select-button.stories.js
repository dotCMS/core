import readme from "./readme.md";
import {text} from "@storybook/addon-knobs";


export default {
    title: 'Components',
    parameters: {
        notes: readme
    }
};

const optionsMock  = [
    {
        label: 'Code',
        icon: 'code',
    },

    {
        label: 'Backup',
        icon: 'backup',
        disabled: true
    },
    {
        label: 'Help',
        icon: 'help',
    },

]


export const SelectButton = () => {

    const props = [
        {
            name: 'value',
            content: text('Selected', 'code')
        }
    ];


    const dotSelectButton = document.createElement('dot-select-button');

    dotSelectButton.options = optionsMock;

    props.forEach(({ name, content }) => {
        dotSelectButton[name] = content;
    });

    return dotSelectButton;

}
