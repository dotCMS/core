import readme from './readme.md';
import { withKnobs, text, boolean } from '@storybook/addon-knobs';

export default {
    title: 'Components/Content Types Fields',
    decorators: [withKnobs],
    parameters: {
        notes: readme
    }
};

export const DateTime = () => {
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
            name: 'min',
            content: text('Min', '')
        },
        {
            name: 'max',
            content: text('Max', '')
        },
        {
            name: 'step',
            content: text('Step', '')
        },
        {
            name: 'dateLabel',
            content: text('Date Label', 'Date')
        },
        {
            name: 'timeLabel',
            content: text('Time Label', 'Time')
        }
    ];

    const datetime = document.createElement('dot-date-time');

    props.forEach(({ name, content }) => {
        datetime[name] = content;
    });

    return datetime;
};
