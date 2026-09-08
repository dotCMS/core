/* eslint-disable @typescript-eslint/no-explicit-any */

import { createPipeFactory, SpectatorPipe } from '@openng/spectator/jest';

import { Injector, runInInjectionContext } from '@angular/core';
import { DomSanitizer } from '@angular/platform-browser';

import { DotHighlightPipe } from './dot-highlight.pipe';

describe('DotHighlightPipe', () => {
    let spectator: SpectatorPipe<DotHighlightPipe>;
    let pipe: DotHighlightPipe;
    let sanitizer: DomSanitizer;

    const createPipe = createPipeFactory({
        pipe: DotHighlightPipe,
        providers: []
    });

    beforeEach(() => {
        spectator = createPipe(`<div>{{ text | dotHighlight:search }}</div>`, {
            hostProps: {
                text: '',
                search: ''
            }
        });
        sanitizer = spectator.inject(DomSanitizer);
        jest.spyOn(sanitizer, 'bypassSecurityTrustHtml');
        const injector = spectator.inject(Injector);
        pipe = runInInjectionContext(injector, () => new DotHighlightPipe());
    });

    afterEach(() => {
        jest.clearAllMocks();
    });

    it('should create', () => {
        expect(spectator.element).toBeTruthy();
    });

    it('should return empty string when text is empty', () => {
        expect(pipe.transform('', 'test')).toBe('');
    });

    it('should return original text when search is null', () => {
        const text = 'Hello World';
        expect(pipe.transform(text, null)).toBe(text);
    });

    it('should highlight single occurrence of search term', () => {
        const text = 'Hello World';
        const search = 'World';
        const expected = 'Hello <span class="highlight">World</span>';

        pipe.transform(text, search);
        expect(sanitizer.bypassSecurityTrustHtml).toHaveBeenCalledWith(expected);
    });

    it('should highlight multiple occurrences of search term', () => {
        const text = 'Hello World, World';
        const search = 'World';
        const expected =
            'Hello <span class="highlight">World</span>, <span class="highlight">World</span>';

        pipe.transform(text, search);
        expect(sanitizer.bypassSecurityTrustHtml).toHaveBeenCalledWith(expected);
    });

    it('should handle case-insensitive search', () => {
        const text = 'Hello WORLD';
        const search = 'world';
        const expected = 'Hello <span class="highlight">WORLD</span>';

        pipe.transform(text, search);
        expect(sanitizer.bypassSecurityTrustHtml).toHaveBeenCalledWith(expected);
    });

    it('should escape special regex characters in search term', () => {
        const text = 'Hello (World)';
        const search = '(World)';
        const expected = 'Hello <span class="highlight">(World)</span>';

        pipe.transform(text, search);
        expect(sanitizer.bypassSecurityTrustHtml).toHaveBeenCalledWith(expected);
    });

    it('should handle non-string inputs by converting them to strings', () => {
        const text = 123;
        const search = '23';
        const expected = '1<span class="highlight">23</span>';

        pipe.transform(text as any, search);
        expect(sanitizer.bypassSecurityTrustHtml).toHaveBeenCalledWith(expected);
    });

    it('should update highlight when search term changes', () => {
        const text = 'Hello World';
        const expected = 'Hello <span class="highlight">World</span>';

        pipe.transform(text, 'World');
        expect(sanitizer.bypassSecurityTrustHtml).toHaveBeenCalledWith(expected);
    });

    describe('untrusted text', () => {
        /**
         * The result is handed to `bypassSecurityTrustHtml`, which switches Angular's sanitizer
         * off, and the text it wraps is author-supplied — content names, language variable keys,
         * experiment names and descriptions. Anything not escaped here reaches the DOM as live
         * markup, so these are the tests that keep this pipe from being an injection sink.
         */
        it('should escape markup in the text instead of emitting it', () => {
            pipe.transform('<img src=x onerror="alert(1)">Alpha', 'Alpha');

            expect(sanitizer.bypassSecurityTrustHtml).toHaveBeenCalledWith(
                '&lt;img src=x onerror=&quot;alert(1)&quot;&gt;<span class="highlight">Alpha</span>'
            );
        });

        it('should escape markup inside the matched part too', () => {
            pipe.transform('a <b>bold</b> word', '<b>bold</b>');

            expect(sanitizer.bypassSecurityTrustHtml).toHaveBeenCalledWith(
                'a <span class="highlight">&lt;b&gt;bold&lt;/b&gt;</span> word'
            );
        });

        it('should escape ampersands once, not double-escape them', () => {
            pipe.transform('Tom & Jerry', 'Tom');

            expect(sanitizer.bypassSecurityTrustHtml).toHaveBeenCalledWith(
                '<span class="highlight">Tom</span> &amp; Jerry'
            );
        });
    });
});

/**
 * Separate suite because the one above instantiates a TestBed in `beforeEach`, and a pipe host
 * template cannot be declared once that has happened.
 */
describe('DotHighlightPipe bound with innerHTML', () => {
    const createPipe = createPipeFactory({ pipe: DotHighlightPipe, providers: [] });

    it('should not put live nodes from the text into the DOM', () => {
        const spectator = createPipe(`<div [innerHTML]="text | dotHighlight:search"></div>`, {
            hostProps: {
                text: '<img src=x onerror="alert(1)">Alpha',
                search: 'Alpha'
            }
        });

        expect(spectator.element.querySelector('img')).toBeNull();
        expect(spectator.element.querySelector('span.highlight')?.textContent).toBe('Alpha');
    });
});
