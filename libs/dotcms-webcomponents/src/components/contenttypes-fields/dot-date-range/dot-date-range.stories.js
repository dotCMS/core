import readme from './readme.md';
import { withKnobs, text, boolean } from '@storybook/addon-knobs';

export default {
    title: 'Components/Content Types Fields',
    decorators: [withKnobs],
    parameters: {
        notes: readme
    }
};

export const DateRange = () => {
    const props = [
        {
            name: 'value',
            content: text('Value', '')
        },
        {
            name: 'name',
            content: text('Name', 'field-name')
        },
        {
            name: 'label',
            content: text('Label', 'Label')
        },
        {
            name: 'hint',
            content: text('Hint', 'Hello Im a hint')
        },
        {
            name: 'min',
            content: text('Min', '')
        },
        {
            name: 'max',
            content: text('Max', '')
        },
        {
            name: 'required',
            content: boolean('Required', false)
        },
        {
            name: 'requiredMessage',
            content: text('Required Message', '')
        },
        {
            name: 'disabled',
            content: boolean('Disabled', false)
        },
        {
            name: 'displayFormat',
            content: text('Display Format', '')
        }
    ];

    const daterange = document.createElement('dot-date-range');

    props.forEach(({ name, content }) => {
        daterange[name] = content;
    });

    return daterange;
};
