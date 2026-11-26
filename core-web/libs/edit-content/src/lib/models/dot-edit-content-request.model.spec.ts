import { DotEditContentRequest, withoutUnset } from './dot-edit-content-request.model';

describe('withoutUnset', () => {
    it('drops the keys left undefined, so the request names only what was said', () => {
        const request = withoutUnset({
            mode: 'edit',
            inode: 'abc',
            contentType: undefined,
            languageId: undefined
        });

        expect(request).toEqual({ mode: 'edit', inode: 'abc' });
        expect(Object.keys(request)).toEqual(['mode', 'inode']);
    });

    it('keeps falsy values that were set', () => {
        const request: DotEditContentRequest = { mode: 'new', contentType: '', depth: 0 };

        expect(withoutUnset(request)).toEqual({ mode: 'new', contentType: '', depth: 0 });
    });

    it('returns a copy, leaving the request it was given alone', () => {
        const request: DotEditContentRequest = { mode: 'edit', inode: 'abc', title: undefined };

        const result = withoutUnset(request);

        expect(result).not.toBe(request);
        expect('title' in request).toBe(true);
    });
});
