import {
    combineClasses,
    getColumnPositionClasses,
    getDotContentletAttributes,
    getContainersData,
    getContentletsInContainer,
    getDotContainerAttributes
} from '../utils';

describe('utils', () => {
    describe('combineClasses', () => {
        it('should combine valid classes', () => {
            expect(combineClasses(['class1', '', 'class2', 'class3'])).toBe('class1 class2 class3');
        });

        it('should return empty string for empty array', () => {
            expect(combineClasses([])).toBe('');
        });
    });

    describe('getColumnPositionClasses', () => {
        it('should return correct position classes for valid column config', () => {
            const column = { leftOffset: 1, width: 6 } as any;
            const result = getColumnPositionClasses(column);
            expect(result).toEqual({
                startClass: 'col-start-1',
                endClass: 'col-end-7'
            });
        });

        it('should handle edge cases', () => {
            const column = { leftOffset: 12, width: 1 } as any;
            const result = getColumnPositionClasses(column);
            expect(result).toEqual({
                startClass: 'col-start-12',
                endClass: 'col-end-13'
            });
        });
    });

    describe('getDotContentletAttributes', () => {
        it('should return correct attributes for a contentlet', () => {
            const contentlet = {
                identifier: 'test-id',
                baseType: 'test-base',
                title: 'Test Title',
                inode: 'test-inode',
                contentType: 'test-type',
                onNumberOfPages: '5'
            } as any;
            const container = 'test-container';

            const result = getDotContentletAttributes(contentlet, container);

            expect(result).toEqual({
                'data-dot-identifier': 'test-id',
                'data-dot-basetype': 'test-base',
                'data-dot-title': 'Test Title',
                'data-dot-inode': 'test-inode',
                'data-dot-type': 'test-type',
                'data-dot-container': 'test-container',
                'data-dot-on-number-of-pages': '5'
            });
        });

        it('should use widgetTitle if available', () => {
            const contentlet = {
                identifier: 'test-id',
                widgetTitle: 'Widget Title',
                title: 'Regular Title'
            };
            const result = getDotContentletAttributes(contentlet as any, 'container');
            expect(result['data-dot-title']).toBe('Widget Title');
        });
    });

    describe('getContainersData', () => {
        it('should return null if container not found', () => {
            const pageAsset = { containers: {} } as any;
            const columnContainer = { identifier: 'not-found', uuid: '123' } as any;
            const result = getContainersData(pageAsset, columnContainer);
            expect(result).toBeNull();
        });

        it('should return formatted container data', () => {
            const pageAsset = {
                containers: {
                    'test-container': {
                        containerStructures: [
                            { contentTypeVar: 'type1' },
                            { contentTypeVar: 'type2' }
                        ],
                        container: {
                            maxContentlets: 5,
                            parentPermissionable: {
                                variantId: 'variant-1'
                            },
                            path: '/test/path'
                        }
                    }
                }
            } as any;
            const columnContainer = {
                identifier: 'test-container',
                uuid: '123'
            } as any;

            const result = getContainersData(pageAsset, columnContainer);

            expect(result).toEqual({
                uuid: '123',
                variantId: 'variant-1',
                acceptTypes: 'type1,type2',
                maxContentlets: 5,
                identifier: '/test/path'
            });
        });
    });

    describe('getContentletsInContainer', () => {
        const mockContentlets = [{ id: 1 }, { id: 2 }];

        it('should return contentlets using standard uuid format', () => {
            const pageAsset = {
                containers: {
                    'test-container': {
                        contentlets: {
                            'uuid-123': mockContentlets
                        }
                    }
                }
            } as any;
            const columnContainer = { identifier: 'test-container', uuid: '123' } as any;

            const result = getContentletsInContainer(pageAsset, columnContainer);
            expect(result).toEqual(mockContentlets);
        });

        it('should return contentlets using dotParser uuid format', () => {
            const pageAsset = {
                containers: {
                    'test-container': {
                        contentlets: {
                            'uuid-dotParser_123': mockContentlets
                        }
                    }
                }
            } as any;
            const columnContainer = { identifier: 'test-container', uuid: '123' } as any;

            const result = getContentletsInContainer(pageAsset, columnContainer);
            expect(result).toEqual(mockContentlets);
        });
    });

    describe('getDotContainerAttributes', () => {
        it('should return correct container attributes', () => {
            const containerData = {
                uuid: 'test-uuid',
                identifier: 'test-identifier',
                acceptTypes: 'type1,type2',
                maxContentlets: 5
            };

            const result = getDotContainerAttributes(containerData);

            expect(result).toEqual({
                'data-dot-object': 'container',
                'data-dot-accept-types': 'type1,type2',
                'data-dot-identifier': 'test-identifier',
                'data-max-contentlets': '5',
                'data-dot-uuid': 'test-uuid'
            });
        });
    });
});
