import { DotFilterPipe } from './dot-filter.pipe';

describe('DotFilterPipe', () => {
    it('should filter items in list', () => {
        const list = [{ name: 'Costa Rica' }, { name: 'USA' }];
        const pipe = new DotFilterPipe();
        expect(pipe.transform(list, 'name', 'Costa')).toEqual([{ name: 'Costa Rica' }]);
    });
});
