import { DotRandomIconPipe } from './dot-random-icon.pipe';

describe('DotRandomIconPipe', () => {
    it('should return a random icon', () => {
        const randomText = 'Content';
        const pipe = new DotRandomIconPipe();

        expect(pipe.transform(randomText)).toEqual('grid_view');
    });
});
