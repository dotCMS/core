import { byTestId, createComponentFactory, Spectator } from '@ngneat/spectator/jest';
import { BehaviorSubject } from 'rxjs';

import { BreakpointObserver, Breakpoints, BreakpointState } from '@angular/cdk/layout';

import { Button } from 'primeng/button';
import { Menu } from 'primeng/menu';
import { SplitButton } from 'primeng/splitbutton';

import { DotMessageService } from '@dotcms/data-access';
import {
    MockDotMessageService,
    mockWorkflowsActions,
    mockWorkflowsActionsWithMove
} from '@dotcms/utils-testing';

import { DotWorkflowActionsComponent } from './dot-workflow-actions.component';

import { DotMessagePipe } from '../../dot-message/dot-message.pipe';

// mockWorkflowsActions       → 3 actions (Assign Workflow, Save, Save/Publish)
// mockWorkflowsActionsWithMove → 4 actions (above + Move)

const SEPARATOR_ACTION = {
    assignable: false,
    commentable: false,
    condition: '',
    icon: '',
    id: 'separator-id',
    name: 'SEPARATOR',
    nextAssign: '',
    nextStep: '',
    nextStepCurrentStep: false,
    order: 0,
    roleHierarchyForAssign: false,
    schemeId: '',
    showOn: [],
    actionInputs: [],
    metadata: { subtype: 'SEPARATOR' }
};

const messageServiceMock = new MockDotMessageService({
    'edit.ema.page.no.workflow.action': 'no workflow action',
    Loading: 'loading'
});

/** Shared mock so responsive tests can drive {@link BreakpointObserver} without a second TestBed module. */
const breakpointState$ = new BehaviorSubject<BreakpointState>({ matches: true, breakpoints: {} });
const breakpointMatchMap: Record<string, boolean> = {};

const resetBreakpointMock = (): void => {
    Object.keys(breakpointMatchMap).forEach((k) => delete breakpointMatchMap[k]);
    breakpointState$.next({ matches: true, breakpoints: {} });
};

const setBreakpointMatch = (partial: Record<string, boolean>): void => {
    resetBreakpointMock();
    Object.assign(breakpointMatchMap, partial);
    breakpointState$.next({ matches: true, breakpoints: {} });
};

const breakpointObserverMock: Pick<BreakpointObserver, 'observe' | 'isMatched'> = {
    observe: jest.fn(() => breakpointState$.asObservable()),
    isMatched: jest.fn((query: string | readonly string[]) => {
        const key = typeof query === 'string' ? query : query[0];

        return !!breakpointMatchMap[key];
    })
};

