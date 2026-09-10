import { byTestId, createComponentFactory, Spectator } from '@openng/spectator/jest';

import { FormArray, FormControl, FormGroup } from '@angular/forms';

import { DotMessageService } from '@dotcms/data-access';
import { MockDotMessageService } from '@dotcms/utils-testing';

import {
    DotAiAdditionalPropertiesComponent,
    DotAiAdditionalPropertyGroup
} from './dot-ai-additional-properties.component';

describe('DotAiAdditionalPropertiesComponent', () => {
    let spectator: Spectator<DotAiAdditionalPropertiesComponent>;
    let properties: FormArray<DotAiAdditionalPropertyGroup>;

    const createComponent = createComponentFactory({
        component: DotAiAdditionalPropertiesComponent,
        providers: [{ provide: DotMessageService, useValue: new MockDotMessageService({}) }]
    });

    function propertyRow(key: string, value: string): DotAiAdditionalPropertyGroup {
        return new FormGroup({
            key: new FormControl(key, { nonNullable: true }),
            value: new FormControl(value, { nonNullable: true })
        });
    }

    /**
     * Rows have to exist before the first render. The component is OnPush and `properties` is a
     * signal holding the FormArray, so pushing into that array from outside mutates it without
     * changing the signal and without marking the component dirty. In the app the rows arrive the
     * same way — hydrated into the array by the parent card before it renders.
     */
    function setup(rows: DotAiAdditionalPropertyGroup[] = []): void {
        properties = new FormArray<DotAiAdditionalPropertyGroup>(rows);
        spectator = createComponent({ props: { properties } });
    }

    /** p-button renders its own `<button>`; the testid sits on the `<p-button>` host. */
    function clickButton(testId: string): void {
        const button = spectator.query(byTestId(testId))?.querySelector('button');
        expect(button).toBeTruthy();
        spectator.click(button as HTMLElement);
    }

    describe('rendering', () => {
        it('renders one key/value pair per control in the array', () => {
            setup([propertyRow('listenerIndexer', 'true'), propertyRow('customFlag', 'yes')]);

            expect(spectator.query(byTestId('property-key-0'))).toExist();
            expect(spectator.query(byTestId('property-value-0'))).toExist();
            expect(spectator.query(byTestId('property-key-1'))).toExist();
            expect(spectator.query(byTestId('remove-property-1'))).toExist();
        });

        it('renders no rows and only the add button when the array is empty', () => {
            setup();

            expect(spectator.query(byTestId('property-key-0'))).not.toExist();
            expect(spectator.query(byTestId('add-property'))).toExist();
        });

        it('binds each row to its own control', () => {
            setup([propertyRow('listenerIndexer', 'true')]);

            const key = spectator.query(byTestId('property-key-0')) as HTMLInputElement;
            const value = spectator.query(byTestId('property-value-0')) as HTMLInputElement;

            expect(key.value).toBe('listenerIndexer');
            expect(value.value).toBe('true');
        });
    });

    // The icons are projected through `<ng-template #icon>` rather than the `icon` input, which is
    // a different p-button rendering path — these cover that clicks still reach the handlers, and
    // that the icon-only button keeps the shape that path is required for.
    describe('add / remove', () => {
        it('appends an empty row when the add button is pressed', () => {
            setup();

            clickButton('add-property');

            expect(properties.length).toBe(1);
            expect(properties.at(0).value).toEqual({ key: '', value: '' });
        });

        it('removes only the pressed row', () => {
            setup([propertyRow('keep', 'a'), propertyRow('drop', 'b')]);

            clickButton('remove-property-1');

            expect(properties.length).toBe(1);
            expect(properties.at(0).value.key).toBe('keep');
        });

        it('keeps the remove button icon-only, which needs the `#icon` template path', () => {
            setup([propertyRow('listenerIndexer', 'true')]);

            const button = spectator.query(byTestId('remove-property-0'))?.querySelector('button');

            // `p-button-icon-only` is derived from `hasIcon && !label`, and `hasIcon` counts the
            // `icon` input or an `#icon` template — bare projected content satisfies neither.
            expect(button?.classList.contains('p-button-icon-only')).toBe(true);
        });
    });

    describe('accessible names', () => {
        it('names the remove button, which has no visible label', () => {
            setup([propertyRow('listenerIndexer', 'true')]);

            const button = spectator.query(byTestId('remove-property-0'))?.querySelector('button');

            expect(button?.getAttribute('aria-label')).toBe(
                'apps.ai.additional-properties.remove.aria-label'
            );
        });
    });
});
