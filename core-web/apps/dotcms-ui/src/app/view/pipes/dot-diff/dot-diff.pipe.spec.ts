import { DotDiffPipe } from './dot-diff.pipe';

describe('DotDiffPipe', () => {
    it('should show difference', () => {
        const pipe = new DotDiffPipe();
        expect(pipe.transform('Hi', 'hello')).toEqual(
            '<del class="diffmod">Hi</del><ins class="diffmod">hello</ins>'
        );
    });
    it('should show new value', () => {
        const pipe = new DotDiffPipe();
        expect(pipe.transform('By', 'hello', false)).toEqual('hello');
    });
});
