import { Config } from '@stencil/core';
import { sass } from '@stencil/sass';

export const config: Config = {
    namespace: 'dotcms-field-elements',
    taskQueue: 'async',

    outputTargets: [
        {
            type: 'dist',
            esmLoaderPath: '../loader',
            dir: '../../dist/libs/dotcms-field-elements/dist'
        },
        {
            type: 'docs-readme'
        },
        {
            type: 'www',
            dir: '../../dist/libs/dotcms-field-elements/www',
            serviceWorker: null // disable service workers
        }
    ],

    plugins: [sass()]
};
