import { Component, ElementRef, HostListener, input, ViewChild } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';

import { Editor } from '@tiptap/core';

import { EditorModalDirective } from '../../../../directive/editor-modal.directive';

/**
 * A popover component for editing image properties in the DotCMS block editor.
 * This component provides a form interface to modify image attributes such as src, alt text, and title.
 * It integrates with the TipTap editor to update image node attributes.
 */
@Component({
    selector: 'dot-image-editor-popover',
    templateUrl: './dot-image-editor-popover.component.html',
    styleUrls: ['./dot-image-editor-popover.component.scss'],
    standalone: true,
    imports: [EditorModalDirective, InputTextModule, ReactiveFormsModule, ButtonModule]
})
export class DotImageEditorPopoverComponent {
    @ViewChild('imagePopover', { read: EditorModalDirective }) imagePopover: EditorModalDirective;
    @ViewChild('input', { read: ElementRef }) urlInput?: ElementRef<HTMLInputElement>;
    readonly editor = input.required<Editor>();
    readonly appendTo = input<HTMLElement>();

    protected readonly imageForm = new FormGroup({
        src: new FormControl('', Validators.required),
        alt: new FormControl(''),
        title: new FormControl('')
    });

    protected readonly tippyOptions = {
        onShow: this.initializeFormWithImageData.bind(this),
        onShown: this.focusSearchInput.bind(this)
    };

    @HostListener('document:keydown.escape', ['$event'])
    protected onEscapeKey(event: KeyboardEvent) {
        if (event.key === 'Escape') {
            this.imagePopover?.hide();
        }
    }

    /**
     * Saves the form values and updates the image attributes in the editor.
     * Validates the form before applying changes and hides the popover on success.
     * Updates the 'dotImage' node with the new src, alt, and title attributes.
     */
    protected saveImageChanges() {
        if (!this.imageForm.valid) {
            return;
        }

        this.editor()
            .chain()
            .focus()
            .updateAttributes('dotImage', {
                src: this.imageForm.value.src,
                alt: this.imageForm.value.alt,
                title: this.imageForm.value.title
            })
            .run();

        this.imagePopover?.hide();
    }

    /**
     * Cancels the image editing operation and closes the popover.
     * Does not save any changes made to the form.
     */
    protected cancelImageEditing() {
        this.imagePopover?.hide();
    }

    /**
     * Toggles the visibility of the image editor popover.
     * Can be used to both show and hide the popover.
     */
    protected toggle() {
        this.imagePopover?.toggle();
    }

    /**
     * Initializes the form with current image attributes when the popover is shown.
     * Extracts image data from the selected dotImage node and populates the form fields.
     * Handles both direct image attributes and nested data properties.
     */
    protected initializeFormWithImageData() {
        const { alt, src, title, data } = this.editor().getAttributes('dotImage');
        const { title: dotTitle = '', asset } = data || {};
        this.imageForm.patchValue({
            src: src || asset,
            alt: alt || dotTitle,
            title: title || dotTitle
        });
    }

    /**
     * Sets focus to the search input field and highlights the current selection in the editor.
     * Called when the popover is shown to provide immediate user interaction feedback.
     */
    private focusSearchInput() {
        this.urlInput?.nativeElement.focus();
    }
}
