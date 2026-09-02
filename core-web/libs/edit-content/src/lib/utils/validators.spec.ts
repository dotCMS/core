import { FormControl } from '@angular/forms';

import { blockEditorRequiredValidator } from './validators';

describe('blockEditorRequiredValidator', () => {
    const validator = blockEditorRequiredValidator();

    describe('empty content (should return required error)', () => {
        it('should return error for null', () => {
            const control = new FormControl(null);
            expect(validator(control)).toEqual({ required: true });
        });

        it('should return error for undefined', () => {
            const control = new FormControl(undefined);
            expect(validator(control)).toEqual({ required: true });
        });

        it('should return error for empty string', () => {
            const control = new FormControl('');
            expect(validator(control)).toEqual({ required: true });
        });

        it('should return error for whitespace-only string', () => {
            const control = new FormControl('   ');
            expect(validator(control)).toEqual({ required: true });
        });

        it('should return error for empty doc JSON structure', () => {
            const emptyDoc = { type: 'doc', content: [{ type: 'paragraph' }] };
            const control = new FormControl(emptyDoc);
            expect(validator(control)).toEqual({ required: true });
        });

        it('should return error for empty doc JSON as string', () => {
            const emptyDoc = JSON.stringify({
                type: 'doc',
                content: [{ type: 'paragraph' }]
            });
            const control = new FormControl(emptyDoc);
            expect(validator(control)).toEqual({ required: true });
        });

        it('should return error for doc with empty text node', () => {
            const doc = {
                type: 'doc',
                content: [
                    {
                        type: 'paragraph',
                        content: [{ type: 'text', text: '' }]
                    }
                ]
            };
            const control = new FormControl(doc);
            expect(validator(control)).toEqual({ required: true });
        });

        it('should return error for doc with whitespace-only text node', () => {
            const doc = {
                type: 'doc',
                content: [
                    {
                        type: 'paragraph',
                        content: [{ type: 'text', text: '   ' }]
                    }
                ]
            };
            const control = new FormControl(doc);
            expect(validator(control)).toEqual({ required: true });
        });

        it('should return error for nested structure without text content', () => {
            const doc = {
                type: 'doc',
                content: [
                    { type: 'paragraph' },
                    { type: 'paragraph', content: [] },
                    { type: 'heading', attrs: { level: 1 } }
                ]
            };
            const control = new FormControl(doc);
            expect(validator(control)).toEqual({ required: true });
        });
    });

    describe('non-empty content (should return null)', () => {
        it('should return null for doc with text content', () => {
            const doc = {
                type: 'doc',
                content: [
                    {
                        type: 'paragraph',
                        content: [{ type: 'text', text: 'Hello world' }]
                    }
                ]
            };
            const control = new FormControl(doc);
            expect(validator(control)).toBeNull();
        });

        it('should return null for doc with text content as JSON string', () => {
            const doc = JSON.stringify({
                type: 'doc',
                content: [
                    {
                        type: 'paragraph',
                        content: [{ type: 'text', text: 'Hello world' }]
                    }
                ]
            });
            const control = new FormControl(doc);
            expect(validator(control)).toBeNull();
        });

        it('should return null for deeply nested text content', () => {
            const doc = {
                type: 'doc',
                content: [
                    {
                        type: 'blockquote',
                        content: [
                            {
                                type: 'paragraph',
                                content: [{ type: 'text', text: 'Quoted text' }]
                            }
                        ]
                    }
                ]
            };
            const control = new FormControl(doc);
            expect(validator(control)).toBeNull();
        });

        it('should return null for doc with heading text', () => {
            const doc = {
                type: 'doc',
                content: [
                    {
                        type: 'heading',
                        attrs: { level: 1, textAlign: 'left' },
                        content: [{ type: 'text', text: 'A title!!' }]
                    }
                ]
            };
            const control = new FormControl(doc);
            expect(validator(control)).toBeNull();
        });

        it('should return null for plain text string that is not valid JSON', () => {
            const control = new FormControl('Some plain text content');
            expect(validator(control)).toBeNull();
        });

        it('should return null for doc with multiple paragraphs where only one has text', () => {
            const doc = {
                type: 'doc',
                content: [
                    { type: 'paragraph' },
                    {
                        type: 'paragraph',
                        content: [{ type: 'text', text: 'Content here' }]
                    },
                    { type: 'paragraph' }
                ]
            };
            const control = new FormControl(doc);
            expect(validator(control)).toBeNull();
        });
    });
});
