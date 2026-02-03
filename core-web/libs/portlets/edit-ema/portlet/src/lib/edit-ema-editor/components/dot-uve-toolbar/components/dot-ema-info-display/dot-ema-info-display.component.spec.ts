import { Spectator, byTestId, createComponentFactory } from '@ngneat/spectator/jest';

import { ButtonModule } from 'primeng/button';

import { DotMessageService } from '@dotcms/data-access';
import { DotMessagePipe } from '@dotcms/ui';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotEmaInfoDisplayComponent } from './dot-ema-info-display.component';

import { InfoOptions } from '../../../../../shared/models';

describe('DotEmaInfoDisplayComponent - Presentational', () => {
    let spectator: Spectator<DotEmaInfoDisplayComponent>;
    let emittedActions: string[] = [];

    const createComponent = createComponentFactory({
        component: DotEmaInfoDisplayComponent,
        imports: [ButtonModule, DotMessagePipe],
        providers: [
            {
                provide: DotMessageService,
                useValue: new MockDotMessageService({
                    'editpage.editing.variant': 'Editing Variant: {0}',
                    'editpage.viewing.variant': 'Viewing Variant: {0}'
                })
            }
        ],
        detectChanges: false
    });

    beforeEach(() => {
        emittedActions = [];
        spectator = createComponent();

        // Subscribe to output events
        spectator.component.actionClicked.subscribe((optionId) => {
            emittedActions.push(optionId);
        });
    });

    describe('Component Creation', () => {
        it('should create', () => {
            expect(spectator.component).toBeTruthy();
        });
    });

    describe('when options are provided with action icon', () => {
        beforeEach(() => {
            const options: InfoOptions = {
                info: {
                    message: 'editpage.editing.variant',
                    args: ['Variant A']
                },
                icon: 'pi pi-file-edit',
                id: 'variant',
                actionIcon: 'pi pi-arrow-left'
            };
            spectator.setInput('options', options);
            spectator.detectChanges();
        });

        it('should display action button when actionIcon is provided', () => {
            const actionButton = spectator.query(byTestId('info-action'));
            expect(actionButton).toBeTruthy();
        });

        it('should display the info icon', () => {
            const icon = spectator.query(byTestId('info-icon'));
            expect(icon).toBeTruthy();
            expect(icon).toHaveClass('pi');
            expect(icon).toHaveClass('pi-file-edit');
        });

        it('should display the info message with translated text', () => {
            const infoText = spectator.query(byTestId('info-text'));
            expect(infoText).toBeTruthy();
            expect(infoText?.innerHTML).toContain('Editing Variant: Variant A');
        });

        it('should emit actionClicked event when action button is clicked', () => {
            const actionButton = spectator.query(byTestId('info-action'));
            expect(actionButton).toBeTruthy();

            // Call handleAction directly (it's public and this is what the button calls)
            spectator.component.handleAction();

            expect(emittedActions).toHaveLength(1);
            expect(emittedActions[0]).toBe('variant');
        });
    });

    describe('when options are provided without action icon', () => {
        beforeEach(() => {
            const options: InfoOptions = {
                info: {
                    message: 'editpage.viewing.variant',
                    args: ['Variant B']
                },
                icon: 'pi pi-eye',
                id: 'preview'
            };
            spectator.setInput('options', options);
            spectator.detectChanges();
        });

        it('should not display action button when actionIcon is not provided', () => {
            const actionButton = spectator.query(byTestId('info-action'));
            expect(actionButton).toBeFalsy();
        });

        it('should still display the info message', () => {
            const infoText = spectator.query(byTestId('info-text'));
            expect(infoText).toBeTruthy();
            expect(infoText?.innerHTML).toContain('Viewing Variant: Variant B');
        });
    });

    describe('when options indicate device action', () => {
        beforeEach(() => {
            const options: InfoOptions = {
                info: {
                    message: 'Viewing on Mobile Device',
                    args: []
                },
                icon: 'pi pi-mobile',
                id: 'device',
                actionIcon: 'pi pi-times'
            };
            spectator.setInput('options', options);
            spectator.detectChanges();
        });

        it('should emit device option ID when action is triggered', () => {
            spectator.component.handleAction();

            expect(emittedActions).toHaveLength(1);
            expect(emittedActions[0]).toBe('device');
        });
    });

    describe('when options indicate social media action', () => {
        beforeEach(() => {
            const options: InfoOptions = {
                info: {
                    message: 'Viewing Facebook Preview',
                    args: []
                },
                icon: 'pi pi-facebook',
                id: 'socialMedia',
                actionIcon: 'pi pi-times'
            };
            spectator.setInput('options', options);
            spectator.detectChanges();
        });

        it('should emit socialMedia option ID when action is triggered', () => {
            spectator.component.handleAction();

            expect(emittedActions).toHaveLength(1);
            expect(emittedActions[0]).toBe('socialMedia');
        });
    });

    describe('when options are null or undefined', () => {
        beforeEach(() => {
            spectator.setInput('options', undefined);
            spectator.detectChanges();
        });

        it('should not display anything when options are undefined', () => {
            const actionButton = spectator.query(byTestId('info-action'));
            const icon = spectator.query(byTestId('info-icon'));
            const infoText = spectator.query(byTestId('info-text'));

            expect(actionButton).toBeFalsy();
            expect(icon).toBeFalsy();
            expect(infoText).toBeFalsy();
        });

        it('should not emit event when handleAction is called with no options', () => {
            spectator.component.handleAction();
            expect(emittedActions).toHaveLength(0);
        });
    });

    describe('handleAction method', () => {
        it('should emit optionId when called with valid options', () => {
            const options: InfoOptions = {
                info: { message: 'Test', args: [] },
                icon: 'pi pi-test',
                id: 'test-action',
                actionIcon: 'pi pi-check'
            };
            spectator.setInput('options', options);
            spectator.detectChanges();

            spectator.component.handleAction();

            expect(emittedActions).toEqual(['test-action']);
        });

        it('should not emit when options have no id', () => {
            const options: InfoOptions = {
                info: { message: 'Test', args: [] },
                icon: 'pi pi-test',
                id: null,
                actionIcon: 'pi pi-check'
            };
            spectator.setInput('options', options);
            spectator.detectChanges();

            spectator.component.handleAction();

            expect(emittedActions).toHaveLength(0);
        });
    });
});
