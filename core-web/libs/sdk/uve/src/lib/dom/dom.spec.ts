/* eslint-disable @typescript-eslint/no-explicit-any */
import { DotCMSBasicContentlet } from '@dotcms/types';

import {
    getDotCMSContentletsBound,
    computeScrollIsInBottom,
    getContentletsInContainer,
    getDotContainerAttributes,
    getContainersData,
    getDotContentletAttributes,
    combineClasses,
    getColumnPositionClasses,
    findDotCMSElement
} from './dom.utils';

describe('getDotCMSContentletsBound', () => {
    const createContentlet = ({
        x,
        y,
        width,
        height,
        dataset
    }: {
        x: number;
        y: number;
        width: number;
        height: number;
        dataset: { [key: string]: string };
    }): HTMLDivElement => {
        const contentlet = document.createElement('div');
        const mockGetBoundingClientRect = jest.fn(() => ({
            x,
            y,
            width,
            height
        })) as unknown as () => DOMRect;
        contentlet.getBoundingClientRect = mockGetBoundingClientRect;
        Object.keys(dataset).forEach((key) => {
            contentlet.setAttribute(`data-${key}`, dataset[key]);
        });

        return contentlet;
    };

    const containerRect = {
        x: 0,
        y: 0,
        width: 100,
        height: 100,
        top: 0,
        right: 100,
        bottom: 100,
        left: 0
    } as DOMRect;

    const contentlets: HTMLDivElement[] = [
        createContentlet({
            x: 10,
            y: 20,
            width: 30,
            height: 40,
            dataset: {
                'dot-container': JSON.stringify({ uuid: 'container1' }),
                'dot-identifier': 'contentlet1',
                'dot-title': 'Contentlet 1',
                'dot-inode': 'inode1',
                'dot-type': 'type1'
            }
        }),
        createContentlet({
            x: 50,
            y: 60,
            width: 70,
            height: 80,
            dataset: {
                'dot-container': JSON.stringify({ uuid: 'container1' }),
                'dot-identifier': 'contentlet2',
                'dot-title': 'Contentlet 2',
                'dot-inode': 'inode2',
                'dot-type': 'type2'
            }
        })
    ];

    it('should return an array of contentlets bound from contentlet with data attribute dotContainer ', () => {
        const result = getDotCMSContentletsBound(containerRect, contentlets);

        expect(result).toEqual([
            {
                x: 0,
                y: 20,
                width: 30,
                height: 40,
                payload: JSON.stringify({
                    container: { uuid: 'container1' },
                    contentlet: {
                        identifier: 'contentlet1',
                        title: 'Contentlet 1',
                        inode: 'inode1',
                        contentType: 'type1'
                    }
                })
            },
            {
                x: 0,
                y: 60,
                width: 70,
                height: 80,
                payload: JSON.stringify({
                    container: { uuid: 'container1' },
                    contentlet: {
                        identifier: 'contentlet2',
                        title: 'Contentlet 2',
                        inode: 'inode2',
                        contentType: 'type2'
                    }
                })
            }
        ]);
    });

    it('should return an empty array if contentlets is empty', () => {
        const result = getDotCMSContentletsBound(containerRect, []);

        expect(result).toEqual([]);
    });

    it('should return an array of contentlets with correct properties when dotContainer is not present in dataset', () => {
        const contentletsWithMissingContainer: HTMLDivElement[] = [
            createContentlet({
                x: 10,
                y: 20,
                width: 30,
                height: 40,
                dataset: {
                    'dot-identifier': 'contentlet1',
                    'dot-title': 'Contentlet 1',
                    'dot-inode': 'inode1',
                    'dot-type': 'type1'
                }
            })
        ];

        const container = document.createElement('div');

        container.appendChild(contentletsWithMissingContainer[0]);
        container.setAttribute('data-dot-object', 'container');
        container.setAttribute('data-dot-accept-types', '[Blogs]');
        container.setAttribute('data-dot-identifier', '1');
        container.setAttribute('data-max-contentlets', '1');
        container.setAttribute('data-dot-uuid', '1');
        const result = getDotCMSContentletsBound(containerRect, contentletsWithMissingContainer);

        expect(result).toEqual([
            {
                x: 0,
                y: 20,
                width: 30,
                height: 40,
                payload: JSON.stringify({
                    container: {
                        acceptTypes: '[Blogs]',
                        identifier: '1',
                        maxContentlets: '1',
                        uuid: '1'
                    },
                    contentlet: {
                        identifier: 'contentlet1',
                        title: 'Contentlet 1',
                        inode: 'inode1',
                        contentType: 'type1'
                    }
                })
            }
        ]);
    });
});

