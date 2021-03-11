import readme from './readme.md';
import { withKnobs, text, boolean, number } from '@storybook/addon-knobs';

export default {
    title: 'Components/Content Types Fields',
    decorators: [withKnobs],
    parameters: {
        notes: readme
    }
};

export const Tags = () => {
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
            name: 'placeholder',
            content: text('Placeholder', 'The Placeholder')
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
            name: 'threshold',
            content: number('Threshold', 0)
        },
        {
            name: 'debounce',
            content: number('Debounce', 300)
        }
    ];

    const tags = document.createElement('dot-tags');

    props.forEach(({ name, content }) => {
        tags[name] = content;
    });

    return tags;
};
