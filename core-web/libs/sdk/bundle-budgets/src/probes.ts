/**
 * The consumption patterns #37571 is about, and what must not come with them.
 *
 * `forbidden` entries are matched as substrings against every module esbuild resolved, so a
 * regression names the exact file that crept back in.
 */
export interface Probe {
    name: string;
    /** What a consumer writes. */
    source: string;
    /** Module path fragments that must be absent from the bundle. */
    forbidden: string[];
    /** SDK packages the probe needs staged. */
    packages: string[];
}

export const PROBES: Probe[] = [
    {
        name: 'react-layout-only',
        source: `import { DotCMSLayoutBody } from '@dotcms/react';\nexport { DotCMSLayoutBody };\n`,
        packages: ['react', 'uve', 'client', 'types'],
        forbidden: ['tinymce', 'DotCMSEditableText', 'BlockEditor', 'useAISearch']
    },
    {
        name: 'react-hook-only',
        source: `import { useEditableDotCMSPage } from '@dotcms/react';\nexport { useEditableDotCMSPage };\n`,
        packages: ['react', 'uve', 'client', 'types'],
        forbidden: [
            'tinymce',
            'DotCMSEditableText',
            'BlockEditor',
            'useAISearch',
            'DotCMSLayoutBody',
            '/Row/',
            '/Column/',
            '.css'
        ]
    },
    {
        name: 'analytics-neutral',
        source: `import { initializeContentAnalytics } from '@dotcms/analytics';\nexport { initializeContentAnalytics };\n`,
        packages: ['analytics', 'uve', 'types'],
        forbidden: ['/react/', 'next/navigation', 'useContentAnalytics', 'DotContentAnalytics']
    },
    {
        name: 'events-init',
        source: `import { events } from '@dotcms/events';\nexport { events };\n`,
        packages: ['events', 'uve', 'types'],
        forbidden: ['/react/', 'next/navigation', 'tinymce']
    },
    {
        // The React adapter brings the markup and the boot script builder, never the engine
        name: 'events-react',
        source: `import { DotCMSExperiment } from '@dotcms/events/react';\nexport { DotCMSExperiment };\n`,
        packages: ['events'],
        forbidden: ['@dotcms/events/index', '/analytics/', '@analytics/']
    },
    {
        name: 'uve-only',
        source: `import { getUVEState } from '@dotcms/uve';\nexport { getUVEState };\n`,
        packages: ['uve', 'types'],
        forbidden: ['tinymce', 'style-editor']
    }
];
