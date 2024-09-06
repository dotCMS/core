import { DotStringFormatPipe } from '@dotcms/ui';

describe('DotStringFormatPipe', () => {
    it('should replace tokens correctly', () => {
        const pipe = new DotStringFormatPipe();
        expect(pipe.transform('Good {0} {1}', ['morning', 'cr'])).toEqual('Good morning cr');
    });
});
