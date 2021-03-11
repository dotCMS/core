import { withKnobs, text } from '@storybook/addon-knobs';
import readme from './readme.md';

export default {
    title: 'Elements/Contentlet Lock Icon',
    decorators: [withKnobs],
    parameters: {
        notes: readme
    }
};

export const Lock = () => {
    const props = [
        {
            name: 'contentlet',
            content: text('Contentlet', {
                locked: 'true'
            })
        }
    ];

    const contentletLockIcon = document.createElement('dot-contentlet-lock-icon');

    props.forEach(({ name, content }) => {
        contentletLockIcon[name] = content;
    });

    return contentletLockIcon;
};

export const Unlock = () => {
    const props = [
        {
            name: 'contentlet',
            content: text('Contentlet', {
                locked: 'false'
            })
        }
    ];

    const contentletLockIcon = document.createElement('dot-contentlet-lock-icon');

    props.forEach(({ name, content }) => {
        contentletLockIcon[name] = content;
    });

    return contentletLockIcon;
};

