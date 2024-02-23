import { deleteContentletFromContainer, insertContentletInContainer, sanitizeURL } from '.';

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
                    maxContentlets: 1,
                    contentletsId: ['test']
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
                    title: 'test',
                    contentType: 'test'
                },
                personaTag: 'test',
                position: 'after'
            });

            expect(result).toEqual({
                pageContainers: [
                    {
                        identifier: 'test',
                        uuid: 'test',
                        contentletsId: [],
                        personaTag: 'test'
                    }
                ],
                contentletsId: []
            });
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
                title: 'test',
                contentType: 'test'
            };

            const result = deleteContentletFromContainer({
                pageContainers,
                container,
                contentlet,
                pageId: 'test',
                language_id: 'test',
                position: 'after'
            });

            expect(result).toEqual({
                pageContainers: [
                    {
                        identifier: 'test',
                        uuid: 'test',
                        contentletsId: ['test'],
                        personaTag: undefined
                    }
                ],
                contentletsId: ['test']
            });
        });
    });

    describe('insert contentlet in container', () => {
        it('should insert at the end', () => {
            // Current page
            const pageContainers = [
                {
                    identifier: 'container-identifier-123',
                    uuid: 'container-uui-123',
                    contentletsId: ['contentlet-mark-123']
                }
            ];

            // Container where we want to insert the contentlet
            const container = {
                identifier: 'container-identifier-123',
                acceptTypes: 'test',
                uuid: 'container-uui-123',
                contentletsId: ['contentlet-mark-123'],
                maxContentlets: 1
            };

            // Contentlet position mark
            const contentlet = {
                identifier: 'contentlet-mark-123',
                inode: 'contentlet-mark-inode-123',
                title: 'test',
                contentType: 'test'
            };

            const result = insertContentletInContainer({
                pageContainers,
                container,
                contentlet,
                pageId: 'page-id-123',
                language_id: '1',
                newContentletId: 'new-contentlet-id-123'
            });

            expect(result).toEqual({
                didInsert: true,
                pageContainers: [
                    {
                        identifier: 'container-identifier-123',
                        uuid: 'container-uui-123',
                        contentletsId: ['contentlet-mark-123', 'new-contentlet-id-123'],
                        personaTag: undefined
                    }
                ]
            });
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
                title: 'test',
                contentType: 'test'
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

            expect(result).toEqual({
                didInsert: true,
                pageContainers: [
                    {
                        identifier: 'test',
                        uuid: 'test',
                        contentletsId: ['test', 'test123', '000', 'test1234'],
                        personaTag: undefined
                    }
                ]
            });
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
                title: 'test',
                contentType: 'test'
            };

            const result = insertContentletInContainer({
                pageContainers,
                container,
                contentlet,
                newContentletId: 'test',
                language_id: 'test',
                pageId: 'test'
            });

            expect(result).toEqual({
                didInsert: false,
                pageContainers: [
                    {
                        identifier: 'test',
                        uuid: 'test',
                        contentletsId: ['test']
                    }
                ]
            });
        });
    });

    describe('url sanitize', () => {
        it('should remove the slash from the start', () => {
            expect(sanitizeURL('/cool')).toEqual('cool');
        });

        it("should remove the slash from the end if it's not the only character", () => {
            expect(sanitizeURL('super-cool/')).toEqual('super-cool');
        });

        it('should remove the slash from the end and the beggining', () => {
            expect(sanitizeURL('/hello-there/')).toEqual('hello-there');
        });

        it('should remove the index if a nested path', () => {
            expect(sanitizeURL('i-have-the-high-ground/index')).toEqual('i-have-the-high-ground');
        });

        it('should remove the index if a nested path with slash', () => {
            expect(sanitizeURL('no-index-please/index/')).toEqual('no-index-please');
        });

        it('should leave as it is for valid url', () => {
            expect(sanitizeURL('this-is-where-the-fun-begins')).toEqual(
                'this-is-where-the-fun-begins'
            );
        });

        it('should leave as it is for a nested valid url', () => {
            expect(sanitizeURL('hello-there/general-kenobi')).toEqual('hello-there/general-kenobi');
        });
    });
});
