jest.mock('../http', () => ({
    graphql: jest.fn()
}));

import { graphql } from '../http';
import { resolveIdentifiersOnServer } from '../resolve';

const mockGraphql = graphql as jest.Mock;
const mockClient = jest.fn() as unknown as Parameters<typeof resolveIdentifiersOnServer>[0];

describe('resolveIdentifiersOnServer', () => {
    beforeEach(() => {
        jest.clearAllMocks();
    });

    it('should return empty map for empty identifiers list', async () => {
        const result = await resolveIdentifiersOnServer(mockClient, 'Blog', []);
        expect(result.size).toBe(0);
        expect(mockGraphql).not.toHaveBeenCalled();
    });

    it('should resolve found identifiers with inode', async () => {
        mockGraphql.mockResolvedValue({
            BlogCollection: [
                { identifier: 'id-1', inode: 'inode-1' },
                { identifier: 'id-2', inode: 'inode-2' }
            ]
        });

        const result = await resolveIdentifiersOnServer(mockClient, 'Blog', ['id-1', 'id-2']);

        expect(result.get('id-1')).toEqual({ identifier: 'id-1', inode: 'inode-1' });
        expect(result.get('id-2')).toEqual({ identifier: 'id-2', inode: 'inode-2' });
    });

    it('should return null for identifiers not found on server', async () => {
        mockGraphql.mockResolvedValue({
            BlogCollection: [{ identifier: 'id-1', inode: 'inode-1' }]
        });

        const result = await resolveIdentifiersOnServer(mockClient, 'Blog', [
            'id-1',
            'id-not-found'
        ]);

        expect(result.get('id-1')).toEqual({ identifier: 'id-1', inode: 'inode-1' });
        expect(result.get('id-not-found')).toBeNull();
    });

    it('should deduplicate identifiers', async () => {
        mockGraphql.mockResolvedValue({
            BlogCollection: [{ identifier: 'id-1', inode: 'inode-1' }]
        });

        const result = await resolveIdentifiersOnServer(mockClient, 'Blog', [
            'id-1',
            'id-1',
            'id-1'
        ]);

        expect(mockGraphql).toHaveBeenCalledTimes(1);
        expect(result.get('id-1')).toEqual({ identifier: 'id-1', inode: 'inode-1' });
    });

    it('should handle batch errors gracefully (treat as not found)', async () => {
        mockGraphql.mockRejectedValue(new Error('Network error'));

        const result = await resolveIdentifiersOnServer(mockClient, 'Blog', ['id-1', 'id-2']);

        expect(result.get('id-1')).toBeNull();
        expect(result.get('id-2')).toBeNull();
    });

    it('should build correct GraphQL query with Lucene OR syntax', async () => {
        mockGraphql.mockResolvedValue({ BlogCollection: [] });

        await resolveIdentifiersOnServer(mockClient, 'Blog', ['id-1', 'id-2']);

        expect(mockGraphql).toHaveBeenCalledWith(
            mockClient,
            expect.stringContaining('+identifier:(id-1 OR id-2)')
        );
        expect(mockGraphql).toHaveBeenCalledWith(
            mockClient,
            expect.stringContaining('BlogCollection')
        );
    });

    it('should handle empty collection response', async () => {
        mockGraphql.mockResolvedValue({ BlogCollection: [] });

        const result = await resolveIdentifiersOnServer(mockClient, 'Blog', ['id-1']);

        expect(result.get('id-1')).toBeNull();
    });

    it('should handle missing collection key in response', async () => {
        mockGraphql.mockResolvedValue({});

        const result = await resolveIdentifiersOnServer(mockClient, 'Blog', ['id-1']);

        expect(result.get('id-1')).toBeNull();
    });

    it('should split large sets into batches of 50', async () => {
        // Create 75 unique identifiers
        const identifiers = Array.from({ length: 75 }, (_, i) => `id-${i}`);

        mockGraphql
            .mockResolvedValueOnce({
                BlogCollection: identifiers.slice(0, 50).map((id) => ({
                    identifier: id,
                    inode: `inode-${id}`
                }))
            })
            .mockResolvedValueOnce({
                BlogCollection: identifiers.slice(50).map((id) => ({
                    identifier: id,
                    inode: `inode-${id}`
                }))
            });

        const result = await resolveIdentifiersOnServer(mockClient, 'Blog', identifiers);

        // Should make 2 batch calls
        expect(mockGraphql).toHaveBeenCalledTimes(2);
        // All 75 should be resolved
        expect(result.size).toBe(75);
        expect(result.get('id-0')).toEqual({ identifier: 'id-0', inode: 'inode-id-0' });
        expect(result.get('id-74')).toEqual({ identifier: 'id-74', inode: 'inode-id-74' });
    });

    it('should handle partial batch failure (first succeeds, second fails)', async () => {
        const identifiers = Array.from({ length: 75 }, (_, i) => `id-${i}`);

        mockGraphql
            .mockResolvedValueOnce({
                BlogCollection: identifiers.slice(0, 50).map((id) => ({
                    identifier: id,
                    inode: `inode-${id}`
                }))
            })
            .mockRejectedValueOnce(new Error('Timeout'));

        const result = await resolveIdentifiersOnServer(mockClient, 'Blog', identifiers);

        // First batch resolved, second batch treated as null
        expect(result.get('id-0')).toEqual({ identifier: 'id-0', inode: 'inode-id-0' });
        expect(result.get('id-49')).toEqual({ identifier: 'id-49', inode: 'inode-id-49' });
        expect(result.get('id-50')).toBeNull();
        expect(result.get('id-74')).toBeNull();
    });
});
