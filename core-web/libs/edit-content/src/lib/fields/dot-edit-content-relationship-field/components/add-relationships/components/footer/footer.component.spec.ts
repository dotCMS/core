import { Spectator, byTestId, createComponentFactory } from '@openng/spectator/vitest';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { DynamicDialogConfig } from 'primeng/dynamicdialog';

import { DotMessageService } from '@dotcms/data-access';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { AddRelationshipsFooterComponent } from './footer.component';

/**
 * US2 — the confirm action is never disabled (FR-013).
 *
 * This is implemented by *removing* a binding rather than adding a mechanism: the dialog this
 * replaces carried `[disabled]="totalItems === 0"`, which made "uncheck the last row" the single
 * case where unchecking did not unrelate. Content Drive's own footer already had it right, with the
 * comment "clearing is a valid filter state".
 */
describe('AddRelationshipsFooterComponent (US2)', () => {
    let spectator: Spectator<AddRelationshipsFooterComponent>;

    const messageServiceMock = new MockDotMessageService({
        'dot.common.dialog.reject': 'Cancel',
        'dot.relationship.add.dialog.confirm': 'Add Relationships',
        'content-drive.field-filter.apply': 'Apply'
    });

    /**
     * Mutable rather than re-provided per test: the TestBed is instantiated by the time a test body
     * runs, so overriding `DynamicDialogConfig` there throws.
     */
    let dialogData: { confirmLabel?: string } = {};

    const createComponent = createComponentFactory({
        component: AddRelationshipsFooterComponent,
        providers: [
            { provide: DotMessageService, useValue: messageServiceMock },
            {
                provide: DynamicDialogConfig,
                useValue: {
                    get data() {
                        return dialogData;
                    }
                }
            }
        ]
    });

    beforeEach(() => {
        dialogData = {};
        spectator = createComponent();
        spectator.detectChanges();
    });

    it('renders the confirm action enabled with an empty selection', () => {
        const confirm = spectator
            .query(byTestId('add-relationships-confirm'))
            ?.querySelector('button');

        expect(confirm).toBeTruthy();
        expect(confirm?.disabled).toBe(false);
    });

    it('carries no disabled binding at all on the confirm action', () => {
        const confirm = spectator.query(byTestId('add-relationships-confirm'));

        expect(confirm?.getAttribute('ng-reflect-disabled')).toBeNull();
    });

    it('emits confirm when pressed with an empty selection, so the relationship can be emptied', () => {
        const confirmed = vi.fn();
        spectator.output('confirm').subscribe(confirmed);

        const button = spectator
            .query(byTestId('add-relationships-confirm'))
            ?.querySelector('button');
        spectator.click(button as HTMLElement);

        expect(confirmed).toHaveBeenCalled();
    });

    /**
     * The second consumer's only hook into this footer.
     *
     * Content Drive's field-filter chip opens the same dialog to pick filter values, where "Add
     * Relationships" would be wrong — it passes `confirmLabel` through `DynamicDialogConfig` and
     * gets "Apply". That is the whole reason the two footers collapsed into one, and nothing
     * exercised it.
     */
    it('renders the confirm label the caller asked for', () => {
        dialogData = { confirmLabel: 'content-drive.field-filter.apply' };
        spectator = createComponent();
        spectator.detectChanges();

        const confirm = spectator.query(byTestId('add-relationships-confirm'));

        expect(confirm?.textContent?.trim()).toBe('Apply');
    });

    it('falls back to its own label when the caller passes none', () => {
        const confirm = spectator.query(byTestId('add-relationships-confirm'));

        expect(confirm?.textContent?.trim()).toBe('Add Relationships');
    });

    it('emits cancel when the cancel action is pressed', () => {
        const cancelled = vi.fn();
        spectator.output('cancel').subscribe(cancelled);

        const button = spectator
            .query(byTestId('add-relationships-cancel'))
            ?.querySelector('button');
        spectator.click(button as HTMLElement);

        expect(cancelled).toHaveBeenCalled();
    });
});
