import { DOT_PANEL_URL_PARAM, readPanelUrlIntent } from './read-panel-url-intent';

/** An identifier, in the shape `isDotIdentifier` accepts. */
const IDENTIFIER = '3f0c8e5a-7b2d-4c1e-9a6f-0d4b8e2c1a57';

/** An inode: the same UUID shape as an identifier. */
const INODE = '9b1d2c3e-4f5a-4b6c-8d7e-1f2a3b4c5d6e';

/**
 * What a URL asks to open when a surface loads (#38303, FR-049). Values reach a Lucene query and
 * REST paths, so only the shapes they can take are accepted and anything else reads as absent.
 */
describe('readPanelUrlIntent', () => {
    // Content Drive's names, kept so the links authors already share keep opening (FR-044).
    it('names the params as Content Drive always has', () => {
        expect(DOT_PANEL_URL_PARAM).toEqual({
            EDIT_CONTENT: 'editContent',
            EDIT_CONTENT_LANG: 'editContentLang',
            CREATE_CONTENT: 'createContent',
            EDIT_CODE: 'editCode'
        });
    });

    describe('an edit', () => {
        it('reads an identifier with its language', () => {
            expect(readPanelUrlIntent({ editContent: IDENTIFIER, editContentLang: '2' })).toEqual({
                kind: 'edit',
                identifier: IDENTIFIER,
                languageId: 2
            });
        });

        // `toEqual` ignores a `languageId: undefined`, and fails on any number.
        it('reads an identifier without a language', () => {
            expect(readPanelUrlIntent({ editContent: IDENTIFIER })).toEqual({
                kind: 'edit',
                identifier: IDENTIFIER
            });
        });

        it.each(['abc', '0', '-1', '1.5'])(
            'reads a language of %j as no language',
            (editContentLang) => {
                expect(readPanelUrlIntent({ editContent: IDENTIFIER, editContentLang })).toEqual({
                    kind: 'edit',
                    identifier: IDENTIFIER
                });
            }
        );

        // `new` is the retired create marker: an old link opens nothing rather than an empty form.
        it.each(['new', 'not-an-identifier', `${IDENTIFIER} OR +live:true`, ''])(
            'reads editContent=%j as nothing',
            (editContent) => {
                expect(readPanelUrlIntent({ editContent })).toEqual({ kind: 'none' });
            }
        );

        it('reads a language alone as nothing', () => {
            expect(readPanelUrlIntent({ editContentLang: '1' })).toEqual({ kind: 'none' });
        });
    });

    describe('a create', () => {
        it('reads a content type variable', () => {
            expect(readPanelUrlIntent({ createContent: 'Blog' })).toEqual({
                kind: 'create',
                contentType: 'Blog'
            });
        });

        it('reads a content type id', () => {
            expect(readPanelUrlIntent({ createContent: 'blog_post-2' })).toEqual({
                kind: 'create',
                contentType: 'blog_post-2'
            });
        });

        it.each(['blog/posts', '../Blog', 'Blog Post', ''])(
            'reads createContent=%j as nothing',
            (createContent) => {
                expect(readPanelUrlIntent({ createContent })).toEqual({ kind: 'none' });
            }
        );
    });

    describe('Edit Source', () => {
        it('reads an inode', () => {
            expect(readPanelUrlIntent({ editCode: INODE })).toEqual({
                kind: 'editCode',
                inode: INODE
            });
        });

        it.each(['not-an-inode', '/etc/passwd', ''])('reads editCode=%j as nothing', (editCode) => {
            expect(readPanelUrlIntent({ editCode })).toEqual({ kind: 'none' });
        });
    });

    describe('more than one panel', () => {
        it.each([
            ['an edit and a create', { editContent: IDENTIFIER, createContent: 'Blog' }],
            ['an edit and Edit Source', { editContent: IDENTIFIER, editCode: INODE }],
            ['a create and Edit Source', { createContent: 'Blog', editCode: INODE }]
        ])('reads %s as a conflict', (_label, queryParams) => {
            expect(readPanelUrlIntent(queryParams)).toEqual({ kind: 'conflict' });
        });

        // A refused value counts as absent, so it cannot make a conflict out of a valid one.
        it('reads a refused value beside a valid one as the valid one alone', () => {
            expect(readPanelUrlIntent({ editContent: 'new', createContent: 'Blog' })).toEqual({
                kind: 'create',
                contentType: 'Blog'
            });
        });
    });

    describe('nothing to open', () => {
        it('reads no params as nothing', () => {
            expect(readPanelUrlIntent({})).toEqual({ kind: 'none' });
        });

        it("reads only the surface's own params as nothing", () => {
            expect(readPanelUrlIntent({ q: '+live:true', limit: '20' })).toEqual({ kind: 'none' });
        });

        // A param repeated in the URL arrives as an array.
        it('reads a value that is not a string as absent', () => {
            expect(readPanelUrlIntent({ editContent: [IDENTIFIER, IDENTIFIER] })).toEqual({
                kind: 'none'
            });
        });
    });
});
