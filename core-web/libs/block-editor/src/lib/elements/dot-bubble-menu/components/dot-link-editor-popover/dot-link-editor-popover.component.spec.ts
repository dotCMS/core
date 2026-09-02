import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { Component, viewChild } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { Editor } from '@tiptap/core';
import { Document } from '@tiptap/extension-document';
import { Link } from '@tiptap/extension-link';
import { Paragraph } from '@tiptap/extension-paragraph';
import { Text } from '@tiptap/extension-text';

import { DotLinkEditorPopoverComponent } from './dot-link-editor-popover.component';

@Component({
    template: `
        <dot-link-editor-popover [editor]="editor" />
    `,
    imports: [DotLinkEditorPopoverComponent]
})
class TestHostComponent {
    editor: Editor;
    popover = viewChild.required(DotLinkEditorPopoverComponent);

    constructor() {
        this.editor = new Editor({
            extensions: [
                Document,
                Paragraph,
                Text,
                Link.extend({
                    addAttributes() {
                        return {
                            ...this.parent?.(),
                            title: { default: null },
                            'aria-label': { default: null },
                            rel: { default: null }
                        };
                    }
                }).configure({ autolink: false, openOnClick: false })
            ],
            content: '<p>Hello <a href="https://example.com" target="_blank">world</a></p>'
        });
    }
}

function mockEditorChain(editor: Editor) {
    const runSpy = jest.fn();
    const setLinkSpy = jest.fn().mockReturnValue({ run: runSpy });
    const focusSpy = jest.fn().mockReturnValue({ setLink: setLinkSpy });
    jest.spyOn(editor, 'chain').mockReturnValue({ focus: focusSpy } as never);

    return { setLinkSpy, focusSpy, runSpy };
}

describe('DotLinkEditorPopoverComponent', () => {
    let fixture: ComponentFixture<TestHostComponent>;
    let hostComponent: TestHostComponent;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [DotLinkEditorPopoverComponent, TestHostComponent],
            providers: [provideHttpClient(), provideHttpClientTesting()]
        }).compileComponents();

        fixture = TestBed.createComponent(TestHostComponent);
        hostComponent = fixture.componentInstance;
        fixture.detectChanges();
    });

    afterEach(() => {
        hostComponent.editor.destroy();
    });

    it('should create the component', () => {
        expect(hostComponent.popover()).toBeTruthy();
    });

    it('should expose target options with all standard values', () => {
        const component = hostComponent.popover();
        const values = component.targetOptions.map((o) => o.value);
        expect(values).toEqual(['_blank', '_self', '_parent', '_top']);
    });

    it('should expose rel options with predefined values', () => {
        const component = hostComponent.popover();
        const values = component.relOptions.map((o) => o.value);
        expect(values).toContain('noopener noreferrer');
        expect(values).toContain('nofollow');
        expect(values).toContain('sponsored');
        expect(values).toContain('ugc');
    });

    it('should initialize with default signal values', () => {
        const component = hostComponent.popover();
        expect(component['linkTitle']()).toBe('');
        expect(component['linkAriaLabel']()).toBe('');
        expect(component['linkRel']()).toBeNull();
        expect(component['showAdvanced']()).toBe(false);
        expect(component['linkTargetAttribute']()).toBe('_blank');
    });

    describe('addLinkToNode', () => {
        beforeEach(() => {
            const component = hostComponent.popover();
            component['popover'] = { hide: jest.fn() } as never;
        });

        it('should call setLink with all accessibility attributes', () => {
            const component = hostComponent.popover();
            const editor = hostComponent.editor;
            const { setLinkSpy } = mockEditorChain(editor);
            jest.spyOn(editor, 'isActive').mockReturnValue(false);

            component['linkTargetAttribute'].set('_self');
            component['linkTitle'].set('My Title');
            component['linkAriaLabel'].set('Click here');
            component['linkRel'].set('nofollow');

            component['addLinkToNode']('https://example.com');

            expect(setLinkSpy).toHaveBeenCalledWith({
                href: 'https://example.com',
                target: '_self',
                title: 'My Title',
                'aria-label': 'Click here',
                rel: 'nofollow'
            });
        });

        it('should trim whitespace-only values to null', () => {
            const component = hostComponent.popover();
            const editor = hostComponent.editor;
            const { setLinkSpy } = mockEditorChain(editor);
            jest.spyOn(editor, 'isActive').mockReturnValue(false);

            component['linkTitle'].set('   ');
            component['linkAriaLabel'].set('  ');
            component['linkRel'].set(null);

            component['addLinkToNode']('https://example.com');

            expect(setLinkSpy).toHaveBeenCalledWith(
                expect.objectContaining({
                    title: null,
                    'aria-label': null,
                    rel: null
                })
            );
        });
    });

    describe('saveLinkAttributes', () => {
        beforeEach(() => {
            const component = hostComponent.popover();
            component['popover'] = { hide: jest.fn() } as never;
        });

        it('should save all attributes to an existing link', () => {
            const component = hostComponent.popover();
            const editor = hostComponent.editor;
            const { setLinkSpy } = mockEditorChain(editor);

            component['existingLinkUrl'].set('https://dotcms.com');
            component['linkTargetAttribute'].set('_blank');
            component['linkTitle'].set('dotCMS');
            component['linkAriaLabel'].set('Visit dotCMS');
            component['linkRel'].set('noopener noreferrer');

            component['saveLinkAttributes']();

            expect(setLinkSpy).toHaveBeenCalledWith({
                href: 'https://dotcms.com',
                target: '_blank',
                title: 'dotCMS',
                'aria-label': 'Visit dotCMS',
                rel: 'noopener noreferrer'
            });
        });
    });

    describe('initializeExistingLinkData', () => {
        it('should auto-expand advanced section when existing link has accessibility attrs', () => {
            const component = hostComponent.popover();
            const editor = hostComponent.editor;

            jest.spyOn(editor, 'isActive').mockReturnValue(true);
            jest.spyOn(editor, 'getAttributes').mockReturnValue({
                href: 'https://dotcms.com',
                target: '_self',
                title: 'My title',
                'aria-label': 'Visit site',
                rel: 'nofollow'
            });

            component['initializeExistingLinkData']();

            expect(component['showAdvanced']()).toBe(true);
            expect(component['linkTitle']()).toBe('My title');
            expect(component['linkAriaLabel']()).toBe('Visit site');
            expect(component['linkRel']()).toBe('nofollow');
            expect(component['linkTargetAttribute']()).toBe('_self');
        });

        it('should keep advanced section collapsed when no accessibility attrs exist', () => {
            const component = hostComponent.popover();
            const editor = hostComponent.editor;

            jest.spyOn(editor, 'isActive').mockReturnValue(true);
            jest.spyOn(editor, 'getAttributes').mockReturnValue({
                href: 'https://example.com',
                target: '_blank'
            });

            component['initializeExistingLinkData']();

            expect(component['showAdvanced']()).toBe(false);
            expect(component['linkTitle']()).toBe('');
            expect(component['linkAriaLabel']()).toBe('');
            expect(component['linkRel']()).toBeNull();
        });
    });
});
