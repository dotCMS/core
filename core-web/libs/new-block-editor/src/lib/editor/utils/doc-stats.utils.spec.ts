import { type JSONContent } from '@tiptap/core';

import { stripDocStats } from './doc-stats.utils';

describe('stripDocStats', () => {
    it('drops the attrs key entirely when only stats are present', () => {
        const input: JSONContent = {
            type: 'doc',
            attrs: { charCount: 6, wordCount: 2, readingTime: 1 },
            content: [{ type: 'paragraph' }]
        };

        expect(stripDocStats(input)).toEqual({
            type: 'doc',
            content: [{ type: 'paragraph' }]
        });
    });

    it('matches a plain editor.getJSON() shape after a stats round-trip', () => {
        const editorJson: JSONContent = { type: 'doc', content: [{ type: 'paragraph' }] };
        const emitted: JSONContent = {
            type: 'doc',
            attrs: { charCount: 6, wordCount: 2, readingTime: 1 },
            content: [{ type: 'paragraph' }]
        };

        // The whole point of the guard fix (#36985): the round-tripped value must compare equal.
        expect(JSON.stringify(stripDocStats(emitted))).toBe(
            JSON.stringify(stripDocStats(editorJson))
        );
    });

    it('keeps non-stat attrs and removes only the stats', () => {
        const input: JSONContent = {
            type: 'doc',
            attrs: { charCount: 6, wordCount: 2, readingTime: 1, custom: 'keep' },
            content: []
        };

        expect(stripDocStats(input)).toEqual({
            type: 'doc',
            attrs: { custom: 'keep' },
            content: []
        });
    });

    it('returns the input untouched when there are no attrs', () => {
        const input: JSONContent = { type: 'doc', content: [{ type: 'paragraph' }] };

        expect(stripDocStats(input)).toBe(input);
    });

    it('does not mutate the input object', () => {
        const input: JSONContent = {
            type: 'doc',
            attrs: { charCount: 6, wordCount: 2, readingTime: 1 },
            content: []
        };

        stripDocStats(input);

        expect(input.attrs).toEqual({ charCount: 6, wordCount: 2, readingTime: 1 });
    });
});
