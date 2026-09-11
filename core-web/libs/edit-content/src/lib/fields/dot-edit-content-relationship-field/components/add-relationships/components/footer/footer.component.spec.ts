import { describe, expect } from '@jest/globals';
import { Spectator, byTestId, createComponentFactory, mockProvider } from '@openng/spectator/jest';

import { DotMessageService } from '@dotcms/data-access';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { AddRelationshipsFooterComponent } from './footer.component';

import { AddRelationshipsStore } from '../../store/add-relationships.store';

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
        'dot.relationship.add.dialog.confirm': 'Add Relationships'
    });

    const createComponent = createComponentFactory({
        component: AddRelationshipsFooterComponent,
        providers: [
            { provide: DotMessageService, useValue: messageServiceMock },
            mockProvider(AddRelationshipsStore, {
                $selectedCount: () => 0
            })
        ]
    });

    beforeEach(() => {
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
        const confirmed = jest.fn();
        spectator.output('confirm').subscribe(confirmed);

        const button = spectator
            .query(byTestId('add-relationships-confirm'))
            ?.querySelector('button');
        spectator.click(button as HTMLElement);

        expect(confirmed).toHaveBeenCalled();
    });

    it('emits cancel when the cancel action is pressed', () => {
        const cancelled = jest.fn();
        spectator.output('cancel').subscribe(cancelled);

        const button = spectator
            .query(byTestId('add-relationships-cancel'))
            ?.querySelector('button');
        spectator.click(button as HTMLElement);

        expect(cancelled).toHaveBeenCalled();
    });
});
