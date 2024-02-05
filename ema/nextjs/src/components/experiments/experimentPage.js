'use client';
// TODO: meter esto en otro lugar

import ExperimentContent from '@/components/experiments/experimentContent';
import { createClient, JitsuProvider } from '@jitsu/react';

const host = process.env.NEXT_PUBLIC_DOTCMS_HOST;
const token = 'js.cluster1.customer1.tpw8gbup16ekrnasj9';
console.info('NEXT_PUBLIC_DOTCMS_HOST', host);
console.info('EXPERIMENT_TOKEN_APP', process.env.EXPERIMENT_TOKEN_APP);

const jitsuClient = createClient({
    tracking_host: `${host}`,
    key: `${token}`,
    log_level: 'DEBUG'
});

function PageWithExperiment({ children }) {
    return (
        <div>
            <JitsuProvider client={jitsuClient}>
                <ExperimentContent>{children}</ExperimentContent>
            </JitsuProvider>
        </div>
    );
}

export default PageWithExperiment;
