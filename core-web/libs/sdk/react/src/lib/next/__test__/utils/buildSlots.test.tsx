import '@testing-library/jest-dom';

import { DotCMSPageAssetContainers } from '@dotcms/types';

import { buildSlots } from '../../utils/buildSlots';

/**
 * `buildSlots` only destructures `contentlets` off each container, so the fixture supplies
 * just that. The cast records what the test actually exercises instead of inventing a full
 * `DotCMSPageAssetContainer`.
 */
const makeContainers = (contentlets: object[]) =>
    ({
        'container-1': {
            contentlets: {
                'uuid-1': contentlets
            }
        }
    }) as unknown as DotCMSPageAssetContainers;

describe('buildSlots', () => {
    test('returns empty record when no server components provided', async () => {
        const containers = makeContainers([{ identifier: 'abc', contentType: 'Blog' }]);
        const result = await buildSlots(containers, {});
        expect(result).toEqual({});
    });

    test('builds a slot for each contentlet matching a server component', async () => {
        const BlogComponent = ({ title }: { title: string }) => <div>{title}</div>;
        const containers = makeContainers([
            { identifier: 'id-1', contentType: 'Blog', title: 'Hello' }
        ]);

        const result = await buildSlots(containers, { Blog: BlogComponent });
        expect(result).toHaveProperty('id-1');
        expect(result['id-1']).not.toBeNull();
    });

    test('resolves async server components', async () => {
        const AsyncComponent = async ({ title }: { title: string }) => <div>{title}</div>;
        const containers = makeContainers([
            { identifier: 'id-async', contentType: 'Blog', title: 'Async' }
        ]);

        const result = await buildSlots(containers, { Blog: AsyncComponent });
        expect(result).toHaveProperty('id-async');
        expect(result['id-async']).not.toBeNull();
    });

    test('skips contentlets with no matching component type', async () => {
        const BlogComponent = () => <div />;
        const containers = makeContainers([
            { identifier: 'id-1', contentType: 'Blog' },
            { identifier: 'id-2', contentType: 'News' }
        ]);

        const result = await buildSlots(containers, { Blog: BlogComponent });
        expect(result).toHaveProperty('id-1');
        expect(result).not.toHaveProperty('id-2');
    });

    test('skips contentlets with undefined identifier', async () => {
        const BlogComponent = () => <div />;
        const containers = makeContainers([{ contentType: 'Blog' }]); // no identifier

        const result = await buildSlots(containers, { Blog: BlogComponent });
        expect(result).not.toHaveProperty('undefined');
        expect(Object.keys(result)).toHaveLength(0);
    });

    test('builds slots across multiple containers', async () => {
        const BlogComponent = () => <div />;
        const containers = {
            'container-1': {
                contentlets: { 'uuid-1': [{ identifier: 'id-1', contentType: 'Blog' }] }
            },
            'container-2': {
                contentlets: { 'uuid-2': [{ identifier: 'id-2', contentType: 'Blog' }] }
            }
        } as unknown as DotCMSPageAssetContainers;

        const result = await buildSlots(containers, { Blog: BlogComponent });
        expect(result).toHaveProperty('id-1');
        expect(result).toHaveProperty('id-2');
    });

    test('returns empty record when containers is empty', async () => {
        const result = await buildSlots({}, { Blog: () => <div /> });
        expect(result).toEqual({});
    });
});
