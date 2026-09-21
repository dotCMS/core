import { SpectatorHost, createHostFactory } from '@openng/spectator/vitest';

import { Component } from '@angular/core';

import { DotCardFieldLabelComponent } from './dot-card-field-label.component';

@Component({
    standalone: false,
    selector: 'dot-custom-host',
    template: ''
})
export class MockHostComponent {}

describe('DotCardFieldLabelComponent', () => {
    let spectator: SpectatorHost<DotCardFieldLabelComponent, MockHostComponent>;

    const createHost = createHostFactory({
        component: DotCardFieldLabelComponent,
        host: MockHostComponent,
        detectChanges: false
    });

    const render = (isRequired: boolean) => {
        spectator = createHost(
            `<label dotCardFieldLabel variableName="title" [isRequired]="${isRequired}">Title</label>`
        );
        spectator.detectChanges();
    };

    /**
     * The component's host element must BE the <label>, not wrap one. See the direct-child
     * reasoning in dot-card-field.component.spec.ts and research R1.
     */
    describe('host element', () => {
        it('should be the label itself rather than a wrapper around one', () => {
            render(false);

            const host = spectator.element as HTMLElement;

            expect(host.tagName.toLowerCase()).toBe('label');
            expect(host.querySelector('label')).toBeNull();
        });

        it('should project its content', () => {
            render(false);

            expect(spectator.element.textContent.trim()).toBe('Title');
        });

        it('should point `for` at the field variable', () => {
            render(false);

            expect(spectator.element.getAttribute('for')).toBe('title');
        });

        it('should expose a stable id so composite widgets can name themselves by it', () => {
            render(false);

            // `<label for>` only associates with labelable elements. A radio group or a checkbox
            // group is a div, so it has to be named with aria-labelledby pointing back here.
            expect(spectator.element.getAttribute('id')).toBe('label-title');
        });

        it('should keep the data-testid format specs across the library select on', () => {
            render(false);

            expect(spectator.element.getAttribute('data-testid')).toBe('label-title');
        });
    });

    /**
     * The asterisk comes from `.p-label-input-required::after` and is applied by the
     * dotFieldRequired directive in BARE mode.
     *
     * Bare mode, not checkIsRequiredControl: that mode reads Validators.required off the
     * FormGroup, but a required Block Editor uses blockEditorRequiredValidator() instead, so it
     * would silently drop the asterisk. The source of truth is the content type's field.required,
     * which reaches this component as `isRequired`. T-02 guards that case end to end.
     */
    describe('required indicator (AC-209)', () => {
        it('should mark the label as required when the field is required', () => {
            render(true);

            expect(spectator.element.classList.contains('p-label-input-required')).toBe(true);
        });

        it('should not mark the label when the field is not required', () => {
            render(false);

            expect(spectator.element.classList.contains('p-label-input-required')).toBe(false);
        });
    });

    /**
     * AC-204: hints are plain text below the control, never a tooltip and never an icon.
     */
    describe('hint (AC-204)', () => {
        it('should not render a tooltip icon', () => {
            render(false);

            expect(spectator.query('i.pi-info-circle')).toBeNull();
        });
    });
});
