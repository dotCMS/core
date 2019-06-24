import { Config } from '@stencil/core';
import { sass } from '@stencil/sass';

export const config: Config = {
    namespace: 'dotcmsFields',
    copy: [{ src: './dot-form.html', dest: '../www/dot-form.html' }],
    outputTargets: [
        { type: 'dist' },
        { type: 'docs' },
        {
            type: 'www',
            serviceWorker: null // disable service workers
        }
    ],
    plugins: [sass()]
};
