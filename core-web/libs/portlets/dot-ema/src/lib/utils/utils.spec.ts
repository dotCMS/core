import { deleteContentletFromContainer, insertContentletInContainer } from '.';

describe('utils functions', () => {
    describe('delete contentlet from container', () => {
        it('should delete', () => {
            // Current page

            const result = deleteContentletFromContainer({
                pageId: 'test',
                language_id: 'test',
                container: {
                    identifier: 'test',
                    acceptTypes: 'test',
                    uuid: 'test',
                    contentletsId: ['test'],
                    maxContentlets: 1
                },
                pageContainers: [
                    {
                        identifier: 'test',
                        uuid: 'test',
                        contentletsId: ['test']
                    }
                ],
                contentlet: {
                    identifier: 'test',
                    inode: 'test',
                    title: 'test'
                },
                personaTag: 'test'
            });

            expect(result).toEqual([
                {
                    identifier: 'test',
                    uuid: 'test',
                    contentletsId: [],
                    personaTag: 'test'
                }
            ]);
        });

        it('should not delete if id not found', () => {
            // Current page
            const pageContainers = [
                {
                    identifier: 'test',
                    uuid: 'test',
                    contentletsId: ['test']
                }
            ];

            // Container where we want to delete the contentlet
            const container = {
                identifier: 'test',
                uuid: 'test',
                contentletsId: ['test'],
                maxContentlets: 1,
                acceptTypes: 'test'
            };

            // Contentlet to delete
            const contentlet = {
                identifier: 'test2',
                inode: 'test',
                title: 'test'
            };

            const result = deleteContentletFromContainer({
                pageContainers,
                container,
                contentlet,
                pageId: 'test',
                language_id: 'test'
            });

            expect(result).toEqual([
                {
                    identifier: 'test',
                    uuid: 'test',
                    contentletsId: ['test'],
                    personaTag: undefined
                }
            ]);
        });
    });

    describe('insert contentlet in container', () => {
        it('should insert the end', () => {
            // Current page
            const pageContainers = [
                {
                    identifier: 'test',
                    uuid: 'test',
                    contentletsId: ['test']
                }
            ];

            // Container where we want to insert the contentlet
            const container = {
                identifier: 'test',
                acceptTypes: 'test',
                uuid: 'test',
                contentletsId: ['test'],
                maxContentlets: 1
            };

            // Contentlet to insert
            const contentlet = {
                identifier: 'test2',
                inode: 'test',
                title: 'test'
            };

            const result = insertContentletInContainer({
                pageContainers,
                container,
                contentlet,
                pageId: 'test',
                language_id: 'test'
            });

            expect(result).toEqual([
                {
                    identifier: 'test',
                    uuid: 'test',
                    contentletsId: ['test', 'test2'],
                    personaTag: undefined
                }
            ]);
        });

        it('should insert in specific position', () => {
            // Current page
            const pageContainers = [
                {
                    identifier: 'test',
                    uuid: 'test',
                    contentletsId: ['test', 'test123', 'test1234']
                }
            ];

            // Container where we want to insert the contentlet
            const container = {
                identifier: 'test',
                acceptTypes: 'test',
                uuid: 'test',
                contentletsId: ['test'],
                maxContentlets: 1
            };

            // Contentlet to insert
            const contentlet = {
                identifier: 'test123',
                inode: 'test',
                title: 'test'
            };

            const result = insertContentletInContainer({
                pageContainers,
                container,
                contentlet,
                pageId: 'test',
                language_id: 'test',
                position: 'after',
                newContentletId: '000'
            });

            expect(result).toEqual([
                {
                    identifier: 'test',
                    uuid: 'test',
                    contentletsId: ['test', 'test123', '000', 'test1234'],
                    personaTag: undefined
                }
            ]);
        });

        it('should not insert contentlet if already exist', () => {
            // Current Page
            const pageContainers = [
                {
                    identifier: 'test',
                    uuid: 'test',
                    contentletsId: ['test']
                }
            ];

            // Container where we want to insert the contentlet
            const container = {
                identifier: 'test',
                acceptTypes: 'test',
                uuid: 'test',
                contentletsId: ['test'],
                maxContentlets: 1
            };

            // Contentlet to insert
            const contentlet = {
                identifier: 'test',
                inode: 'test',
                title: 'test'
            };

            const result = insertContentletInContainer({
                pageContainers,
                container,
                contentlet,
                language_id: 'test',
                pageId: 'test'
            });

            expect(result).toEqual([
                {
                    identifier: 'test',
                    uuid: 'test',
                    contentletsId: ['test']
                }
            ]);
        });
    });
});
