import { Config } from '@stencil/core';
import { sass } from '@stencil/sass';

export const config: Config = {
    namespace: 'dotcms-webcomponents',
    taskQueue: 'async',

    outputTargets: [
        {
            type: 'dist',
            esmLoaderPath: '../loader',
            dir: '../../dist/libs/dotcms-webcomponents/dist'
        },
        {
            type: 'docs-readme'
        },
        {
            type: 'www',
            dir: '../../dist/libs/dotcms-webcomponents/www',
            serviceWorker: null // disable service workers
        }
    ],
    testing: {
        reporters: [
            'default',
            [
                'jest-html-reporters',
                {
                    publicPath: 'test-reports/dotcms-webcomponents',
                    filename: 'report.html',
                    openReport: false
                }
            ]
        ]
    },

    plugins: [sass()]
};
