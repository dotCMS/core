import { withKnobs, object } from '@storybook/addon-knobs';
import { withActions } from '@storybook/addon-actions';

import readme from './readme.md';
export default {
    title: 'Components',
    decorators: [withKnobs, withActions('valueChange')],
    parameters: {
        notes: readme
    }
};
export const ContentletCard = () => {
    const props = [
        {
            name: 'item',
            content: object('Item', {
                data: {
                    title: 'Hola Mundo',
                    language: 'es-es',
                    locked: 'true'
                },
                actions: [
                    {
                        label: 'Action 1',
                        action: (e) => {
                            console.log(e);
                        }
                    },
                    {
                        label: 'Action 2',
                        action: (e) => {
                            console.log(e);
                        }
                    }
                ]
            })
        }
    ];

    const cardContentlet = document.createElement('dot-card-contentlet');
    props.forEach(({ name, content }) => {
        cardContentlet[name] = content;
    });

    return cardContentlet;
};
