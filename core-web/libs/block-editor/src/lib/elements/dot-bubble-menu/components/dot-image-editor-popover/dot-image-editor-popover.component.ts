import { Props } from 'tippy.js';

import { Component, ElementRef, HostListener, input, ViewChild } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';

import { Editor } from '@tiptap/core';

import { EditorModalDirective } from '../../../../directive/editor-modal.directive';

/**
 * A popover component for editing image properties in the DotCMS block editor.
 * This component provides a form interface to modify image attributes such as src, alt text, and title.
 */
@Component({
    selector: 'dot-image-editor-popover',
    templateUrl: './dot-image-editor-popover.component.html',
    styleUrls: ['./dot-image-editor-popover.component.scss'],
    imports: [EditorModalDirective, InputTextModule, ReactiveFormsModule, ButtonModule]
})
export class DotImageEditorPopoverComponent {
    @ViewChild('popover', { read: EditorModalDirective })
    private readonly popover: EditorModalDirective;
    @ViewChild('input', { read: ElementRef })
    private readonly urlInput?: ElementRef<HTMLInputElement>;

    /**
     * TipTap editor instance passed from parent.
     */
    readonly editor = input.required<Editor>();

    protected readonly imageForm = new FormGroup({
        src: new FormControl('', Validators.required),
        alt: new FormControl(''),
        title: new FormControl('')
    });

    protected readonly tippyOptions: Partial<Props> = {
        onShow: this.initializeFormWithImageData.bind(this),
        onShown: this.focusSearchInput.bind(this)
    };

    @HostListener('document:keydown.escape', ['$event'])
    protected onEscapeKey(event: KeyboardEvent) {
        if (event.key === 'Escape') {
            this.cancelImageEditing();
        }
    }

    /**
     * The native element of the Tippy instance.
     */
    get tippyElement() {
        return this.popover?.nativeElement;
    }

    /**
     * Toggles the visibility of the image editor popover.
     */
    toggle() {
        this.popover?.toggle();
    }

    /**
     * Shows the image editor popover.
     */
    show() {
        this.popover?.show();
    }

    /**
     * Hides the image editor popover.
     */
    hide() {
        this.popover?.hide();
    }

    /**
     * Saves the image changes to the editor.
     */
    protected saveImageChanges() {
        if (!this.imageForm.valid) {
            return;
        }

        const { src, alt, title } = this.imageForm.value;

        this.editor()
            .chain()
            .focus()
            .updateAttributes('dotImage', {
                src,
                alt,
                title
            })
            .run();

        this.popover?.hide();
    }

    /**
     * Cancels the image editing process and resets the form.
     */
    protected cancelImageEditing() {
        this.imageForm.reset();
        this.popover?.hide();
    }

    /**
     * Initializes the form with the image data from the editor.
     */
    protected initializeFormWithImageData() {
        const { src, alt, title } = this.getDotImageAttributes();

        this.imageForm.patchValue({
            src: src || '',
            alt: alt || '',
            title: title || ''
        });
    }

    /**
     * Gets the image attributes from the editor.
     * @returns The image attributes.
     */
    private getDotImageAttributes() {
        const { alt, src, title, data } = this.editor().getAttributes('dotImage');
        const { title: dotTitle = '', asset = '' } = data || {};

        return {
            src: src || asset,
            alt: alt || dotTitle,
            title: title || dotTitle
        };
    }

    /**
     * Focuses the search input.
     */
    private focusSearchInput() {
        this.urlInput?.nativeElement.focus();
    }
}
