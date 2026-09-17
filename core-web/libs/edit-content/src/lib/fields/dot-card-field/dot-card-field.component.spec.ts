import { SpectatorHost, createHostFactory } from '@openng/spectator/vitest';

import { Component } from '@angular/core';

import { DotCardFieldContentComponent } from './components/dot-card-field-content.component';
import { DotCardFieldFooterComponent } from './components/dot-card-field-footer.component';
import { DotCardFieldLabelComponent } from './components/dot-card-field-label/dot-card-field-label.component';
import { DotCardFieldComponent } from './dot-card-field.component';

@Component({
    standalone: false,
    selector: 'dot-custom-host',
    template: ''
})
export class MockHostComponent {}

describe('DotCardFieldComponent', () => {
    let spectator: SpectatorHost<DotCardFieldComponent, MockHostComponent>;

    const createHost = createHostFactory({
        component: DotCardFieldComponent,
        host: MockHostComponent,
        imports: [
            DotCardFieldLabelComponent,
            DotCardFieldContentComponent,
            DotCardFieldFooterComponent
        ],
        detectChanges: false
    });

    const render = (hasError = false) => {
        spectator = createHost(
            `<dot-card-field [hasError]="${hasError}">
                <label dotCardFieldLabel variableName="title" [isRequired]="true">Title</label>
                <dot-card-field-content>
                    <input id="title" />
                </dot-card-field-content>
            </dot-card-field>`
        );
        spectator.detectChanges();
    };

    describe('the .field wrapper', () => {
        it('should wrap its projected content in a .field element', () => {
            render();

            expect(spectator.query('.field')).toBeTruthy();
        });

        it('should NOT keep the hand-rolled flex layout the global .field class replaces', () => {
            render();

            expect(spectator.query('.flex.flex-col.gap-2')).toBeNull();
        });
    });

    /**
     * The load-bearing assertion of #37460.
     *
     * `.form .field > label` in apps/dotcms-ui/src/style.css is a DIRECT-CHILD selector. While the
     * label sits inside a `<dot-card-field-label>` host element, that host is the label's DOM
     * parent and the rule never matches — the label keeps rendering at the inherited 14px/400,
     * which is the reported bug.
     *
     * This asserts the structural precondition only. It cannot assert that the rule APPLIES: the
     * test run compiles no CSS at all (the global sheet is an application-level Tailwind build,
     * and vite.config.mts installs stubScss()), so getComputedStyle returns the jsdom default
     * either way. AC-105 covers that in the browser.
     *
     * Worth having anyway, precisely because `display: contents` on the old wrapper would fix the
     * LAYOUT while leaving this relationship — and the typography — unchanged. That is the failure
     * that looks like success, and this test is what refuses it.
     */
    describe('label placement (AC-105 precondition)', () => {
        it('should render the label as a DIRECT child of .field', () => {
            render();

            const label = spectator.query('label');

            expect(label).toBeTruthy();
            expect(label.parentElement.classList.contains('field')).toBe(true);
        });

        it('should not interpose any element between .field and the label', () => {
            render();

            const field = spectator.query('.field');

            expect(field.querySelector(':scope > label')).toBeTruthy();
        });
    });

    describe('the error marker', () => {
        it('should render the scroll anchor when the field has an error', () => {
            render(true);

            expect(spectator.query('.field-error-marker')).toBeTruthy();
        });

        it('should not render the scroll anchor when the field has no error', () => {
            render(false);

            expect(spectator.query('.field-error-marker')).toBeNull();
        });
    });

    /**
     * AC-209, second half — the control itself must say it is mandatory.
     *
     * The red asterisk is decorative `::after` content: it is deliberately absent from the label's
     * accessible name, which is what stops a screen reader announcing "star". That leaves nothing
     * conveying "required" to assistive technology unless the control carries it.
     *
     * Set here rather than in the eighteen field templates because the control is whatever each
     * field projects — a native input, a PrimeNG combobox, a contenteditable — and a11y should not
     * depend on each template remembering.
     */
    describe('required state for assistive technology (AC-209)', () => {
        const renderWith = (isRequired: boolean, control = '<input id="title" />') => {
            spectator = createHost(
                `<dot-card-field [hasError]="false" [isRequired]="${isRequired}">
                    <label dotCardFieldLabel variableName="title" [isRequired]="${isRequired}">Title</label>
                    <dot-card-field-content>${control}</dot-card-field-content>
                </dot-card-field>`
            );
            spectator.detectChanges();
        };

        it('should mark a required field control as required', () => {
            renderWith(true);

            expect(spectator.query('input').getAttribute('aria-required')).toBe('true');
        });

        it('should not mark a control that is not required', () => {
            renderWith(false);

            expect(spectator.query('input').getAttribute('aria-required')).toBeNull();
        });

        it('should keep the asterisk out of the accessible name', () => {
            renderWith(true);

            expect(spectator.query('label').textContent).not.toContain('*');
        });

        it('should reach a composite widget whose focusable element is not a native input', () => {
            renderWith(true, '<div id="title" role="combobox" tabindex="0"></div>');

            expect(spectator.query('[role="combobox"]').getAttribute('aria-required')).toBe('true');
        });

        it('should mark nothing rather than guess when the label points at no element', () => {
            renderWith(true, '<span role="combobox" tabindex="0"></span><textarea></textarea>');

            expect(spectator.query('[role="combobox"]').getAttribute('aria-required')).toBeNull();
            expect(spectator.query('textarea').getAttribute('aria-required')).toBeNull();
        });

        // Text Area and WYSIWYG both render an editor-mode dropdown ABOVE their textarea, so
        // "first focusable descendant" marks the wrong control on exactly the fields where it
        // matters most. The label's `for` names the real one.
        it('should mark the control the label points at, not the first focusable one', () => {
            renderWith(
                true,
                '<span role="combobox" tabindex="0"></span><textarea id="title"></textarea>'
            );

            expect(spectator.query('textarea').getAttribute('aria-required')).toBe('true');
            expect(spectator.query('[role="combobox"]').getAttribute('aria-required')).toBeNull();
        });
    });
});
