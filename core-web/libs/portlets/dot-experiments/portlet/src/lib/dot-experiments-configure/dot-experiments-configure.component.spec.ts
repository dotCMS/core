import { Dispatcher, EventCreator } from '@ngrx/signals/events';
import { byTestId, createComponentFactory, mockProvider, Spectator } from '@openng/spectator/jest';

import { provideLocationMocks } from '@angular/common/testing';
import { ApplicationRef, Component, signal } from '@angular/core';
import { provideRouter, Router } from '@angular/router';

import { ConfirmationService } from 'primeng/api';

import { DotMessageDisplayService, DotMessageService } from '@dotcms/data-access';
import {
    ComponentStatus,
    DotExperiment,
    DotExperimentStatus,
    DotMessageSeverity
} from '@dotcms/dotcms-models';
import { getExperimentMock, MockDotMessageService } from '@dotcms/utils-testing';

import { DotExperimentsConfigureDetailsComponent } from './components/dot-experiments-configure-details/dot-experiments-configure-details.component';
import { DotExperimentsConfigureFooterComponent } from './components/dot-experiments-configure-footer/dot-experiments-configure-footer.component';
import { DotExperimentsConfigureGoalComponent } from './components/dot-experiments-configure-goal/dot-experiments-configure-goal.component';
import { DotExperimentsConfigureHeaderComponent } from './components/dot-experiments-configure-header/dot-experiments-configure-header.component';
import { DotExperimentsConfigurePageComponent } from './components/dot-experiments-configure-page/dot-experiments-configure-page.component';
import { DotExperimentsConfigureSchedulingComponent } from './components/dot-experiments-configure-scheduling/dot-experiments-configure-scheduling.component';
import { DotExperimentsConfigureVariantsComponent } from './components/dot-experiments-configure-variants/dot-experiments-configure-variants.component';
import { DotExperimentsConfigureComponent } from './dot-experiments-configure.component';

import { LOCKED_BANNER_KEY_READ_ONLY, LOCKED_BANNER_KEY_RUNNING } from '../shared/constants';
import { ConfigureValidationRule } from '../shared/models';
import { dotExperimentsConfigureApiEvents } from '../store/dot-experiments-configure-api.events';
import { DotExperimentsConfigureStore } from '../store/dot-experiments-configure.store';

const ERROR_COPY = {
    title: 'Could not load the experiment',
    subtitle: 'Failed to retrieve experiments data'
};

const LOCKED_COPY = {
    running: 'This experiment is running and cannot be edited',
    readOnly: 'This experiment can no longer be edited'
};

const messageServiceMock = new MockDotMessageService({
    'experiments.list.error.title': ERROR_COPY.title,
    'experiments.error.fetching.data': ERROR_COPY.subtitle,
    'experiments.configure.action.back-to-list': 'Back to experiments',
    [LOCKED_BANNER_KEY_RUNNING]: LOCKED_COPY.running,
    [LOCKED_BANNER_KEY_READ_ONLY]: LOCKED_COPY.readOnly,
    'experiments.configure.notification.created': 'Experiment {0} created',
    'experiments.action.scheduled.confirm-message': 'Experiment {0} scheduled',
    'experiments.action.start.confirm-message': 'Experiment {0} started',
    'experiments.action.stop.confirm-message': 'Experiment {0} ended',
    'experiments.notification.cancel.schedule': 'Experiment {0} unscheduled',
    'experiments.notification.abort': 'Experiment {0} aborted'
});

/**
 * The cards are shallow-rendered: this screen owns the layout, the banner, the toasts and the
 * scroll to the first failing field — everything else belongs to the card that renders it.
 *
 * Two of them carry a `[data-error]` marker, standing in for a card revealing a failed rule, so
 * the scroll has something to find and the *first* one can be told apart from the second.
 */
@Component({ selector: 'dot-experiments-configure-header', template: '' })
class HeaderStubComponent {}

@Component({
    selector: 'dot-experiments-configure-details',
    template: '<span data-error data-testid="details-error-marker"></span>'
})
class DetailsStubComponent {}

