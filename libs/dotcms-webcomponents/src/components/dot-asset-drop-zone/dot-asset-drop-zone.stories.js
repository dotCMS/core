import { withKnobs, text, boolean } from '@storybook/addon-knobs';
import readme from './readme.md';

export default {
    title: 'Content Types Fields',
    decorators: [withKnobs],
    parameters: {
        notes: readme
    }
};

export const DopZone = () => {
    const props = [
        {
            name: 'Text',
            content: text('Value', '')
        }
    ];
    const body = document.createElement('div');
    const dropZone = document.createElement('dot-asset-drop-zone');

    props.forEach(({ name, content }) => {
        dropZone[name] = content;
    });

    body.appendChild(dropZone);

    body.style.width = '600px';
    body.style.height = '600px';
    body.style.border = '3px solid green';

    return body;
};
