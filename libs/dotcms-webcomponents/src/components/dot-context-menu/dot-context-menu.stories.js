import { withKnobs, array } from '@storybook/addon-knobs';

import readme from './readme.md';
export default {
    title: 'Components',
    decorators: [withKnobs],
    parameters: {
        notes: readme
    }
};
export const ContextMenu = () => {
    const props = [
        {
            name: 'options',
            content: array('Options', [
                {
                    label: 'Publish',
                    action: (e) => {
                        console.log(e);
                    }
                },
                {
                    label: 'Archived',
                    action: (e) => {
                        console.log(e);
                    }
                }
            ])
        }
    ];

    const menu = document.createElement('dot-context-menu');
    props.forEach(({ name, content }) => {
        menu[name] = content;
    });

    return menu;
};
