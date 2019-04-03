import { getItemsFromString } from './utils';

describe('getItemsFromString', () => {
    it('should render a Form', async () => {
        const rawItems = 'key1|A,key2|B';
        const items = getItemsFromString(rawItems);
        expect(items.length).toBe(2);
        expect(items[0].label).toBe('key1');
        expect(items[0].value).toBe('A');
        expect(items[1].label).toBe('key2');
        expect(items[1].value).toBe('B');
    });
});
