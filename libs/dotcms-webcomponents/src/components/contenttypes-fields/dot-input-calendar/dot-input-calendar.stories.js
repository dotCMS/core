import readme from './readme.md';
import { withKnobs, text, boolean } from '@storybook/addon-knobs';

export default {
    title: 'Components/Content Types Fields',
    decorators: [withKnobs],
    parameters: {
        notes: readme
    }
};

export const InputCalendar = () => {
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
            name: 'required',
            content: boolean('Required', false)
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
            name: 'type',
            content: text('Type', '')
        }
    ];

    const inputcalendar = document.createElement('dot-input-calendar');

    props.forEach(({ name, content }) => {
        inputcalendar[name] = content;
    });

    return inputcalendar;
};
