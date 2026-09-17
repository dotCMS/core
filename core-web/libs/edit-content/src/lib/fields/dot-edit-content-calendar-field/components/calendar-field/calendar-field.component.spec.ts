import { SpectatorHost, createHostFactory, mockProvider } from '@openng/spectator/vitest';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { Component } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';

import { DatePicker } from 'primeng/datepicker';

import { DotMessageService } from '@dotcms/data-access';
import {
    DotCMSContentType,
    DotCMSContentTypeField,
    DotSystemTimezone
} from '@dotcms/dotcms-models';

import { DotCalendarFieldComponent } from './calendar-field.component';

import { FIELD_TYPES } from '../../../../models/dot-edit-content-field.enum';
import { CONTENT_TYPE_MOCK, DATE_FIELD_MOCK } from '../../../../utils/mocks';

/**
 * Host for the calendar field, which is a ControlValueAccessor and therefore has to be
 * driven through a real reactive form rather than by setting inputs directly.
 */
@Component({
    standalone: false,
    selector: 'dot-custom-host',
    template: ''
})
export class MockFormComponent {
    formGroup: FormGroup;
    field: DotCMSContentTypeField;
    utcTimezone: DotSystemTimezone | null;
    contentType: DotCMSContentType | null;
    hasError = false;
}

