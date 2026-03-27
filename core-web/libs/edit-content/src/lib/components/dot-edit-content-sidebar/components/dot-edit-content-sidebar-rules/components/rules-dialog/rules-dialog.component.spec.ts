import { byTestId, createComponentFactory, Spectator } from '@ngneat/spectator/jest';

import { CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';

import { DynamicDialogConfig } from 'primeng/dynamicdialog';

import { DotRulesDialogComponent, RulesDialogData } from './rules-dialog.component';

describe('DotRulesDialogComponent', () => {
    let spectator: Spectator<DotRulesDialogComponent>;

    const defaultData: RulesDialogData = {
        identifier: 'page-123'
    };

    const configRef: { data: RulesDialogData | null | undefined } = { data: defaultData };

    const createComponent = createComponentFactory({
        component: DotRulesDialogComponent,
        schemas: [CUSTOM_ELEMENTS_SCHEMA],
        providers: [
            {
                provide: DynamicDialogConfig,
                useValue: configRef
            }
        ],
        overrideComponents: [
            [
                DotRulesDialogComponent,
                {
                    set: {
                        imports: [],
                        providers: []
                    }
                }
            ]
        ]
    });

    beforeEach(() => {
        configRef.data = defaultData;
        spectator = createComponent();
    });

    it('should create', () => {
        expect(spectator.component).toBeTruthy();
    });

    describe('Elements by data-testId - Success', () => {
        it('should render rules-container when identifier is valid', () => {
            expect(spectator.query(byTestId('rules-container'))).toBeTruthy();
        });

        it('should NOT render rules-empty when data is valid', () => {
            expect(spectator.query(byTestId('rules-empty'))).toBeFalsy();
        });

        it('should expose the identifier from config data', () => {
            expect(spectator.component.identifier).toBe('page-123');
        });
    });

    describe('Elements by data-testId - Failure and Edge Cases', () => {
        it('should render rules-empty when data is undefined', () => {
            configRef.data = undefined;
            spectator = createComponent();
            spectator.detectChanges();

            expect(spectator.query(byTestId('rules-empty'))).toBeTruthy();
            expect(spectator.query(byTestId('rules-empty'))?.textContent?.trim()).toContain(
                'No content selected'
            );
            expect(spectator.query(byTestId('rules-container'))).toBeFalsy();
        });

        it('should render rules-empty when data is null', () => {
            configRef.data = null;
            spectator = createComponent();
            spectator.detectChanges();

            expect(spectator.query(byTestId('rules-empty'))).toBeTruthy();
            expect(spectator.query(byTestId('rules-container'))).toBeFalsy();
        });

        it('should render rules-empty when identifier is empty string', () => {
            configRef.data = { identifier: '' };
            spectator = createComponent();
            spectator.detectChanges();

            expect(spectator.query(byTestId('rules-empty'))).toBeTruthy();
            expect(spectator.query(byTestId('rules-container'))).toBeFalsy();
        });

        it('should render rules-empty when data is empty object', () => {
            configRef.data = {} as RulesDialogData;
            spectator = createComponent();
            spectator.detectChanges();

            expect(spectator.query(byTestId('rules-empty'))).toBeTruthy();
            expect(spectator.query(byTestId('rules-container'))).toBeFalsy();
        });
    });

    describe('identifier computed - Edge Cases', () => {
        it('should expose identifier value correctly for different identifiers', () => {
            configRef.data = { identifier: 'abc-xyz-789' };
            spectator = createComponent();
            spectator.detectChanges();

            expect(spectator.component.identifier).toBe('abc-xyz-789');
            expect(spectator.query(byTestId('rules-container'))).toBeTruthy();
        });
    });
});
