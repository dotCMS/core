import { byTestId, createComponentFactory, mockProvider, Spectator } from '@ngneat/spectator/jest';

import { signal } from '@angular/core';

import { DynamicDialogRef } from 'primeng/dynamicdialog';

import { DotMessageService } from '@dotcms/data-access';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotPublishingQueueDeleteDialogComponent } from './dot-publishing-queue-delete-dialog.component';

import { DotPublishingQueueStore } from '../../dot-publishing-queue-page/store/dot-publishing-queue.store';

describe('DotPublishingQueueDeleteDialogComponent', () => {
    let spectator: Spectator<DotPublishingQueueDeleteDialogComponent>;
    let dialogRef: jest.Mocked<DynamicDialogRef>;

    const historySelectedIds = signal<string[]>([]);

    const createComponent = createComponentFactory({
        component: DotPublishingQueueDeleteDialogComponent,
        componentProviders: [mockProvider(DotPublishingQueueStore, { historySelectedIds })],
        providers: [
            mockProvider(DynamicDialogRef, { close: jest.fn() }),
            {
                provide: DotMessageService,
                useValue: new MockDotMessageService({
                    'bundle.delete.selected': 'SELECTED',
                    'bundle.delete.all': 'ALL',
                    'bundle.delete.success': 'SUCCESS',
                    'bundle.delete.failed': 'FAILED',
                    'bundle.delete.process.info':
                        'Bundles will be deleted in the background. Please refresh to update the progress.'
                })
            }
        ]
    });

    beforeEach(() => {
        historySelectedIds.set([]);
        spectator = createComponent();
        dialogRef = spectator.inject(DynamicDialogRef) as jest.Mocked<DynamicDialogRef>;
    });

    function clickButton(testId: string): void {
        const btn = spectator.query(byTestId(testId))?.querySelector('button');
        spectator.click(btn as HTMLButtonElement);
    }

    it('renders all four scope buttons + the background-process hint', () => {
        expect(spectator.query(byTestId('pq-delete-selected'))).toBeTruthy();
        expect(spectator.query(byTestId('pq-delete-all'))).toBeTruthy();
        expect(spectator.query(byTestId('pq-delete-success'))).toBeTruthy();
        expect(spectator.query(byTestId('pq-delete-failed'))).toBeTruthy();
        expect(spectator.query(byTestId('pq-delete-hint'))?.textContent).toContain(
            'will be deleted in the background'
        );
    });

    it('disables SELECTED when there is no selection', () => {
        historySelectedIds.set([]);
        spectator.detectChanges();
        const btn = spectator.query(byTestId('pq-delete-selected'))?.querySelector('button');
        expect(btn?.hasAttribute('disabled')).toBe(true);
    });

    it('enables SELECTED when there is a selection', () => {
        historySelectedIds.set(['b1']);
        spectator.detectChanges();
        const btn = spectator.query(byTestId('pq-delete-selected'))?.querySelector('button');
        expect(btn?.hasAttribute('disabled')).toBe(false);
    });

    it.each([
        ['pq-delete-selected', 'selected'],
        ['pq-delete-all', 'all'],
        ['pq-delete-success', 'success'],
        ['pq-delete-failed', 'failed']
    ])('closes with scope "%s" when %s is clicked', (testId, expected) => {
        historySelectedIds.set(['b1']); // enable SELECTED for the parameterised test
        spectator.detectChanges();
        clickButton(testId);
        expect(dialogRef.close).toHaveBeenCalledWith(expected);
    });
});
