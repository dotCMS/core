import { getDotOptionsFromFieldValue } from './utils';

describe('getDotOptionsFromFieldValue', () => {
    it('should render a Form', async () => {
        const rawItems = 'key1|A,key2|B';
        const items = getDotOptionsFromFieldValue(rawItems);
        expect(items.length).toBe(2);
        expect(items).toEqual([
            { label: 'key1', value: 'A' },
            { label: 'key2', value: 'B' }
        ]);
    });
});
