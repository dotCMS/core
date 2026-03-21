import '@testing-library/jest-dom';

import { buildSlots } from '../../utils/buildSlots';

const makeContainers = (contentlets: object[]) => ({
    'container-1': {
        contentlets: {
            'uuid-1': contentlets
        }
    }
});

describe('buildSlots', () => {
    test('returns empty record when no server components provided', () => {
        const containers = makeContainers([{ identifier: 'abc', contentType: 'Blog' }]);
        const result = buildSlots(containers, {});
        expect(result).toEqual({});
    });

    test('builds a slot for each contentlet matching a server component', () => {
        const BlogComponent = ({ title }: any) => <div>{title}</div>;
        const containers = makeContainers([
            { identifier: 'id-1', contentType: 'Blog', title: 'Hello' }
        ]);

        const result = buildSlots(containers, { Blog: BlogComponent });
        expect(result).toHaveProperty('id-1');
    });

    test('skips contentlets with no matching component type', () => {
        const BlogComponent = () => <div />;
        const containers = makeContainers([
            { identifier: 'id-1', contentType: 'Blog' },
            { identifier: 'id-2', contentType: 'News' }
        ]);

        const result = buildSlots(containers, { Blog: BlogComponent });
        expect(result).toHaveProperty('id-1');
        expect(result).not.toHaveProperty('id-2');
    });

    test('builds slots across multiple containers', () => {
        const BlogComponent = () => <div />;
        const containers = {
            'container-1': {
                contentlets: { 'uuid-1': [{ identifier: 'id-1', contentType: 'Blog' }] }
            },
            'container-2': {
                contentlets: { 'uuid-2': [{ identifier: 'id-2', contentType: 'Blog' }] }
            }
        };

        const result = buildSlots(containers, { Blog: BlogComponent });
        expect(result).toHaveProperty('id-1');
        expect(result).toHaveProperty('id-2');
    });

    test('returns empty record when containers is empty', () => {
        const result = buildSlots({}, { Blog: () => <div /> });
        expect(result).toEqual({});
    });
});