describe('DotWorkflowActionsComponent', () => {
    let spectator: Spectator<DotWorkflowActionsComponent>;

    const createComponent = createComponentFactory({
        component: DotWorkflowActionsComponent,
        imports: [DotMessagePipe],
        providers: [
            { provide: DotMessageService, useValue: messageServiceMock },
            { provide: BreakpointObserver, useValue: breakpointObserverMock }
        ],
        detectChanges: false
    });

    beforeEach(() => {
        resetBreakpointMock();
        spectator = createComponent({ props: { actions: [] } });
    });

    describe('empty state', () => {
        beforeEach(() => {
            spectator.detectChanges();
        });

        it('should show loading spinner and loading label when loading is true', () => {
            spectator.setInput('loading', true);
            spectator.detectChanges();

            const button = spectator.query(Button);

            expect(button.loading).toBeTruthy();
            expect(button.disabled).toBeFalsy();
            expect(button.label).toBe('loading');
        });

        it('should show disabled button with no-workflow label when not loading', () => {
            const button = spectator.query(Button);

            expect(button.disabled).toBeTruthy();
            expect(button.loading).toBeFalsy();
            expect(button.label).toBe('no workflow action');
        });
    });

    describe('inline buttons', () => {
        it('should render 1 primary button for 1 action', () => {
            spectator.setInput('actions', [mockWorkflowsActions[0]]);
            spectator.detectChanges();

            const buttons = spectator.queryAll(Button);

            expect(buttons.length).toBe(1);
            expect(buttons[0].variant).toBeUndefined();
        });

        it('should render primary and outlined buttons for 2 actions', () => {
            spectator.setInput('actions', mockWorkflowsActions.slice(0, 2));
            spectator.detectChanges();

            const buttons = spectator.queryAll(Button);

            expect(buttons.length).toBe(2);
            expect(buttons[0].variant).toBeUndefined();
            expect(buttons[1].variant).toBe('outlined');
        });

        it('should render primary and outlined buttons for 3 actions', () => {
            spectator.setInput('actions', mockWorkflowsActions);
            spectator.detectChanges();

            const buttons = spectator.queryAll(Button);

            expect(buttons.length).toBe(3);
            expect(buttons[0].variant).toBeUndefined();
            expect(buttons[1].variant).toBe('outlined');
            expect(buttons[2].variant).toBe('outlined');
        });

        it('should render labels matching the action names', () => {
            spectator.setInput('actions', mockWorkflowsActions);
            spectator.detectChanges();

            const buttons = spectator.queryAll(Button);

            mockWorkflowsActions.forEach((action, idx) => {
                expect(buttons[idx].label).toBe(action.name);
            });
        });

        it('should filter out SEPARATOR actions', () => {
            spectator.setInput('actions', [
                mockWorkflowsActions[0],
                SEPARATOR_ACTION,
                mockWorkflowsActions[1]
            ]);
            spectator.detectChanges();

            const buttons = spectator.queryAll(Button);

            expect(buttons.length).toBe(2);
        });
    });

    describe('overflow', () => {
        it('should not show overflow button when actions are 3 or fewer', () => {
            spectator.setInput('actions', mockWorkflowsActions);
            spectator.detectChanges();

            expect(spectator.query(byTestId('overflow-button'))).toBeNull();
            expect(spectator.query(Menu)).toBeNull();
        });

        it('should show overflow button when actions exceed the inline cap', () => {
            setBreakpointMatch({ [Breakpoints.Large]: true });
            spectator.setInput('actions', mockWorkflowsActionsWithMove);
            spectator.detectChanges();

            expect(spectator.query(byTestId('overflow-button'))).toBeTruthy();
        });

        it('should put actions beyond the cap into the overflow menu model', () => {
            setBreakpointMatch({ [Breakpoints.Large]: true });
            spectator.setInput('actions', mockWorkflowsActionsWithMove);
            spectator.detectChanges();

            const menu = spectator.query(Menu);
            const overflowAction = mockWorkflowsActionsWithMove[3];

            expect(menu.model.length).toBe(1);
            expect(menu.model[0].label).toBe(overflowAction.name);
        });

        it('should emit actionFired when an overflow menu item command is invoked', () => {
            setBreakpointMatch({ [Breakpoints.Large]: true });
            spectator.setInput('actions', mockWorkflowsActionsWithMove);
            spectator.detectChanges();

            const spy = jest.spyOn(spectator.component.actionFired, 'emit');
            const menu = spectator.query(Menu);
            menu.model[0].command({});

            expect(spy).toHaveBeenCalledWith(mockWorkflowsActionsWithMove[3]);
        });
    });

    describe('actionFired', () => {
        beforeEach(() => {
            spectator.setInput('actions', mockWorkflowsActions);
            spectator.detectChanges();
        });

        it('should emit the action when an inline button is clicked', () => {
            const spy = jest.spyOn(spectator.component.actionFired, 'emit');
            const action = mockWorkflowsActions[0];
            const btn = spectator
                .query(byTestId(`action-button-${action.id}`))
                ?.querySelector('button');

            spectator.click(btn);

            expect(spy).toHaveBeenCalledWith(action);
        });
    });

    describe('loading', () => {
        beforeEach(() => {
            spectator.setInput('actions', mockWorkflowsActions);
            spectator.setInput('loading', true);
            spectator.detectChanges();
        });

        it('should show loading spinner only on the primary (first) button', () => {
            const buttons = spectator.queryAll(Button);

            expect(buttons[0].loading).toBeTruthy();
            expect(buttons[1].loading).toBeFalsy();
            expect(buttons[2].loading).toBeFalsy();
        });

        it('should disable all buttons while loading', () => {
            spectator.queryAll(Button).forEach((button) => {
                expect(button.disabled).toBeTruthy();
            });
        });
    });

    describe('disabled', () => {
        beforeEach(() => {
            spectator.setInput('actions', mockWorkflowsActions);
            spectator.detectChanges();
        });

        it('should disable all buttons when disabled is true', () => {
            spectator.setInput('disabled', true);
            spectator.detectChanges();

            spectator.queryAll(Button).forEach((button) => {
                expect(button.disabled).toBeTruthy();
            });
        });

        it('should have all buttons enabled by default', () => {
            spectator.queryAll(Button).forEach((button) => {
                expect(button.disabled).toBeFalsy();
            });
        });
    });

    describe('size', () => {
        beforeEach(() => {
            spectator.setInput('actions', mockWorkflowsActions);
        });

        it('should use PrimeNG default size when size is normal', () => {
            spectator.setInput('size', 'normal');
            spectator.detectChanges();

            spectator.queryAll(Button).forEach((button) => {
                expect(button.size).toBeUndefined();
            });
        });

        it('should set size to small on all buttons', () => {
            spectator.setInput('size', 'small');
            spectator.detectChanges();

            spectator.queryAll(Button).forEach((button) => {
                expect(button.size).toBe('small');
            });
        });

        it('should set size to large on all buttons', () => {
            spectator.setInput('size', 'large');
            spectator.detectChanges();

            spectator.queryAll(Button).forEach((button) => {
                expect(button.size).toBe('large');
            });
        });
    });

    /**
     * {@link DotWorkflowActionsComponent} derives inline vs overflow from CDK {@link BreakpointObserver}.
     * These tests drive the shared mock so each breakpoint branch runs without relying on viewport size.
     */
    describe('responsive inline cap', () => {
        it('should put all actions in overflow when XSmall matches (0 inline)', () => {
            setBreakpointMatch({ [Breakpoints.XSmall]: true });
            spectator.setInput('actions', mockWorkflowsActions);
            spectator.detectChanges();

            expect(spectator.queryAll(Button).length).toBe(1);
            expect(spectator.query(byTestId('overflow-button'))).toBeTruthy();
            expect(spectator.query(Menu).model.length).toBe(3);
        });

        it('should show one inline button when Small matches (cap 1)', () => {
            setBreakpointMatch({ [Breakpoints.Small]: true });
            spectator.setInput('actions', mockWorkflowsActions);
            spectator.detectChanges();

            expect(spectator.queryAll(Button).length).toBe(2);
            expect(spectator.query(byTestId('overflow-button'))).toBeTruthy();
            expect(spectator.query(Menu).model.length).toBe(2);
        });

        it('should show two inline buttons when Medium matches (cap 2)', () => {
            setBreakpointMatch({ [Breakpoints.Medium]: true });
            spectator.setInput('actions', mockWorkflowsActions);
            spectator.detectChanges();

            expect(spectator.queryAll(Button).length).toBe(3);
            expect(spectator.query(byTestId('overflow-button'))).toBeTruthy();
            expect(spectator.query(Menu).model.length).toBe(1);
        });

        it('should show three inline buttons when Large matches (cap 3)', () => {
            setBreakpointMatch({ [Breakpoints.Large]: true });
            spectator.setInput('actions', mockWorkflowsActions);
            spectator.detectChanges();

            expect(spectator.queryAll(Button).length).toBe(3);
            expect(spectator.query(byTestId('overflow-button'))).toBeNull();
        });

        it('should put one action in overflow when Large matches and there are four actions (cap 3)', () => {
            setBreakpointMatch({ [Breakpoints.Large]: true });
            spectator.setInput('actions', mockWorkflowsActionsWithMove);
            spectator.detectChanges();

            expect(spectator.queryAll(Button).length).toBe(4); // 3 inline + overflow button
            expect(spectator.query(byTestId('overflow-button'))).toBeTruthy();
            expect(spectator.query(Menu).model.length).toBe(1);
        });

        it('should show all four inline buttons when no CDK breakpoint matches (XLarge fallback, cap 4)', () => {
            setBreakpointMatch({});
            spectator.setInput('actions', mockWorkflowsActionsWithMove);
            spectator.detectChanges();

            expect(spectator.queryAll(Button).length).toBe(4);
            expect(spectator.query(byTestId('overflow-button'))).toBeNull();
        });
    });

    describe('stacked', () => {
        beforeEach(() => {
            spectator.setInput('stacked', true);
        });

        it('should render ALL actions as buttons with no overflow menu', () => {
            setBreakpointMatch({ [Breakpoints.XSmall]: true }); // would otherwise force overflow
            spectator.setInput('actions', mockWorkflowsActionsWithMove);
            spectator.detectChanges();

            expect(spectator.queryAll(Button).length).toBe(mockWorkflowsActionsWithMove.length);
            expect(spectator.query(byTestId('overflow-button'))).toBeNull();
            expect(spectator.query(Menu)).toBeNull();
        });

        it('should render the first action solid and the rest outlined', () => {
            spectator.setInput('actions', mockWorkflowsActions);
            spectator.detectChanges();

            const buttons = spectator.queryAll(Button);

            expect(buttons[0].variant).toBeUndefined();
            expect(buttons[1].variant).toBe('outlined');
            expect(buttons[2].variant).toBe('outlined');
        });

        it('should render every button full-width (fluid)', () => {
            spectator.setInput('actions', mockWorkflowsActions);
            spectator.detectChanges();

            spectator.queryAll(Button).forEach((button) => {
                expect(button.fluid()).toBe(true);
            });
        });

        it('should stack the host in a column (no flex-row-reverse)', () => {
            spectator.setInput('actions', mockWorkflowsActions);
            spectator.detectChanges();

            const host = spectator.element;

            expect(host.classList.contains('flex-col')).toBe(true);
            expect(host.classList.contains('flex-row-reverse')).toBe(false);
        });

        it('should filter out SEPARATOR actions', () => {
            spectator.setInput('actions', [
                mockWorkflowsActions[0],
                SEPARATOR_ACTION,
                mockWorkflowsActions[1]
            ]);
            spectator.detectChanges();

            expect(spectator.queryAll(Button).length).toBe(2);
        });

        it('should emit actionFired when a stacked button is clicked', () => {
            spectator.setInput('actions', mockWorkflowsActions);
            spectator.detectChanges();

            const spy = jest.spyOn(spectator.component.actionFired, 'emit');
            const action = mockWorkflowsActions[1];
            const btn = spectator
                .query(byTestId(`action-button-${action.id}`))
                ?.querySelector('button');

            spectator.click(btn);

            expect(spy).toHaveBeenCalledWith(action);
        });
    });

    describe('groupActions', () => {
        const actionsWithSeparator = [
            mockWorkflowsActions[0],
            SEPARATOR_ACTION,
            mockWorkflowsActions[1],
            mockWorkflowsActions[2]
        ];

        beforeEach(() => {
            spectator.setInput('groupActions', true);
        });

        it('should render a p-splitButton when the group has sub-actions', () => {
            spectator.setInput('actions', mockWorkflowsActions);
            spectator.detectChanges();

            const splitButtons = spectator.queryAll(SplitButton);

            expect(splitButtons.length).toBe(1);
            expect(splitButtons[0].label).toBe(mockWorkflowsActions[0].name);
            expect(splitButtons[0].model.length).toBe(2);
        });

        it('should put sub-actions in the splitButton model with correct labels', () => {
            spectator.setInput('actions', mockWorkflowsActions);
            spectator.detectChanges();

            const [splitButton] = spectator.queryAll(SplitButton);

            expect(splitButton.model[0].label).toBe(mockWorkflowsActions[1].name);
            expect(splitButton.model[1].label).toBe(mockWorkflowsActions[2].name);
        });

        it('should emit actionFired for the main action when the splitButton primary button is clicked', () => {
            spectator.setInput('actions', mockWorkflowsActions);
            spectator.detectChanges();

            const spy = jest.spyOn(spectator.component.actionFired, 'emit');
            const [splitButton] = spectator.queryAll(SplitButton);
            splitButton.onClick.emit({});

            expect(spy).toHaveBeenCalledWith(mockWorkflowsActions[0]);
        });

        it('should emit actionFired when a sub-action command is invoked', () => {
            spectator.setInput('actions', mockWorkflowsActions);
            spectator.detectChanges();

            const spy = jest.spyOn(spectator.component.actionFired, 'emit');
            const [splitButton] = spectator.queryAll(SplitButton);
            splitButton.model[0].command({});

            expect(spy).toHaveBeenCalledWith(mockWorkflowsActions[1]);
        });

        it('should render one p-splitButton per separator-delimited group', () => {
            spectator.setInput('actions', actionsWithSeparator);
            spectator.detectChanges();

            // Group 1: [action0] → p-button (no sub-actions)
            // Group 2: [action1, action2] → p-splitButton
            expect(spectator.queryAll(SplitButton).length).toBe(1);
            expect(spectator.queryAll(Button).length).toBe(1);
        });

        it('should render a plain p-button for a single-action group', () => {
            spectator.setInput('actions', [mockWorkflowsActions[0]]);
            spectator.detectChanges();

            expect(spectator.queryAll(SplitButton).length).toBe(0);
            expect(spectator.queryAll(Button).length).toBe(1);
        });

        it('should show empty-button and no split-button when actions list is empty', () => {
            spectator.setInput('actions', []);
            spectator.detectChanges();

            expect(spectator.queryAll(SplitButton).length).toBe(0);
            expect(spectator.query(byTestId('empty-button'))).toBeTruthy();
        });

        it('should not render overflow menu — flat path is inactive when groupActions is true', () => {
            spectator.setInput('actions', mockWorkflowsActions);
            spectator.detectChanges();

            expect(spectator.query(byTestId('overflow-button'))).toBeNull();
            expect(spectator.query(Menu)).toBeNull();
        });

        it('should use breakpoint-based inline cap when groupActions is false', () => {
            setBreakpointMatch({});
            spectator.setInput('groupActions', false);
            spectator.setInput('actions', mockWorkflowsActionsWithMove);
            spectator.detectChanges();

            expect(spectator.queryAll(Button).length).toBe(4);
            expect(spectator.query(byTestId('overflow-button'))).toBeNull();
        });
    });
});