@Component({
    selector: 'dot-experiments-configure-goal',
    template: '<span data-error data-testid="goal-error-marker"></span>'
})
class GoalStubComponent {}

@Component({ selector: 'dot-experiments-configure-page', template: '' })
class PageStubComponent {}

@Component({ selector: 'dot-experiments-configure-variants', template: '' })
class VariantsStubComponent {}

@Component({ selector: 'dot-experiments-configure-scheduling', template: '' })
class SchedulingStubComponent {}

@Component({ selector: 'dot-experiments-configure-footer', template: '' })
class FooterStubComponent {}

/**
 * The store is provided by the component itself, so it is replaced through `componentProviders`.
 * Real signals rather than `jest.fn()`s: the shell derives `$isLoading` in a `computed` and
 * watches `validationErrors` in an `effect`, neither of which would ever re-run over a plain
 * function.
 */
const createStoreMock = () => ({
    status: signal<ComponentStatus>(ComponentStatus.LOADED),
    validationErrors: signal<ConfigureValidationRule[]>([]),
    $lockedBannerKey: signal<string | null>(null)
});

describe('DotExperimentsConfigureComponent', () => {
    let spectator: Spectator<DotExperimentsConfigureComponent>;
    let storeMock: ReturnType<typeof createStoreMock>;
    let scrollIntoView: jest.Mock;

    const createComponent = createComponentFactory({
        component: DotExperimentsConfigureComponent,
        // `componentProviders` replaces the component's own `providers`, so the real
        // `ConfirmationService` has to be re-declared here (`p-confirmDialog` needs it).
        componentProviders: [
            { provide: DotExperimentsConfigureStore, useFactory: () => storeMock },
            ConfirmationService
        ],
        providers: [
            provideRouter([{ path: 'experiments', children: [] }]),
            provideLocationMocks(),
            { provide: DotMessageService, useValue: messageServiceMock },
            mockProvider(DotMessageDisplayService)
        ],
        overrideComponents: [
            [
                DotExperimentsConfigureComponent,
                {
                    remove: {
                        imports: [
                            DotExperimentsConfigureHeaderComponent,
                            DotExperimentsConfigureDetailsComponent,
                            DotExperimentsConfigureGoalComponent,
                            DotExperimentsConfigurePageComponent,
                            DotExperimentsConfigureVariantsComponent,
                            DotExperimentsConfigureSchedulingComponent,
                            DotExperimentsConfigureFooterComponent
                        ]
                    },
                    add: {
                        imports: [
                            HeaderStubComponent,
                            DetailsStubComponent,
                            GoalStubComponent,
                            PageStubComponent,
                            VariantsStubComponent,
                            SchedulingStubComponent,
                            FooterStubComponent
                        ]
                    }
                }
            ]
        ],
        detectChanges: false
    });

    /** Dispatches an outcome the store would have raised once a call settled. */
    const emitSucceeded = (
        event: EventCreator<string, DotExperiment>,
        experiment: DotExperiment
    ) => {
        spectator.inject(Dispatcher).dispatch(event(experiment));
        spectator.detectChanges();
    };

    /** Renders, then flushes the render hooks the scroll-to-error effect schedules. */
    const flush = () => {
        spectator.detectChanges();
        spectator.inject(ApplicationRef).tick();
    };

    beforeEach(() => {
        storeMock = createStoreMock();
        // jsdom does not implement scrollIntoView, so there is nothing to spy on.
        scrollIntoView = jest.fn();
        Element.prototype.scrollIntoView = scrollIntoView;
        spectator = createComponent();
    });

    afterEach(() => {
        jest.clearAllMocks();
    });

    describe('loaded screen', () => {
        it.each([
            'dot-experiments-configure-header',
            'dot-experiments-configure-details',
            'dot-experiments-configure-goal',
            'dot-experiments-configure-page',
            'dot-experiments-configure-variants',
            'dot-experiments-configure-scheduling',
            'dot-experiments-configure-footer'
        ])('should render %s', (selector) => {
            spectator.detectChanges();

            expect(spectator.query(selector)).not.toBeNull();
        });

        it('should render the cards inside the scrolling body', () => {
            spectator.detectChanges();

            const body = spectator.query(byTestId('experiments-configure-body'));

            expect(body?.querySelector('dot-experiments-configure-details')).not.toBeNull();
            expect(body?.querySelector('dot-experiments-configure-scheduling')).not.toBeNull();
        });

        it('should keep the header and the footer out of the scrolling body', () => {
            // They are pinned: the body is the only region that scrolls.
            spectator.detectChanges();

            const body = spectator.query(byTestId('experiments-configure-body'));

            expect(body?.querySelector('dot-experiments-configure-header')).toBeNull();
            expect(body?.querySelector('dot-experiments-configure-footer')).toBeNull();
        });

        it('should not render the loading or the error state', () => {
            spectator.detectChanges();

            expect(spectator.query(byTestId('experiments-configure-loading'))).toBeNull();
            expect(spectator.query(byTestId('experiments-configure-error'))).toBeNull();
        });
    });

    describe('loading', () => {
        it.each([ComponentStatus.INIT, ComponentStatus.LOADING])(
            'should render the skeleton on %s',
            (status) => {
                // INIT counts: the route is read in the store's `onInit`, so an existing
                // experiment spends a tick there before its load starts.
                storeMock.status.set(status);
                spectator.detectChanges();

                expect(spectator.query(byTestId('experiments-configure-loading'))).not.toBeNull();
                expect(spectator.query(byTestId('experiments-configure-body'))).toBeNull();
            }
        );

        it('should not render any card while loading', () => {
            storeMock.status.set(ComponentStatus.LOADING);
            spectator.detectChanges();

            expect(spectator.query('dot-experiments-configure-header')).toBeNull();
            expect(spectator.query('dot-experiments-configure-footer')).toBeNull();
        });
    });

    describe('load error', () => {
        const renderError = () => {
            storeMock.status.set(ComponentStatus.ERROR);
            spectator.detectChanges();
        };

        it('should replace the screen with the error state', () => {
            renderError();

            const error = spectator.query(byTestId('experiments-configure-error'));

            expect(error?.textContent).toContain(ERROR_COPY.title);
            expect(error?.textContent).toContain(ERROR_COPY.subtitle);
            expect(spectator.query(byTestId('experiments-configure-body'))).toBeNull();
            expect(spectator.query(byTestId('experiments-configure-loading'))).toBeNull();
        });

        it('should offer a way back to the list', () => {
            renderError();
            const router = spectator.inject(Router);
            const navigate = jest.spyOn(router, 'navigate').mockResolvedValue(true);

            spectator.click(
                spectator
                    .query(byTestId('experiments-configure-error'))
                    ?.querySelector('[data-testid="message-button"]') as HTMLElement
            );

            expect(navigate).toHaveBeenCalledWith(['/experiments']);
        });
    });

    describe('read-only banner', () => {
        it('should not render a banner while the experiment is editable', () => {
            spectator.detectChanges();

            expect(spectator.query(byTestId('experiments-configure-locked-banner'))).toBeNull();
        });

        it('should say the experiment is running when it is', () => {
            storeMock.$lockedBannerKey.set(LOCKED_BANNER_KEY_RUNNING);
            spectator.detectChanges();

            expect(
                spectator.query(byTestId('experiments-configure-locked-banner'))?.textContent
            ).toContain(LOCKED_COPY.running);
        });

        it('should fall back to the generic copy for every other locked status', () => {
            storeMock.$lockedBannerKey.set(LOCKED_BANNER_KEY_READ_ONLY);
            spectator.detectChanges();

            expect(
                spectator.query(byTestId('experiments-configure-locked-banner'))?.textContent
            ).toContain(LOCKED_COPY.readOnly);
        });

        it('should keep the cards on screen: the fields are frozen, not hidden', () => {
            storeMock.$lockedBannerKey.set(LOCKED_BANNER_KEY_RUNNING);
            spectator.detectChanges();

            expect(spectator.query(byTestId('experiments-configure-body'))).not.toBeNull();
            expect(spectator.query('dot-experiments-configure-footer')).not.toBeNull();
        });
    });

    describe('success toasts', () => {
        const experiment = getExperimentMock(0);

        const pushedMessage = (): string =>
            (spectator.inject(DotMessageDisplayService, true).push as jest.Mock).mock.calls.at(
                -1
            )?.[0].message;

        beforeEach(() => spectator.detectChanges());

        it.each([
            ['created', dotExperimentsConfigureApiEvents.createSucceeded],
            ['ended', dotExperimentsConfigureApiEvents.stopSucceeded],
            ['unscheduled', dotExperimentsConfigureApiEvents.cancelScheduleSucceeded],
            ['aborted', dotExperimentsConfigureApiEvents.abortSucceeded]
        ])('should push a success toast once the experiment is %s', (expectedVerb, event) => {
            emitSucceeded(event as EventCreator<string, DotExperiment>, experiment);

            expect(spectator.inject(DotMessageDisplayService, true).push).toHaveBeenCalledWith(
                expect.objectContaining({
                    severity: DotMessageSeverity.SUCCESS,
                    message: `Experiment ${experiment.name} ${expectedVerb}`
                })
            );
        });

        it('should say the experiment started when the server reports it running', () => {
            emitSucceeded(dotExperimentsConfigureApiEvents.startSucceeded, {
                ...experiment,
                status: DotExperimentStatus.RUNNING
            });

            expect(pushedMessage()).toBe(`Experiment ${experiment.name} started`);
        });

        it('should say the experiment was scheduled when the server reports it scheduled', () => {
            // A start dated in the future schedules the experiment instead of running it, and
            // the server's answer is what says which of the two happened.
            emitSucceeded(dotExperimentsConfigureApiEvents.startSucceeded, {
                ...experiment,
                status: DotExperimentStatus.SCHEDULED
            });

            expect(pushedMessage()).toBe(`Experiment ${experiment.name} scheduled`);
        });

        it('should stay silent on an autosave, which is deliberately unannounced', () => {
            emitSucceeded(dotExperimentsConfigureApiEvents.nameSucceeded, experiment);
            emitSucceeded(dotExperimentsConfigureApiEvents.goalSucceeded, experiment);

            expect(spectator.inject(DotMessageDisplayService, true).push).not.toHaveBeenCalled();
        });

        it('should stay silent on a failed transition, which the error manager already reported', () => {
            spectator
                .inject(Dispatcher)
                .dispatch(dotExperimentsConfigureApiEvents.startFailed(new Error('boom')));
            spectator.detectChanges();

            expect(spectator.inject(DotMessageDisplayService, true).push).not.toHaveBeenCalled();
        });
    });

    describe('scroll to the first failing field', () => {
        it('should not scroll while nothing has failed validation', () => {
            flush();

            expect(scrollIntoView).not.toHaveBeenCalled();
        });

        it('should bring the first failing field into view once Start reveals the errors', () => {
            flush();

            storeMock.validationErrors.set(['name', 'goalType']);
            flush();

            expect(scrollIntoView).toHaveBeenCalledWith({ behavior: 'smooth', block: 'center' });
        });

        it('should scroll to the first marker on the screen, not to any later one', () => {
            flush();

            storeMock.validationErrors.set(['goalType']);
            flush();

            expect(scrollIntoView.mock.instances[0]).toBe(
                spectator.query(byTestId('details-error-marker'))
            );
        });

        it('should scroll again when the footer re-runs it on a second press', () => {
            // The errors do not change on a re-press, so the effect alone would never fire twice.
            flush();
            storeMock.validationErrors.set(['name']);
            flush();
            scrollIntoView.mockClear();

            spectator.component.scrollToFirstValidationError();

            expect(scrollIntoView).toHaveBeenCalledTimes(1);
        });
    });
});
