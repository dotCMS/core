import {
    DOT_PANEL_URL_PARAM,
    panelUrlIntentKey,
    readPanelUrlIntent
} from './read-panel-url-intent';

/** An identifier, in the shape `isDotIdentifier` accepts. */
const IDENTIFIER = '3f0c8e5a-7b2d-4c1e-9a6f-0d4b8e2c1a57';

/** An inode: the same UUID shape as an identifier. */
const INODE = '9b1d2c3e-4f5a-4b6c-8d7e-1f2a3b4c5d6e';

/** A link the author cannot have meant: a panel param of the wrong shape. */
const MALFORMED = { kind: 'refused', reason: 'malformed' };

/** A link naming more than one panel. */
const CONFLICT = { kind: 'refused', reason: 'conflict' };

/**
 * What a URL asks to open when a surface loads (#38303, FR-049). Values reach a Lucene query and
 * REST paths, so only the shapes they can take are accepted; a panel param of any other shape
 * refuses the link, so the surface can tell the author instead of opening nothing in silence.
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

        it.each(['abc', '0', '-1', '1.5'])('refuses a language of %j', (editContentLang) => {
            expect(readPanelUrlIntent({ editContent: IDENTIFIER, editContentLang })).toEqual(
                MALFORMED
            );
        });

        // `new` is the retired create marker: an old bookmark is refused rather than opening an
        // empty form, or nothing in silence.
        it.each(['new', 'wrong-identifier', `${IDENTIFIER} OR +live:true`])(
            'refuses editContent=%j',
            (editContent) => {
                expect(readPanelUrlIntent({ editContent })).toEqual(MALFORMED);
            }
        );

        // An empty value names nothing; it is not a link to anything.
        it('reads an empty editContent as nothing', () => {
            expect(readPanelUrlIntent({ editContent: '' })).toEqual({ kind: 'none' });
        });

        // The identifier reaches a Lucene query: a stray space would make the lookup miss.
        it('reads an identifier with spaces around it as the identifier alone', () => {
            expect(readPanelUrlIntent({ editContent: ` ${IDENTIFIER} ` })).toEqual({
                kind: 'edit',
                identifier: IDENTIFIER
            });
        });

        it('reads a language alone as nothing', () => {
            expect(readPanelUrlIntent({ editContentLang: '1' })).toEqual({ kind: 'none' });
        });

        it('refuses a malformed language alone', () => {
            expect(readPanelUrlIntent({ editContentLang: 'abc' })).toEqual(MALFORMED);
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

        it.each(['blog/posts', '../Blog', 'Blog Post'])(
            'refuses createContent=%j',
            (createContent) => {
                expect(readPanelUrlIntent({ createContent })).toEqual(MALFORMED);
            }
        );

        it('reads an empty createContent as nothing', () => {
            expect(readPanelUrlIntent({ createContent: '' })).toEqual({ kind: 'none' });
        });
    });

    describe('Edit Source', () => {
        it('reads an inode', () => {
            expect(readPanelUrlIntent({ editCode: INODE })).toEqual({
                kind: 'editCode',
                inode: INODE
            });
        });

        it.each(['not-an-inode', '/etc/passwd'])('refuses editCode=%j', (editCode) => {
            expect(readPanelUrlIntent({ editCode })).toEqual(MALFORMED);
        });
    });

    describe('more than one panel', () => {
        it.each([
            ['an edit and a create', { editContent: IDENTIFIER, createContent: 'Blog' }],
            ['an edit and Edit Source', { editContent: IDENTIFIER, editCode: INODE }],
            ['a create and Edit Source', { createContent: 'Blog', editCode: INODE }]
        ])('refuses %s as a conflict', (_label, queryParams) => {
            expect(readPanelUrlIntent(queryParams)).toEqual(CONFLICT);
        });

        // A malformed value refuses the link, even beside a valid one: the author did not mean it.
        it('refuses a malformed value beside a valid one', () => {
            expect(readPanelUrlIntent({ editContent: 'new', createContent: 'Blog' })).toEqual(
                MALFORMED
            );
        });
    });

    describe('nothing to open', () => {
        it('reads no params as nothing', () => {
            expect(readPanelUrlIntent({})).toEqual({ kind: 'none' });
        });

        it("reads only the surface's own params as nothing", () => {
            expect(readPanelUrlIntent({ q: '+live:true', limit: '20' })).toEqual({ kind: 'none' });
        });

        it("reads a repeated param of the surface's own as nothing", () => {
            expect(readPanelUrlIntent({ q: ['a', 'b'] })).toEqual({ kind: 'none' });
        });
    });

    // A panel param repeated in the URL arrives as an array, which names no one panel.
    it('refuses a repeated panel param', () => {
        expect(readPanelUrlIntent({ editContent: [IDENTIFIER, IDENTIFIER] })).toEqual(MALFORMED);
    });
});

/**
 * The same for the same panel, so a surface that follows its URL opens a panel once, whatever else
 * a navigation changes in the URL (#38303, FR-049).
 */
describe('panelUrlIntentKey', () => {
    it('is the same for the same panel', () => {
        expect(
            panelUrlIntentKey(readPanelUrlIntent({ editContent: IDENTIFIER, editContentLang: '2' }))
        ).toBe(
            panelUrlIntentKey(
                readPanelUrlIntent({ editContent: IDENTIFIER, editContentLang: '2', q: 'x' })
            )
        );
    });

    it.each([
        ['another language', { editContent: IDENTIFIER, editContentLang: '3' }],
        ['no language', { editContent: IDENTIFIER }],
        ['a create', { createContent: 'Blog' }],
        ['Edit Source', { editCode: INODE }],
        ['no panel', {}],
        ['two panels', { editContent: IDENTIFIER, createContent: 'Blog' }]
    ])('differs for %s', (_label, params) => {
        expect(panelUrlIntentKey(readPanelUrlIntent(params))).not.toBe(
            panelUrlIntentKey(readPanelUrlIntent({ editContent: IDENTIFIER, editContentLang: '2' }))
        );
    });
});
