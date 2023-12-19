import { deleteContentletFromContainer, insertContentletInContainer } from '.';

describe('utils functions', () => {
    describe('deleteContentletFromContainer', () => {
        it('should delete a contentlet from a container', () => {
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

        it('should not delete a contentlet from a container if the id does not match', () => {
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

    describe('insertContentletInContainer', () => {
        it('should insert a contentlet in a container', () => {
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

        it('should not insert a contentlet in a container if the id already exists', () => {
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
