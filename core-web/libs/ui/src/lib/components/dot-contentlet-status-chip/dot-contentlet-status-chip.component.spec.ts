import { createComponentFactory, Spectator } from '@ngneat/spectator/jest';

import { DotContentState } from '@dotcms/dotcms-models';

import { DotContentletStatusChipComponent } from './dot-contentlet-status-chip.component';

describe('DotContentletStatusChipComponent', () => {
    let spectator: Spectator<DotContentletStatusChipComponent>;

    const createComponent = createComponentFactory({
        component: DotContentletStatusChipComponent
    });

    beforeEach(() => {
        // Create component with a default state since state is required
        spectator = createComponent({
            props: {
                state: {
                    live: false,
                    working: false,
                    hasLiveVersion: false,
                    archived: false,
                    deleted: false
                }
            }
        });
    });

    describe('Status chip rendering', () => {
        it('should render "Published" chip with green styling when contentlet is published', () => {
            const state: DotContentState = {
                live: true,
                working: true,
                hasLiveVersion: true,
                archived: false,
                deleted: false
            };

            spectator.setInput('state', state);
            spectator.detectChanges();

            const chip = spectator.query('p-chip');
            expect(chip).toBeTruthy();
            expect(chip).toHaveClass('bg-green-100!');
            expect(chip).toHaveClass('text-green-700!');
            expect(chip?.textContent?.trim()).toBe('Published');
        });

        it('should render "Archived" chip with red styling when contentlet is archived', () => {
            const state: DotContentState = {
                archived: true,
                live: false,
                working: false,
                hasLiveVersion: false
            };

            spectator.setInput('state', state);
            spectator.detectChanges();

            const chip = spectator.query('p-chip');
            expect(chip).toBeTruthy();
            expect(chip).toHaveClass('bg-red-100!');
            expect(chip).toHaveClass('text-red-700!');
            expect(chip?.textContent?.trim()).toBe('Archived');
        });

        it('should render "Archived" chip with red styling when contentlet is deleted', () => {
            const state: DotContentState = {
                deleted: true,
                live: false,
                working: false,
                hasLiveVersion: false
            };

            spectator.setInput('state', state);
            spectator.detectChanges();

            const chip = spectator.query('p-chip');
            expect(chip).toBeTruthy();
            expect(chip).toHaveClass('bg-red-100!');
            expect(chip).toHaveClass('text-red-700!');
            expect(chip?.textContent?.trim()).toBe('Archived');
        });

        it('should render "Revision" chip with yellow styling when contentlet has live version but is not live', () => {
            const state: DotContentState = {
                live: false,
                working: false,
                hasLiveVersion: true,
                archived: false,
                deleted: false
            };

            spectator.setInput('state', state);
            spectator.detectChanges();

            const chip = spectator.query('p-chip');
            expect(chip).toBeTruthy();
            expect(chip).toHaveClass('bg-yellow-100!');
            expect(chip).toHaveClass('text-yellow-700!');
            expect(chip?.textContent?.trim()).toBe('Revision');
        });

        it('should render "Draft" chip with yellow styling when contentlet is draft', () => {
            const state: DotContentState = {
                live: false,
                working: false,
                hasLiveVersion: false,
                archived: false,
                deleted: false
            };

            spectator.setInput('state', state);
            spectator.detectChanges();

            const chip = spectator.query('p-chip');
            expect(chip).toBeTruthy();
            expect(chip).toHaveClass('bg-yellow-100!');
            expect(chip).toHaveClass('text-yellow-700!');
            expect(chip?.textContent?.trim()).toBe('Draft');
        });

        it('should render chip for all status types', () => {
            const states: DotContentState[] = [
                {
                    live: true,
                    working: true,
                    hasLiveVersion: true,
                    archived: false,
                    deleted: false
                },
                {
                    archived: true,
                    live: false,
                    working: false,
                    hasLiveVersion: false
                },
                {
                    live: false,
                    working: false,
                    hasLiveVersion: true,
                    archived: false,
                    deleted: false
                },
                {
                    live: false,
                    working: false,
                    hasLiveVersion: false,
                    archived: false,
                    deleted: false
                }
            ];

            states.forEach((state) => {
                spectator.setInput('state', state);
                spectator.detectChanges();

                const chip = spectator.query('p-chip');
                expect(chip).toBeTruthy();
            });
        });
    });
});