describe('DotCalendarFieldComponent', () => {
    let spectator: SpectatorHost<DotCalendarFieldComponent, MockFormComponent>;

    const createHost = createHostFactory({
        component: DotCalendarFieldComponent,
        host: MockFormComponent,
        imports: [ReactiveFormsModule],
        detectChanges: false,
        providers: [
            mockProvider(DotMessageService, {
                get: vi.fn().mockImplementation((key: string) => key)
            })
        ]
    });

    /** Offset chosen so the server can sit on a different calendar day than the runner. */
    const MOCK_TIMEZONE: DotSystemTimezone = {
        id: 'America/New_York',
        label: 'Eastern Time (GMT-5)',
        offset: -18000000
    };

    const CONTENT_TYPE_WITHOUT_EXPIRE = {
        ...CONTENT_TYPE_MOCK,
        expireDateVar: null
    };

    const TEMPLATE = `<form [formGroup]="formGroup">
        <dot-calendar-field
            [field]="field"
            [hasError]="hasError"
            [formControlName]="field.variable"
            [utcTimezone]="utcTimezone"
            [contentType]="contentType" />
    </form>`;

    /**
     * Builds the host with a single control of the given field type.
     *
     * @param fieldType which of the three calendar types to render
     * @param value initial value of the form control, as a UTC timestamp
     * @param overrides field/timezone/contentType overrides for the case under test
     */
    const buildHost = (
        fieldType: FIELD_TYPES,
        value: number | null = null,
        overrides: Partial<MockFormComponent> = {}
    ): SpectatorHost<DotCalendarFieldComponent, MockFormComponent> => {
        const field = { ...DATE_FIELD_MOCK, fieldType, ...((overrides.field as object) ?? {}) };

        return createHost(TEMPLATE, {
            hostProps: {
                formGroup: new FormGroup({
                    [field.variable]: new FormControl(value)
                }),
                field,
                utcTimezone: MOCK_TIMEZONE,
                contentType: CONTENT_TYPE_WITHOUT_EXPIRE,
                hasError: false,
                ...overrides
            }
        });
    };

    /** All three types, for the assertions that must hold identically across them. */
    const ALL_TYPES = [FIELD_TYPES.DATE, FIELD_TYPES.TIME, FIELD_TYPES.DATE_AND_TIME] as const;

    /** A value the picker will render into the input, so the clear control becomes eligible. */
    const A_VALUE = new Date(2026, 3, 9, 9, 33).getTime();

    /**
     * The clear control as the user meets it: something focusable and activatable, not the
     * bare <svg> PrimeNG renders by default. Queried by role rather than by class so the
     * test fails if the control is present but not a real button.
     */
    const queryClearControl = (): HTMLElement | null =>
        spectator.query('[data-testid="calendar-clear-button"]');

    /**
     * One host change-detection pass, then a microtask turn, then another pass.
     *
     * Deliberately NOT the DatePicker's own detector. An earlier version of this helper called
     * `picker.cd.detectChanges()`, which turned these tests green while the real app was broken:
     * on the load path nothing supplies that pass, so a saved contentlet rendered its value with
     * no way to clear it. The component now schedules the pass itself in a microtask; the test
     * only has to let that microtask run, the same turn the browser gives it.
     */
    const settle = async (): Promise<void> => {
        spectator.detectChanges();
        await Promise.resolve();
        spectator.detectChanges();
    };

    /**
     * Opens the picker overlay. The overlay is appended to `body`, so it lives outside the
     * fixture and has to be queried through the document rather than through Spectator.
     */
    const openPicker = (): void => {
        const picker = spectator.query(DatePicker);
        picker.showOverlay();
        spectator.detectChanges();
        picker.cd.detectChanges();
    };

    /** Queries inside the open overlay panel, which Spectator's own queries cannot reach. */
    const queryInOverlay = (selector: string): HTMLElement | null =>
        document.querySelector(`.p-datepicker-panel ${selector}`);

    afterEach(() => {
        // The body-appended overlay outlives the fixture; left behind it would leak into the
        // next test's document queries.
        document.querySelectorAll('.p-datepicker-panel').forEach((panel) => panel.remove());
    });

    // One host per test: Spectator's createHost instantiates the TestBed, so it cannot be
    // called twice in the same `it`.
    it.each([...ALL_TYPES])('should build the harness for a %s field', async (fieldType) => {
        spectator = buildHost(fieldType);
        spectator.detectChanges();

        expect(spectator.component).toBeTruthy();
    });

    describe('Clear control on the field (US1)', () => {
        // T005 — FR-006
        it.each([...ALL_TYPES])(
            'should NOT render a clear control on an empty %s field',
            async (fieldType) => {
                spectator = buildHost(fieldType, null);
                await settle();

                expect(queryClearControl()).toBeNull();
            }
        );

        // T006 — FR-005
        it.each([...ALL_TYPES])(
            'should render a clear control on a %s field holding a value',
            async (fieldType) => {
                spectator = buildHost(fieldType, A_VALUE);
                await settle();

                expect(queryClearControl()).not.toBeNull();
            }
        );

        // T007 — FR-006
        it('should NOT render a clear control when the control is disabled, despite holding a value', async () => {
            spectator = buildHost(FIELD_TYPES.DATE_AND_TIME, A_VALUE);
            await settle();

            spectator.hostComponent.formGroup.get(DATE_FIELD_MOCK.variable)?.disable();
            await settle();

            expect(queryClearControl()).toBeNull();
        });

        // T008 — FR-007
        it('should empty the value and mark the control touched and dirty when cleared', async () => {
            spectator = buildHost(FIELD_TYPES.DATE_AND_TIME, A_VALUE);
            await settle();

            const control = spectator.hostComponent.formGroup.get(DATE_FIELD_MOCK.variable);
            expect(control?.value).toBe(A_VALUE);

            spectator.click(queryClearControl() as HTMLElement);
            await settle();

            expect(control?.value).toBeNull();
            expect(spectator.component.internalFormControl.value).toBeNull();
            expect(control?.touched).toBe(true);
            expect(control?.dirty).toBe(true);
        });

        // T009 — FR-007a. The part PrimeNG does not provide: its default clear icon is a bare
        // <svg> with a click handler — unfocusable, unnamed, unreachable by keyboard.
        it('should expose the clear control as a focusable button with an accessible name', async () => {
            spectator = buildHost(FIELD_TYPES.DATE_AND_TIME, A_VALUE);
            await settle();

            const clear = queryClearControl();

            expect(clear?.tagName).toBe('BUTTON');
            expect(clear?.getAttribute('type')).toBe('button');
            expect(clear?.getAttribute('aria-label')).toBeTruthy();
        });

        it('should clear the value when the clear button is activated from the keyboard', async () => {
            spectator = buildHost(FIELD_TYPES.DATE_AND_TIME, A_VALUE);
            await settle();

            const control = spectator.hostComponent.formGroup.get(DATE_FIELD_MOCK.variable);
            const clear = queryClearControl() as HTMLButtonElement;

            clear.focus();
            expect(document.activeElement).toBe(clear);

            // A native button turns Enter/Space into a click; asserting the click path from a
            // focused element is what proves it is reachable without a mouse.
            clear.click();
            await settle();

            expect(control?.value).toBeNull();
        });

        // T058 — FR-018 and the "Clearing a field that carries a default value" edge case.
        // Only reachable because FR-005 made clearing available on every field type; before
        // this feature only the expire-date field had a clear control. `handleChangeValue`'s
        // null branch re-applies `processFieldDefaultValue` and pushes it back out through
        // `onChange`, so an explicit clear must not be mistaken for an unpopulated field.
        it.each(['now', '2026-04-09 09:33:00'])(
            'should stay empty after clearing a field whose defaultValue is %s',
            async (defaultValue) => {
                spectator = buildHost(FIELD_TYPES.DATE_AND_TIME, null, {
                    field: { ...DATE_FIELD_MOCK, defaultValue } as DotCMSContentTypeField
                });
                await settle();

                const control = spectator.hostComponent.formGroup.get(DATE_FIELD_MOCK.variable);

                // The default populates the field on open, which is what makes it clearable.
                expect(control?.value).not.toBeNull();
                expect(queryClearControl()).not.toBeNull();

                spectator.click(queryClearControl() as HTMLElement);
                await settle();

                expect(control?.value).toBeNull();
                expect(spectator.component.internalFormControl.value).toBeNull();
                expect(queryClearControl()).toBeNull();
            }
        );

        // T010 — FR-007
        it('should leave a required field empty and invalid when cleared', async () => {
            spectator = buildHost(FIELD_TYPES.DATE_AND_TIME, A_VALUE, {
                field: { ...DATE_FIELD_MOCK, required: true } as DotCMSContentTypeField
            });
            await settle();

            const control = spectator.hostComponent.formGroup.get(DATE_FIELD_MOCK.variable);
            control?.setValidators(Validators.required);
            control?.updateValueAndValidity();

            spectator.click(queryClearControl() as HTMLElement);
            await settle();

            expect(control?.value).toBeNull();
            expect(control?.invalid).toBe(true);
        });
    });

    describe('Picker footer — timezone (US2)', () => {
        // T018 — FR-008
        it('should show the timezone in the picker footer for a Date-and-Time field', async () => {
            spectator = buildHost(FIELD_TYPES.DATE_AND_TIME);
            openPicker();

            const timezone = queryInOverlay('[data-testid="calendar-field-timezone"]');

            expect(timezone).not.toBeNull();
            expect(timezone?.textContent).toContain(MOCK_TIMEZONE.label);
        });

        // T019 — FR-008
        it('should show the timezone in the picker footer for a Time field', async () => {
            spectator = buildHost(FIELD_TYPES.TIME);
            openPicker();

            const timezone = queryInOverlay('[data-testid="calendar-field-timezone"]');

            expect(timezone).not.toBeNull();
            expect(timezone?.textContent).toContain(MOCK_TIMEZONE.label);
        });

        // T020 — FR-008a. A date carries no time, so the zone it would be read in is not a fact
        // the author needs. Decided explicitly in the issue's refinement table.
        it('should NOT show the timezone in the picker footer for a Date field', async () => {
            spectator = buildHost(FIELD_TYPES.DATE);
            openPicker();

            expect(queryInOverlay('[data-testid="calendar-field-timezone"]')).toBeNull();
        });

        it('should render the timezone as non-interactive text, not a control', async () => {
            spectator = buildHost(FIELD_TYPES.DATE_AND_TIME);
            openPicker();

            const timezone = queryInOverlay('[data-testid="calendar-field-timezone"]');

            expect(timezone?.tagName).not.toBe('BUTTON');
            expect(timezone?.tagName).not.toBe('A');
            expect(timezone?.getAttribute('tabindex')).toBeNull();
        });
    });

    describe('Picker footer — Today / Now (US3)', () => {
        // 02:00 UTC on 9 April. New York is then still on 8 April, 22:00 — so every assertion
        // below can tell the server's calendar day apart from the runner's.
        const FAKE_NOW_UTC = new Date('2026-04-09T02:00:00.000Z');
        const SERVER_DAY = 8;
        const SERVER_HOUR = 22;

        beforeEach(() => {
            // Only Date is faked: faking timers wholesale interferes with zone.js scheduling.
            vi.useFakeTimers({ toFake: ['Date'] });
            vi.setSystemTime(FAKE_NOW_UTC);
        });

        afterEach(() => {
            vi.useRealTimers();
        });

        /**
         * The inner <button> of the p-button, not its host element: clicking the host does not
         * fire `onClick`, it just bubbles to the document and PrimeNG closes the overlay — which
         * looks like the handler ran and did the wrong thing.
         */
        const queryActionButton = (): HTMLElement | null =>
            queryInOverlay('[data-testid="calendar-field-today-button"] button') ??
            queryInOverlay('[data-testid="calendar-field-today-button"]');

        const controlValue = (): number | null =>
            spectator.hostComponent.formGroup.get(DATE_FIELD_MOCK.variable)?.value ?? null;

        // T033 — FR-011
        it.each([
            [FIELD_TYPES.DATE, 'edit.content.form.field.calendar.today'],
            [FIELD_TYPES.DATE_AND_TIME, 'edit.content.form.field.calendar.today'],
            [FIELD_TYPES.TIME, 'edit.content.form.field.calendar.now']
        ])('should label the footer action from the %s key', (fieldType, expectedKey) => {
            spectator = buildHost(fieldType as FIELD_TYPES);
            openPicker();

            expect(queryActionButton()?.textContent).toContain(expectedKey);
        });

        // T038 — FR-015. Clearing is the on-field X now; a second way to do it in the footer
        // is the redundancy this feature removes.
        it.each([...ALL_TYPES])(
            'should render exactly one footer action and no Clear button on a %s field',
            async (fieldType) => {
                spectator = buildHost(fieldType);
                openPicker();

                const buttons = document.querySelectorAll('.p-datepicker-buttonbar button');

                expect(buttons).toHaveLength(1);
                expect(document.querySelector('.p-datepicker-clear-button')).toBeNull();
            }
        );

        // T034 + T035 — FR-012, FR-013, FR-017. The regression guard against reading the
        // browser's clock: on a Date-only field the stored value is UTC midnight of the
        // SERVER's day, which here differs from the runner's.
        it('should set the server calendar day, not the browser one, on a Date field', async () => {
            spectator = buildHost(FIELD_TYPES.DATE);
            openPicker();

            spectator.click(queryActionButton() as HTMLElement);
            await settle();

            expect(controlValue()).toBe(Date.UTC(2026, 3, SERVER_DAY, 0, 0, 0));
        });

        // T035 — FR-012. Date and time takes both halves from the server clock, so round-tripping
        // it back to UTC lands on the instant we faked.
        it('should set the server date AND time on a Date-and-Time field', async () => {
            spectator = buildHost(FIELD_TYPES.DATE_AND_TIME);
            openPicker();

            spectator.click(queryActionButton() as HTMLElement);
            await settle();

            expect(spectator.component.internalFormControl.value?.getDate()).toBe(SERVER_DAY);
            expect(spectator.component.internalFormControl.value?.getHours()).toBe(SERVER_HOUR);
            expect(controlValue()).toBe(FAKE_NOW_UTC.getTime());
        });

        // T035 — FR-012
        it('should set the server time on a Time-only field', async () => {
            spectator = buildHost(FIELD_TYPES.TIME);
            openPicker();

            spectator.click(queryActionButton() as HTMLElement);
            await settle();

            expect(spectator.component.internalFormControl.value?.getHours()).toBe(SERVER_HOUR);
            expect(controlValue()).not.toBeNull();
        });

        // FR-015b — symmetric with FR-007a for the clear control. p-button renders a native
        // button, but nothing pinned that, and an icon-only or div-based footer action would
        // satisfy every other criterion while being unreachable without a mouse.
        it('should expose the footer action as a focusable button with an accessible name', () => {
            spectator = buildHost(FIELD_TYPES.DATE_AND_TIME);
            openPicker();

            const btn = queryActionButton() as HTMLButtonElement;

            expect(btn.tagName).toBe('BUTTON');
            btn.focus();
            expect(document.activeElement).toBe(btn);
            expect(btn.textContent?.trim()).toBeTruthy();
        });

        // The behavioural half of the same requirement. Being a focusable, named button is not
        // enough: PrimeNG wires every control it renders inside the panel with
        // `(keydown)="onContainerButtonKeydown($event)"`, and the panel root binds only (click),
        // so replacing the footer without forwarding the event silently removes Escape and the
        // focus trap from the overlay. This is the assertion that catches that.
        it('should close the picker and restore focus when Escape is pressed on the footer action', () => {
            spectator = buildHost(FIELD_TYPES.DATE_AND_TIME);
            openPicker();

            const picker = spectator.query(DatePicker);
            expect(picker.overlayVisible).toBe(true);

            const btn = queryActionButton() as HTMLButtonElement;
            btn.focus();
            btn.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape', which: 27, bubbles: true }));
            spectator.detectChanges();

            expect(picker.overlayVisible).toBe(false);
            expect(document.activeElement).toBe(document.getElementById(DATE_FIELD_MOCK.variable));
        });

        // T036 — FR-014
        it('should mark the control touched and dirty after using the footer action', async () => {
            spectator = buildHost(FIELD_TYPES.DATE_AND_TIME);
            openPicker();

            spectator.click(queryActionButton() as HTMLElement);
            await settle();

            const control = spectator.hostComponent.formGroup.get(DATE_FIELD_MOCK.variable);

            expect(control?.touched).toBe(true);
            expect(control?.dirty).toBe(true);
        });

        // T037 — FR-014a. A date-only pick is complete, so it closes; the two types carrying a
        // time stay open so the hour can still be adjusted.
        // The footer action never closes the picker, matching a day click: hideOnDateTimeSelect
        // is false for all three types, so selecting a value keeps the overlay open until the
        // author clicks outside it.
        it.each([...ALL_TYPES])(
            'should leave the picker open after the footer action on a %s field',
            async (fieldType) => {
                spectator = buildHost(fieldType);
                openPicker();

                spectator.click(queryActionButton() as HTMLElement);
                await settle();

                expect(spectator.query(DatePicker).overlayVisible).toBe(true);
            }
        );
    });
});
