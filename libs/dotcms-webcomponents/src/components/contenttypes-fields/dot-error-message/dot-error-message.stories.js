import readme from './readme.md';
import { withKnobs, text } from '@storybook/addon-knobs';

export default {
    title: 'Components/Content Types Fields',
    decorators: [withKnobs],
    parameters: {
        notes: readme
    }
};

export const ErrorMessage = () =>
    `<dot-error-message>${text(
        'Content',
        'This is an error message for fields and form'
    )}</dot-error-message>`;
