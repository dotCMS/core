import { withKnobs, text, boolean } from '@storybook/addon-knobs';
import readme from './readme.md';

export default {
    title: 'Components/Content Types Fields',
    decorators: [withKnobs],
    parameters: {
        notes: readme
    }
};

export const TextArea = () => {
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
            name: 'required',
            content: boolean('Required', false)
        },
        {
            name: 'requiredMessage',
            content: text('Required Message', '')
        },
        {
            name: 'validationMessage',
            content: text('Validation Message', '')
        },
        {
            name: 'disabled',
            content: boolean('Disabled', false)
        },
        {
            name: 'regexCheck',
            content: text('Regex Check', '')
        }
    ];

    const textarea = document.createElement('dot-textarea');

    props.forEach(({ name, content }) => {
        textarea[name] = content;
    });

    return textarea;
};
