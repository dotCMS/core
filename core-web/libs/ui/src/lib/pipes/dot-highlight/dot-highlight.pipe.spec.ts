/* eslint-disable @typescript-eslint/no-explicit-any */

import { createPipeFactory, SpectatorPipe } from '@ngneat/spectator/jest';

import { DomSanitizer } from '@angular/platform-browser';

import { DotHighlightPipe } from './dot-highlight.pipe';

describe('DotHighlightPipe', () => {
    let spectator: SpectatorPipe<DotHighlightPipe>;
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
    });

    afterEach(() => {
        jest.clearAllMocks();
    });

    it('should create', () => {
        expect(spectator.element).toBeTruthy();
    });

    it('should return empty string when text is empty', () => {
        spectator.setHostInput({
            text: '',
            search: 'test'
        });
        spectator.detectChanges();
        expect(spectator.element.textContent).toBe('');
    });

    it('should return original text when search is null', () => {
        const text = 'Hello World';
        spectator.setHostInput({
            text: text,
            search: null
        });
        spectator.detectChanges();
        expect(spectator.element.textContent).toBe(text);
    });

    it('should highlight single occurrence of search term', () => {
        const text = 'Hello World';
        const search = 'World';
        const expected = 'Hello <span class="highlight">World</span>';

        spectator.setHostInput({
            text: text,
            search: search
        });

        spectator.detectChanges();
        expect(sanitizer.bypassSecurityTrustHtml).toHaveBeenCalledWith(expected);
    });

    it('should highlight multiple occurrences of search term', () => {
        const text = 'Hello World, World';
        const search = 'World';
        const expected =
            'Hello <span class="highlight">World</span>, <span class="highlight">World</span>';

        spectator.setHostInput({
            text: text,
            search: search
        });

        spectator.detectChanges();
        expect(sanitizer.bypassSecurityTrustHtml).toHaveBeenCalledWith(expected);
    });

    it('should handle case-insensitive search', () => {
        const text = 'Hello WORLD';
        const search = 'world';
        const expected = 'Hello <span class="highlight">WORLD</span>';

        spectator.setHostInput({
            text: text,
            search: search
        });

        spectator.detectChanges();
        expect(sanitizer.bypassSecurityTrustHtml).toHaveBeenCalledWith(expected);
    });

    it('should escape special regex characters in search term', () => {
        const text = 'Hello (World)';
        const search = '(World)';
        const expected = 'Hello <span class="highlight">(World)</span>';

        spectator.setHostInput({
            text: text,
            search: search
        });

        spectator.detectChanges();
        expect(sanitizer.bypassSecurityTrustHtml).toHaveBeenCalledWith(expected);
    });

    it('should handle non-string inputs by converting them to strings', () => {
        const text = 123;
        const search = '23';
        const expected = '1<span class="highlight">23</span>';

        spectator.setHostInput({
            text: text as any,
            search: search
        });

        spectator.detectChanges();
        expect(sanitizer.bypassSecurityTrustHtml).toHaveBeenCalledWith(expected);
    });

    it('should update highlight when search term changes', () => {
        const text = 'Hello World';
        const expected = 'Hello <span class="highlight">World</span>';

        spectator.setHostInput({
            text: text,
            search: 'World'
        });

        spectator.detectChanges();
        expect(sanitizer.bypassSecurityTrustHtml).toHaveBeenCalledWith(expected);
    });
});
