import { withKnobs, text } from '@storybook/addon-knobs';
import readme from './readme.md';

export default {
    title: 'Elements',
    decorators: [withKnobs],
    parameters: {
        notes: readme
    }
};

export const ContentletIcon = () => {
    const props = [
        {
            name: 'icon',
            content: text('Icon', '')
        },

        {
            name: 'size',
            content: text('Size', '')
        }
    ];

    const contentletIcon = document.createElement('dot-contentlet-icon');

    props.forEach(({ name, content }) => {
        contentletIcon[name] = content;
    });

    return contentletIcon;
};
