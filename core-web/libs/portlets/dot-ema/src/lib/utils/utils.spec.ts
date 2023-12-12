import { deleteContentletFromContainer, insertContentletInContainer } from '.';

describe('utils functions', () => {
    describe('deleteContentletFromContainer', () => {
        it('should delete a contentlet from a container', () => {
            // Current page
            const pageContainers = [
                {
                    identifier: 'test',
                    acceptTypes: 'test',
                    uuid: 'test',
                    contentletsId: ['test']
                }
            ];

            // Container where we want to delete the contentlet
            const container = {
                identifier: 'test',
                acceptTypes: 'test',
                uuid: 'test',
                contentletsId: ['test']
            };

            // Contentlet to delete

            const contentletID = 'test';

            const result = deleteContentletFromContainer({
                pageContainers,
                container,
                contentletID
            });

            expect(result).toEqual([
                {
                    identifier: 'test',
                    acceptTypes: 'test',
                    uuid: 'test',
                    contentletsId: []
                }
            ]);
        });

        it('should not delete a contentlet from a container if the id does not match', () => {
            // Current page
            const pageContainers = [
                {
                    identifier: 'test',
                    acceptTypes: 'test',
                    uuid: 'test',
                    contentletsId: ['test']
                }
            ];

            // Container where we want to delete the contentlet
            const container = {
                identifier: 'test',
                acceptTypes: 'test',
                uuid: 'test',
                contentletsId: ['test']
            };

            // Contentlet to delete
            const contentletID = 'test2';

            const result = deleteContentletFromContainer({
                pageContainers,
                container,
                contentletID
            });

            expect(result).toEqual([
                {
                    identifier: 'test',
                    acceptTypes: 'test',
                    uuid: 'test',
                    contentletsId: ['test']
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
                    acceptTypes: 'test',
                    uuid: 'test',
                    contentletsId: ['test']
                }
            ];

            // Container where we want to insert the contentlet
            const container = {
                identifier: 'test',
                acceptTypes: 'test',
                uuid: 'test',
                contentletsId: ['test']
            };

            // Contentlet to insert
            const contentletID = 'test2';

            const result = insertContentletInContainer({
                pageContainers,
                container,
                contentletID
            });

            expect(result).toEqual([
                {
                    identifier: 'test',
                    acceptTypes: 'test',
                    uuid: 'test',
                    contentletsId: ['test', 'test2']
                }
            ]);
        });

        it('should not insert a contentlet in a container if the id already exists', () => {
            // Current Page
            const pageContainers = [
                {
                    identifier: 'test',
                    acceptTypes: 'test',
                    uuid: 'test',
                    contentletsId: ['test']
                }
            ];

            // Container where we want to insert the contentlet
            const container = {
                identifier: 'test',
                acceptTypes: 'test',
                uuid: 'test',
                contentletsId: ['test']
            };

            // Contentlet to insert
            const contentletID = 'test';

            const result = insertContentletInContainer({
                pageContainers,
                container,
                contentletID
            });

            expect(result).toEqual([
                {
                    identifier: 'test',
                    acceptTypes: 'test',
                    uuid: 'test',
                    contentletsId: ['test']
                }
            ]);
        });
    });
});
