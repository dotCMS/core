import { getContentletsBound, scrollIsInBottom } from './editor.utils';

describe('getContentletsBound', () => {
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

    it('should return an array of contentlets bound from contentlet with data atrribute dotContainer ', () => {
        const result = getContentletsBound(containerRect, contentlets);

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
        const result = getContentletsBound(containerRect, []);

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
        const result = getContentletsBound(containerRect, contentletsWithMissingContainer);

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

        expect(scrollIsInBottom()).toBe(true);
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

        expect(scrollIsInBottom()).toBe(false);
    });
});
