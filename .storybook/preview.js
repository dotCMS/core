import { setCompodocJson } from '@storybook/addon-docs/angular';
import docJson from '../documentation.json';
setCompodocJson(docJson);

export const parameters = {
    actions: { argTypesRegex: '^on[A-Z].*' },
    docs: {
        iframeHeight: 200
    },
    options: {
        storySort: {
            order: ['Getting Started', ['Introduction', 'Design Tokens'], 'DotCMS', 'PrimeNG'],
        },
    }
};
