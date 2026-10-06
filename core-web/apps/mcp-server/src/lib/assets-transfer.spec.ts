import { includeMatcher, splitIncludePatterns } from './assets-transfer';

describe('splitIncludePatterns', () => {
    it('splits comma-separated patterns and trims them', () => {
        expect(splitIncludePatterns('*.vtl, *.scss')).toEqual(['*.vtl', '*.scss']);
    });

    it('does NOT split on a comma inside a brace group', () => {
        expect(splitIncludePatterns('*.{png,webp,jpg}')).toEqual(['*.{png,webp,jpg}']);
    });

    it('splits around a brace group but keeps the group intact', () => {
        expect(splitIncludePatterns('*.{png,jpg},*.vtl')).toEqual(['*.{png,jpg}', '*.vtl']);
    });

    it('drops empty entries and returns [] for undefined', () => {
        expect(splitIncludePatterns('*.png,,')).toEqual(['*.png']);
        expect(splitIncludePatterns(undefined)).toEqual([]);
    });
});

describe('includeMatcher', () => {
    it('matches everything when no include is given', () => {
        const m = includeMatcher();
        expect(m('a.png')).toBe(true);
        expect(m('deep/nested/a.vtl')).toBe(true);
    });

    // The three repro cases from the bug report — files live directly in the source dir.
    describe('bug report repro (top-level files)', () => {
        it('brace expansion matches top-level files (was: 0 matched)', () => {
            const m = includeMatcher('*.{png,webp,jpg}');
            expect(m('amazon-logo.png')).toBe(true);
            expect(m('book1.webp')).toBe(true);
            expect(m('cover.jpg')).toBe(true);
            expect(m('notes.txt')).toBe(false);
        });

        it('** globstar matches a top-level file too (was: 0 matched)', () => {
            const m = includeMatcher('**/*.png');
            expect(m('amazon-logo.png')).toBe(true); // no subdirectory — must still match
            expect(m('img/hero.png')).toBe(true);
            expect(m('a/b/c/deep.png')).toBe(true);
            expect(m('a/b/c/deep.webp')).toBe(false);
        });

        it('plain top-level glob still works', () => {
            const m = includeMatcher('*.png');
            expect(m('amazon-logo.png')).toBe(true);
            expect(m('book1.webp')).toBe(false);
        });
    });

    describe('single-star does not cross directories', () => {
        it('"*.png" (no slash) matches a basename anywhere in the tree', () => {
            const m = includeMatcher('*.png');
            expect(m('a.png')).toBe(true);
            expect(m('deep/dir/a.png')).toBe(true); // basename match, unanchored
        });

        it('an anchored "img/*.png" only matches that one directory level', () => {
            const m = includeMatcher('img/*.png');
            expect(m('img/a.png')).toBe(true);
            expect(m('img/sub/a.png')).toBe(false); // * does not cross /
            expect(m('other/a.png')).toBe(false);
        });
    });

    describe('** globstar depth', () => {
        it('"img/**/*.png" matches zero or more intermediate dirs', () => {
            const m = includeMatcher('img/**/*.png');
            expect(m('img/a.png')).toBe(true); // zero intermediate dirs
            expect(m('img/sub/a.png')).toBe(true);
            expect(m('img/a/b/c.png')).toBe(true);
            expect(m('other/a.png')).toBe(false);
        });
    });

    describe('? single char', () => {
        it('matches exactly one non-slash char', () => {
            const m = includeMatcher('file?.txt');
            expect(m('file1.txt')).toBe(true);
            expect(m('fileA.txt')).toBe(true);
            expect(m('file.txt')).toBe(false);
            expect(m('file12.txt')).toBe(false);
        });
    });

    describe('literals are escaped', () => {
        it('a dot in the pattern is literal, not "any char"', () => {
            const m = includeMatcher('*.png');
            expect(m('axpng')).toBe(false); // the "." must be a real dot
            expect(m('a.png')).toBe(true);
        });

        it('multiple patterns OR together', () => {
            const m = includeMatcher('*.vtl,*.scss');
            expect(m('theme.vtl')).toBe(true);
            expect(m('styles.scss')).toBe(true);
            expect(m('image.png')).toBe(false);
        });
    });

    it('is case-insensitive', () => {
        const m = includeMatcher('*.PNG');
        expect(m('photo.png')).toBe(true);
    });
});

describe('includeMatcher — trailing globstar', () => {
    // `dir/**` is the common glob idiom, but it used to compile to `^dir(?:.*/)?$`, which
    // matches only the bare string `dir` and no file path under it. Anyone writing it hit the
    // "include pattern matched 0 of N files — check the glob syntax" warning while their
    // syntax was perfectly reasonable.
    it('matches files directly under the directory', () => {
        const m = includeMatcher('themes/**');
        expect(m('themes/style.css')).toBe(true);
    });

    it('matches files nested deeper under the directory', () => {
        const m = includeMatcher('themes/**');
        expect(m('themes/travel/css/style.css')).toBe(true);
    });

    it('does not match a sibling directory that shares the prefix', () => {
        const m = includeMatcher('themes/**');
        expect(m('themes-backup/style.css')).toBe(false);
        expect(m('other/style.css')).toBe(false);
    });

    it('still supports a leading globstar with a pattern after it', () => {
        const m = includeMatcher('**/*.png');
        expect(m('logo.png')).toBe(true);
        expect(m('themes/img/logo.png')).toBe(true);
        expect(m('themes/style.css')).toBe(false);
    });

    it('treats a bare globstar as match-everything', () => {
        const m = includeMatcher('**');
        expect(m('a.css')).toBe(true);
        expect(m('a/b/c.vtl')).toBe(true);
    });
});
