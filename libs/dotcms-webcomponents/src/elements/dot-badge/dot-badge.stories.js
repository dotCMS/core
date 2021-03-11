import { withKnobs, text } from '@storybook/addon-knobs';
import readme from './readme.md';

export default {
    title: 'Elements',
    decorators: [withKnobs],
    parameters: {
        notes: readme
    }
};

export const Badge = () => {
    const props = [
        {
            name: 'innerText',
            content: text('Text', 'Hello World')
        },
        {
            name: 'color',
            content: text('Color', '#000')
        },
        {
            name: 'size',
            content: text('Font Size', '16px')
        },
        {
            name: 'bgColor',
            content: text('Background Color', 'lightblue')
        }
    ];

    const contentletLockIcon = document.createElement('dot-badge');

    props.forEach(({ name, content }) => {
        contentletLockIcon[name] = content;
    });

    return contentletLockIcon;
};
