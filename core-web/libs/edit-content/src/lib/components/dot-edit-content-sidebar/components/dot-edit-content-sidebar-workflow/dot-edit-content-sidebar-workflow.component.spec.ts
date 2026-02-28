/* eslint-disable @typescript-eslint/no-explicit-any */

import { Spectator, byTestId, createComponentFactory, mockProvider } from '@ngneat/spectator/jest';

import { FormsModule } from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { SelectModule } from 'primeng/select';
import { SkeletonModule } from 'primeng/skeleton';

import { DotMessageService } from '@dotcms/data-access';
import { DotMessagePipe } from '@dotcms/utils-testing';

import { DotEditContentSidebarWorkflowComponent } from './dot-edit-content-sidebar-workflow.component';

import { WORKFLOW_MOCKS, WORKFLOW_SELECTION_MOCK } from '../../../../utils/mocks';

describe('DotEditContentSidebarWorkflowComponent', () => {
    let spectator: Spectator<DotEditContentSidebarWorkflowComponent>;

    const createComponent = createComponentFactory({
        component: DotEditContentSidebarWorkflowComponent,
        imports: [
            DotMessagePipe,
            SkeletonModule,
            ButtonModule,
            DialogModule,
            SelectModule,
            FormsModule
        ],
        providers: [
            mockProvider(DotMessageService, {
                get: (key: string) => key
            })
        ]
    });

    beforeEach(() => {
        spectator = createComponent({ detectChanges: false });
    });

    describe('Initial State and Loading', () => {
        it('should show loading skeletons when isLoading is true', () => {
            spectator.setInput({
                isLoading: true,
                workflow: WORKFLOW_MOCKS.EXISTING,
                workflowSelection: WORKFLOW_SELECTION_MOCK.WITH_OPTIONS
            } as any);
            spectator.detectChanges();

            expect(spectator.queryAll('p-skeleton').length).toBeGreaterThan(0);
        });
    });

    describe('Workflow Display', () => {
        beforeEach(() => {
            spectator.setInput({
                isLoading: false,
                workflow: WORKFLOW_MOCKS.EXISTING,
                workflowSelection: WORKFLOW_SELECTION_MOCK.WITH_OPTIONS
            } as any);
            spectator.detectChanges();
        });

        it('should display workflow scheme name', () => {
            const workflowName = spectator.query(byTestId('workflow-name'));
            expect(workflowName.textContent.trim()).toBe(WORKFLOW_MOCKS.EXISTING.scheme.name);
        });

        it('should display workflow step', () => {
            const workflowStep = spectator.query(byTestId('workflow-step'));
            expect(workflowStep.textContent.trim()).toBe(WORKFLOW_MOCKS.EXISTING.step.name);
        });

        it('should display assignee when task exists', () => {
            const workflowAssigned = spectator.query(byTestId('workflow-assigned'));
            expect(workflowAssigned.textContent.trim()).toBe(
                WORKFLOW_MOCKS.EXISTING.task?.assignedTo
            );
        });
    });

    describe('Workflow Selection', () => {
        it('should show select workflow link when no workflow is selected', () => {
            spectator.setInput({
                isLoading: false,
                workflow: WORKFLOW_MOCKS.NEW,
                workflowSelection: WORKFLOW_SELECTION_MOCK.NO_WORKFLOW
            } as any);
            spectator.detectChanges();

            const selectLink = spectator.query(byTestId('select-workflow-link'));
            expect(selectLink).toBeTruthy();
        });

        it('should emit selected workflow when workflow is selected', () => {
            const selectSpy = jest.spyOn(spectator.component.onSelectWorkflow, 'emit');

            spectator.setInput({
                workflow: WORKFLOW_MOCKS.NEW,
                workflowSelection: WORKFLOW_SELECTION_MOCK.WITH_OPTIONS
            } as any);
            spectator.detectChanges();

            spectator.component.$selectedWorkflow.set(
                WORKFLOW_SELECTION_MOCK.WITH_OPTIONS.schemeOptions[0]
            );
            spectator.component.selectWorkflow();

            expect(selectSpy).toHaveBeenCalledWith('1');
        });
    });

    describe('Workflow Display States', () => {
        it('should show loading skeleton when isLoading is true', () => {
            spectator.setInput({
                isLoading: true,
                workflow: WORKFLOW_MOCKS.NEW,
                workflowSelection: WORKFLOW_SELECTION_MOCK.WITH_OPTIONS
            } as any);
            spectator.detectChanges();

            expect(spectator.query('p-skeleton')).toBeTruthy();
            expect(spectator.query(byTestId('select-workflow-link'))).toBeFalsy();
            expect(spectator.query(byTestId('edit-workflow-button'))).toBeFalsy();
            expect(spectator.query(byTestId('reset-workflow-button'))).toBeFalsy();
        });

        describe('No Workflow Selected', () => {
            beforeEach(() => {
                spectator.setInput({
                    isLoading: false,
                    workflow: WORKFLOW_MOCKS.NEW,
                    workflowSelection: {
                        ...WORKFLOW_SELECTION_MOCK.WITH_OPTIONS,
                        isWorkflowSelected: true
                    }
                } as any);
            });

            it('should show select workflow link', () => {
                const selectLink = spectator.query(byTestId('select-workflow-link'));

                expect(selectLink).toBeTruthy();
                expect(selectLink).toHaveText('edit.content.sidebar.workflow.select.workflow');
            });

            it('should open dialog when select workflow link is clicked', () => {
                const selectLink = spectator.query(byTestId('select-workflow-link'));
                spectator.click(selectLink);
                expect(spectator.component.$showDialog()).toBeTruthy();
            });
        });

        describe('Workflow Selected', () => {
            it('should show workflow name when workflow is selected', () => {
                spectator.setInput({
                    isLoading: false,
                    workflow: WORKFLOW_MOCKS.NEW,
                    workflowSelection: WORKFLOW_SELECTION_MOCK.WITH_OPTIONS
                } as any);

                const workflowName = spectator.query(byTestId('workflow-name'));
                expect(workflowName).toHaveText(WORKFLOW_MOCKS.NEW.scheme.name);
            });

            it('should show edit button when workflow selection is available', () => {
                spectator.setInput({
                    isLoading: false,
                    workflow: WORKFLOW_MOCKS.NEW,
                    workflowSelection: WORKFLOW_SELECTION_MOCK.WITH_OPTIONS
                } as any);

                const editButton = spectator.query(byTestId('edit-workflow-button'));
                expect(editButton).toBeTruthy();
            });

            it('should not show edit button when workflow selection is not available', () => {
                spectator.setInput({
                    isLoading: false,
                    workflow: WORKFLOW_MOCKS.NEW,
                    workflowSelection: {
                        ...WORKFLOW_SELECTION_MOCK.WITH_OPTIONS,
                        schemeOptions: []
                    }
                } as any);
                spectator.detectChanges();

                const editButton = spectator.query(byTestId('edit-workflow-button'));
                expect(editButton).toBeFalsy();
            });

            it('should show reset button when resetAction is available', () => {
                spectator.setInput({
                    isLoading: false,
                    workflow: WORKFLOW_MOCKS.EXISTING,
                    workflowSelection: WORKFLOW_SELECTION_MOCK.WITH_OPTIONS
                } as any);

                const resetButton = spectator.query(byTestId('reset-workflow-button'));
                expect(resetButton).toBeTruthy();
            });

            it('should not show reset button when resetAction is not available', () => {
                spectator.setInput({
                    isLoading: false,
                    workflow: { ...WORKFLOW_MOCKS.NEW, resetAction: null },
                    workflowSelection: WORKFLOW_SELECTION_MOCK.WITH_OPTIONS
                } as any);

                const resetButton = spectator.query(byTestId('reset-workflow-button'));
                expect(resetButton).toBeFalsy();
            });
        });

        describe('Button Actions', () => {
            it('should open dialog when edit button is clicked', () => {
                spectator.setInput({
                    isLoading: false,
                    workflow: WORKFLOW_MOCKS.NEW,
                    workflowSelection: WORKFLOW_SELECTION_MOCK.WITH_OPTIONS
                } as any);

                const editButton = spectator.query(byTestId('edit-workflow-button'));
                spectator.click(editButton);
                expect(spectator.component.$showDialog()).toBeTruthy();
            });

            it('should emit reset action with correct ID when reset button is clicked', () => {
                const resetSpy = jest.spyOn(spectator.component.onResetWorkflow, 'emit');
                spectator.setInput({
                    isLoading: false,
                    workflow: {
                        ...WORKFLOW_MOCKS.EXISTING,
                        resetAction: { id: '123' }
                    },
                    workflowSelection: WORKFLOW_SELECTION_MOCK.WITH_OPTIONS
                } as any);

                const resetButton = spectator.query(byTestId('reset-workflow-button'));
                spectator.click(resetButton);
                expect(resetSpy).toHaveBeenCalledWith('123');
            });
        });
    });
});