describe('scrollIsInBottom', () => {
    it('should return true when scroll position + viewport height equals document height', () => {
        Object.defineProperty(window, 'innerHeight', {
            writable: true,
            configurable: true,
            value: 500
        });
        Object.defineProperty(window, 'scrollY', {
            writable: true,
            configurable: true,
            value: 500
        });
        Object.defineProperty(document.documentElement, 'scrollHeight', {
            writable: true,
            configurable: true,
            value: 1000
        });

        expect(computeScrollIsInBottom()).toBe(true);
    });

    it('should return false when scroll position + viewport height is less than document height', () => {
        Object.defineProperty(window, 'innerHeight', {
            writable: true,
            configurable: true,
            value: 500
        });
        Object.defineProperty(window, 'scrollY', {
            writable: true,
            configurable: true,
            value: 400
        });
        Object.defineProperty(document.documentElement, 'scrollHeight', {
            writable: true,
            configurable: true,
            value: 1000
        });

        expect(computeScrollIsInBottom()).toBe(false);
    });
});

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
        } as unknown as DotCMSBasicContentlet;
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

    it('should include data-dot-style-properties when styleProperties exist', () => {
        const styleProperties = {
            'font-size': 20,
            'font-family': 'Arial',
            alignment: 'center'
        };

        const contentlet = {
            identifier: 'test-id',
            baseType: 'test-base',
            title: 'Test Title',
            inode: 'test-inode',
            contentType: 'test-type',
            styleProperties
        } as unknown as DotCMSBasicContentlet;

        const result = getDotContentletAttributes(contentlet, 'test-container');

        expect(result['data-dot-style-properties']).toBe(JSON.stringify(styleProperties));
    });

    it('should not include data-dot-style-properties when styleProperties do not exist', () => {
        const contentlet = {
            identifier: 'test-id',
            baseType: 'test-base',
            title: 'Test Title',
            inode: 'test-inode',
            contentType: 'test-type'
        } as unknown as DotCMSBasicContentlet;

        const result = getDotContentletAttributes(contentlet, 'test-container');

        expect(result['data-dot-style-properties']).toBeUndefined();
    });

    it('should not include data-dot-style-properties when styleProperties is null', () => {
        const contentlet = {
            identifier: 'test-id',
            baseType: 'test-base',
            title: 'Test Title',
            inode: 'test-inode',
            contentType: 'test-type',
            styleProperties: null
        } as unknown as DotCMSBasicContentlet;

        const result = getDotContentletAttributes(contentlet, 'test-container');

        expect(result['data-dot-style-properties']).toBeUndefined();
    });

    it('should stringify styleProperties correctly', () => {
        const styleProperties = {
            'font-size': 20,
            'font-family': 'Arial',
            'text-decoration': {
                underline: true,
                overline: false
            }
        };

        const contentlet = {
            identifier: 'test-id',
            contentType: 'test-type',
            styleProperties
        } as unknown as DotCMSBasicContentlet;

        const result = getDotContentletAttributes(contentlet, 'test-container');

        expect(result['data-dot-style-properties']).toBe(JSON.stringify(styleProperties));
        expect(JSON.parse(result['data-dot-style-properties']!)).toEqual(styleProperties);
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
                    containerStructures: [{ contentTypeVar: 'type1' }, { contentTypeVar: 'type2' }],
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

describe('findDotCMSElement', () => {
    const createElement = (tagName = 'div'): HTMLElement => {
        return document.createElement(tagName);
    };

    const setDataAttribute = (element: HTMLElement, key: string, value: string): void => {
        element.setAttribute(`data-${key}`, value);
    };

    it('should return null when element is null', () => {
        const result = findDotCMSElement(null);
        expect(result).toBeNull();
    });

    it('should return element when it has data-dot-object="contentlet"', () => {
        const element = createElement();
        setDataAttribute(element, 'dot-object', 'contentlet');

        const result = findDotCMSElement(element);
        expect(result).toBe(element);
    });

    it('should return element when it has data-dot-object="container" and contains empty content', () => {
        const container = createElement();
        const emptyContent = createElement();

        setDataAttribute(container, 'dot-object', 'container');
        setDataAttribute(emptyContent, 'dot-object', 'empty-content');

        container.appendChild(emptyContent);

        const result = findDotCMSElement(container);
        expect(result).toBe(container);
    });

    it('should return element when it has data-dot-object="container" and has no children', () => {
        const container = createElement();
        setDataAttribute(container, 'dot-object', 'container');

        const result = findDotCMSElement(container);
        expect(result).toBe(container);
    });

    it('should not return container when it has children but no empty content', () => {
        const container = createElement();
        const child = createElement();

        setDataAttribute(container, 'dot-object', 'container');
        container.appendChild(child);

        // Mock parentElement to avoid infinite recursion
        Object.defineProperty(container, 'parentElement', {
            value: null,
            writable: true
        });

        const result = findDotCMSElement(container);
        expect(result).toBeNull();
    });

    it('should return null when no element is found', () => {
        const parent = createElement();
        const child = createElement();

        // Neither element has the required data attributes
        parent.appendChild(child);

        Object.defineProperty(child, 'parentElement', {
            value: parent,
            writable: true
        });
        Object.defineProperty(parent, 'parentElement', {
            value: null,
            writable: true
        });

        const result = findDotCMSElement(child);
        expect(result).toBeNull();
    });

    it('should find contentlet in parent when child does not match', () => {
        const parent = createElement();
        const child = createElement();

        setDataAttribute(parent, 'dot-object', 'contentlet');
        parent.appendChild(child);

        Object.defineProperty(child, 'parentElement', {
            value: parent,
            writable: true
        });
        Object.defineProperty(parent, 'parentElement', {
            value: null,
            writable: true
        });

        const result = findDotCMSElement(child);
        expect(result).toBe(parent);
    });

    it('should find container with empty content in parent hierarchy', () => {
        const grandParent = createElement();
        const parent = createElement();
        const child = createElement();
        const emptyContent = createElement();

        setDataAttribute(grandParent, 'dot-object', 'container');
        setDataAttribute(emptyContent, 'dot-object', 'empty-content');

        grandParent.appendChild(emptyContent);
        grandParent.appendChild(parent);
        parent.appendChild(child);

        Object.defineProperty(child, 'parentElement', {
            value: parent,
            writable: true
        });
        Object.defineProperty(parent, 'parentElement', {
            value: grandParent,
            writable: true
        });
        Object.defineProperty(grandParent, 'parentElement', {
            value: null,
            writable: true
        });

        const result = findDotCMSElement(child);
        expect(result).toBe(grandParent);
    });

    it('should find empty container in parent hierarchy', () => {
        const grandParent = createElement();
        const parent = createElement();
        const child = createElement();

        setDataAttribute(grandParent, 'dot-object', 'container');
        // grandParent has no children initially

        // Add parent after setting up the container
        grandParent.appendChild(parent);
        parent.appendChild(child);

        Object.defineProperty(child, 'parentElement', {
            value: parent,
            writable: true
        });
        Object.defineProperty(parent, 'parentElement', {
            value: grandParent,
            writable: true
        });
        Object.defineProperty(grandParent, 'parentElement', {
            value: null,
            writable: true
        });

        // Since grandParent now has children, it won't match the empty container criteria
        // Let's test with an empty container instead
        const emptyContainer = createElement();
        setDataAttribute(emptyContainer, 'dot-object', 'container');

        Object.defineProperty(child, 'parentElement', {
            value: emptyContainer,
            writable: true
        });
        Object.defineProperty(emptyContainer, 'parentElement', {
            value: null,
            writable: true
        });

        const result = findDotCMSElement(child);
        expect(result).toBe(emptyContainer);
    });

    it('should handle complex DOM hierarchy with multiple levels', () => {
        const level4 = createElement(); // contentlet
        const level3 = createElement();
        const level2 = createElement();
        const level1 = createElement();
        const level0 = createElement(); // starting point

        setDataAttribute(level4, 'dot-object', 'contentlet');

        level4.appendChild(level3);
        level3.appendChild(level2);
        level2.appendChild(level1);
        level1.appendChild(level0);

        Object.defineProperty(level0, 'parentElement', { value: level1, writable: true });
        Object.defineProperty(level1, 'parentElement', { value: level2, writable: true });
        Object.defineProperty(level2, 'parentElement', { value: level3, writable: true });
        Object.defineProperty(level3, 'parentElement', { value: level4, writable: true });
        Object.defineProperty(level4, 'parentElement', { value: null, writable: true });

        const result = findDotCMSElement(level0);
        expect(result).toBe(level4);
    });
});
