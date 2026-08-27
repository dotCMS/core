import { Dispatcher } from '@ngrx/signals/events';
import { byTestId, createComponentFactory, Spectator } from '@openng/spectator/jest';
import { Subject } from 'rxjs';

import { Injector, signal, WritableSignal } from '@angular/core';
import { disabled, form, max, min } from '@angular/forms/signals';

import { DialogService } from 'primeng/dynamicdialog';

import { DotMessageService } from '@dotcms/data-access';
import {
    DotCMSContentlet,
    DotExperiment,
    DotExperimentStatus,
    EXP_CONFIG_ERROR_LABEL_CANT_EDIT
} from '@dotcms/dotcms-models';
import { GlobalStore } from '@dotcms/store';
import { DotBrowserSelectorComponent } from '@dotcms/ui';
import { getExperimentMock, MockDotMessageService } from '@dotcms/utils-testing';

import { DotExperimentsConfigurePageComponent } from './dot-experiments-configure-page.component';

import {
    MAX_TRAFFIC_ALLOCATION,
    MIN_TRAFFIC_ALLOCATION,
    SELECT_PAGE_BROWSER_PARAMS
} from '../../../shared/constants';
import { ConfigureValidationRule, DotExperimentConfigurePage } from '../../../shared/models';
import { dotExperimentsConfigurePageEvents } from '../../../store/dot-experiments-configure-page.events';
import { DotExperimentsConfigureStore } from '../../../store/dot-experiments-configure.store';

const EMPTY_PAGE_COPY = 'No Page selected';
const PAGE_REQUIRED_COPY = 'Pick the page the experiment runs on';
const IMMUTABLE_TOOLTIP_COPY = 'The page can only be changed while the experiment is a draft';
const HAS_VARIANTS_TOOLTIP_COPY =
    'This experiment already has variants. Delete them to change the page.';
const LOCKED_TOOLTIP_COPY = 'This experiment can no longer be edited';
const SELECT_PAGE_HEADER_COPY = 'Select A Page';
const TRAFFIC_HELP_ALL_COPY = 'All traffic to {0} enters the Experiment';
const TRAFFIC_HELP_PARTIAL_COPY = '{0}% of the traffic to {1} enters the Experiment';

const messageServiceMock = new MockDotMessageService({
    'experiments.configure.page.empty': EMPTY_PAGE_COPY,
    'experiments.configure.page.error.required': PAGE_REQUIRED_COPY,
    'experiments.configure.page.select.immutable.tooltip': IMMUTABLE_TOOLTIP_COPY,
    'experiments.configure.page.select.has-variants.tooltip': HAS_VARIANTS_TOOLTIP_COPY,
    'experiments.configure.select-page.header': SELECT_PAGE_HEADER_COPY,
    'experiments.configure.page.traffic.help.all': TRAFFIC_HELP_ALL_COPY,
    'experiments.configure.page.traffic.help.partial': TRAFFIC_HELP_PARTIAL_COPY,
    [EXP_CONFIG_ERROR_LABEL_CANT_EDIT]: LOCKED_TOOLTIP_COPY
});

const SELECTED_PAGE: DotExperimentConfigurePage = {
    pageId: 'page-1',
    title: 'Pricing',
    path: '/pricing/index'
};

/** The site the browser must open on — anything else lands on System Host, which holds no pages. */
const SITE_ID = 'site-1';

/** What the shared browser closes with: the chosen page's contentlet. */
const PICKED_PAGE = {
    identifier: 'page-2',
    title: 'About us',
    url: '/about-us/index'
} as DotCMSContentlet;

const EXPERIMENT: DotExperiment = { ...getExperimentMock(1), trafficAllocation: 100 };

/** Only what the card still reads: the allocation now arrives through its field input. */
const createStoreMock = () => ({
    experiment: signal<DotExperiment | null>(null),
    selectedPage: signal<DotExperimentConfigurePage | null>(null),
    pagePrefillError: signal<string | null>(null),
    $validationErrors: signal<ConfigureValidationRule[]>([]),
    $isLocked: signal(false),
    $disabledTooltipKey: signal<string | null>(null),
    $canChangePage: signal(true),
    $status: signal<DotExperimentStatus>(DotExperimentStatus.DRAFT)
});

describe('DotExperimentsConfigurePageComponent', () => {
    let spectator: Spectator<DotExperimentsConfigurePageComponent>;
    let storeMock: ReturnType<typeof createStoreMock>;
    let dispatch: jest.SpyInstance;
    let dialogClosed: Subject<DotCMSContentlet | undefined>;
    let dialogService: { open: jest.Mock };
    let trafficAllocation: WritableSignal<number>;

    // The tooltip's overlay queries `matchMedia`, which jsdom does not implement.
    beforeAll(() => {
        Object.defineProperty(window, 'matchMedia', {
            writable: true,
            value: jest.fn().mockImplementation((query: string) => ({
                matches: false,
                media: query,
                onchange: null,
                addListener: jest.fn(),
                removeListener: jest.fn(),
                addEventListener: jest.fn(),
                removeEventListener: jest.fn(),
                dispatchEvent: jest.fn()
            }))
        });
    });

    const createComponent = createComponentFactory({
        component: DotExperimentsConfigurePageComponent,
        providers: [
            { provide: DotExperimentsConfigureStore, useFactory: () => storeMock },
            { provide: DotMessageService, useValue: messageServiceMock },
            { provide: GlobalStore, useValue: { currentSiteId: signal(SITE_ID) } }
        ],
        // The card provides `DialogService` itself, so it is replaced at component level.
        componentProviders: [{ provide: DialogService, useFactory: () => dialogService }],
        detectChanges: false
    });

    /**
     * Mounts the card on a real allocation leaf carrying the rules the shell declares over it —
     * a stub field would let the card claim a range error the form never raises.
     */
    const mountWith = (allocation = 100) => {
        trafficAllocation = signal(allocation);

        const formTree = form(
            trafficAllocation,
            (path) => {
                min(path, MIN_TRAFFIC_ALLOCATION);
                max(path, MAX_TRAFFIC_ALLOCATION);
                disabled(path, { when: () => storeMock.$isLocked() });
            },
            { injector: spectator.inject(Injector) }
        );

        spectator.setInput('field', formTree);
        spectator.detectChanges();
    };

    /** `injectDispatch` appends a scope argument, so only the event itself is compared. */
    const dispatchedEvents = () => dispatch.mock.calls.map(([event]) => event);

    const selectButton = () =>
        spectator
            .query(byTestId('experiments-configure-page-select-btn'))
            ?.querySelector('button') as HTMLButtonElement;

    const trafficInput = () =>
        spectator.query(byTestId('experiments-configure-traffic-input')) as HTMLInputElement;

    const typeTrafficAllocation = (value: string) => {
        spectator.typeInElement(value, trafficInput());
        spectator.detectChanges();
    };

    /** Copy the tooltip shows on hover, or `null` when there is nothing to explain. */
    const tooltipText = (): string | null => {
        spectator.dispatchMouseEvent(
            spectator.query(byTestId('experiments-configure-page-select-btn')) as HTMLElement,
            'mouseenter'
        );
        spectator.detectChanges();

        return document.querySelector('.p-tooltip-text')?.textContent?.trim() ?? null;
    };

    beforeEach(() => {
        storeMock = createStoreMock();
        dialogClosed = new Subject<DotCMSContentlet | undefined>();
        dialogService = { open: jest.fn().mockReturnValue({ onClose: dialogClosed }) };
        spectator = createComponent();
        dispatch = jest.spyOn(spectator.inject(Dispatcher), 'dispatch');
        mountWith();
    });

    afterEach(() => {
        jest.restoreAllMocks();
        document.querySelectorAll('.p-tooltip').forEach((tooltip) => tooltip.remove());
    });

    describe('selected page', () => {
        it('should say no page is selected while none is', () => {
            expect(
                spectator.query(byTestId('experiments-configure-page-empty'))?.textContent
            ).toContain(EMPTY_PAGE_COPY);
            expect(spectator.query(byTestId('experiments-configure-page-title'))).toBeNull();
        });

        it('should show the title and the path of the selected page', () => {
            storeMock.selectedPage.set(SELECTED_PAGE);
            spectator.detectChanges();

            expect(
                spectator.query(byTestId('experiments-configure-page-title'))?.textContent
            ).toContain(SELECTED_PAGE.title);
            expect(
                spectator.query(byTestId('experiments-configure-page-path'))?.textContent
            ).toContain(SELECTED_PAGE.path);
            expect(spectator.query(byTestId('experiments-configure-page-empty'))).toBeNull();
        });
    });

    describe('picking a page', () => {
        it('should open the shared site browser on the current site, asked for pages only', () => {
            spectator.click(selectButton());

            expect(dialogService.open).toHaveBeenCalledWith(
                DotBrowserSelectorComponent,
                expect.objectContaining({
                    header: SELECT_PAGE_HEADER_COPY,
                    modal: true,
                    data: { ...SELECT_PAGE_BROWSER_PARAMS, hostFolderId: SITE_ID }
                })
            );
        });

        it('should report the picked page as the selected page', () => {
            // The page is not a form value: it is immutable once the draft exists, so it is
            // reported to the store on its own.
            spectator.click(selectButton());

            dialogClosed.next(PICKED_PAGE);
            spectator.detectChanges();

            expect(dispatchedEvents()).toContainEqual(
                dotExperimentsConfigurePageEvents.pageSelected({
                    pageId: PICKED_PAGE.identifier,
                    title: PICKED_PAGE.title,
                    path: PICKED_PAGE.url
                })
            );
        });

        it('should leave the card untouched when the dialog is cancelled', () => {
            spectator.click(selectButton());

            dialogClosed.next(undefined);
            spectator.detectChanges();

            expect(dispatchedEvents()).toEqual([]);
        });
    });

    describe('immutable page', () => {
        it('should offer Clear instead of Select once a changeable page is chosen', () => {
            storeMock.experiment.set(EXPERIMENT);
            storeMock.selectedPage.set(SELECTED_PAGE);
            spectator.detectChanges();

            // The two swap: with a page in place the way to a different one is to clear this one.
            expect(
                spectator.query(byTestId('experiments-configure-page-clear-btn'))
            ).not.toBeNull();
            expect(spectator.query(byTestId('experiments-configure-page-select-btn'))).toBeNull();
        });

        it('should empty the selection when Clear is pressed', () => {
            storeMock.experiment.set(EXPERIMENT);
            storeMock.selectedPage.set(SELECTED_PAGE);
            spectator.detectChanges();

            spectator.click(
                spectator
                    .query(byTestId('experiments-configure-page-clear-btn'))
                    ?.querySelector('button') as HTMLElement
            );

            expect(dispatchedEvents()).toContainEqual(
                dotExperimentsConfigurePageEvents.pageCleared()
            );
        });

        it('should point at the variants when a draft has them, since that is undoable', () => {
            storeMock.experiment.set(EXPERIMENT);
            storeMock.selectedPage.set(SELECTED_PAGE);
            storeMock.$canChangePage.set(false);
            spectator.detectChanges();

            expect(selectButton().disabled).toBe(true);
            expect(tooltipText()).toBe(HAS_VARIANTS_TOOLTIP_COPY);
        });

        it('should explain the status instead once the experiment is past draft', () => {
            storeMock.experiment.set(EXPERIMENT);
            storeMock.selectedPage.set(SELECTED_PAGE);
            storeMock.$canChangePage.set(false);
            storeMock.$status.set(DotExperimentStatus.RUNNING);
            spectator.detectChanges();

            expect(selectButton().disabled).toBe(true);
            expect(tooltipText()).toBe(IMMUTABLE_TOOLTIP_COPY);
        });

        it('should leave the button enabled and unexplained before the experiment exists', () => {
            expect(selectButton().disabled).toBe(false);
            expect(tooltipText()).toBeNull();
        });

        it('should explain a locked experiment instead of the immutable page', () => {
            // AC34: being locked is the stronger reason, and the one the screen leads with.
            storeMock.experiment.set(EXPERIMENT);
            storeMock.$isLocked.set(true);
            storeMock.$disabledTooltipKey.set(EXP_CONFIG_ERROR_LABEL_CANT_EDIT);
            spectator.detectChanges();

            expect(selectButton().disabled).toBe(true);
            expect(tooltipText()).toBe(LOCKED_TOOLTIP_COPY);
        });
    });

    describe('errors', () => {
        it('should show no required-page error before Start is pressed', () => {
            // AC28: nothing is validated until Start/Schedule.
            expect(spectator.query(byTestId('experiments-configure-page-error'))).toBeNull();
        });

        it('should show the required-page error once the store reports it', () => {
            storeMock.$validationErrors.set(['page']);
            spectator.detectChanges();

            expect(
                spectator.query(byTestId('experiments-configure-page-error'))?.textContent
            ).toContain(PAGE_REQUIRED_COPY);
        });
    });

    describe('traffic allocation', () => {
        const help = () => spectator.query(byTestId('experiments-configure-traffic-help'));

        // It rides the Page row and only appears once there is a page to take traffic from, so
        // every case here starts from a selected one.
        beforeEach(() => {
            storeMock.selectedPage.set(SELECTED_PAGE);
            spectator.detectChanges();
        });

        it('should not be offered while no page is selected', () => {
            storeMock.selectedPage.set(null);
            spectator.detectChanges();

            expect(spectator.query(byTestId('experiments-configure-traffic-input'))).toBeNull();
        });

        it('should start from the value the form holds', () => {
            mountWith(EXPERIMENT.trafficAllocation);
            storeMock.selectedPage.set(SELECTED_PAGE);
            spectator.detectChanges();

            expect(trafficInput().value).toBe(String(EXPERIMENT.trafficAllocation));
        });

        it('should write the typed allocation into the model', () => {
            typeTrafficAllocation('40');

            expect(trafficAllocation()).toBe(40);
        });

        it('should show an allocation outside 1-100 as an error rather than accept it silently', () => {
            typeTrafficAllocation('120');

            expect(spectator.query(byTestId('experiments-configure-traffic-error'))).not.toBeNull();
        });

        it('should say the whole page enters the Experiment at 100%', () => {
            typeTrafficAllocation('100');

            expect(help()?.textContent).toContain(SELECTED_PAGE.path);
            expect(help()?.textContent).toContain('All traffic');
        });

        it('should name the share instead once part of the page is held back', () => {
            // "All traffic" would be a lie below 100 — the remainder still sees the Original.
            typeTrafficAllocation('40');

            expect(help()?.textContent).toContain('40%');
            expect(help()?.textContent).toContain(SELECTED_PAGE.path);
        });

        it('should give way to the range error rather than sit beside it', () => {
            typeTrafficAllocation('120');

            expect(help()).toBeNull();
        });

        it('should disable the allocation while the experiment is locked', () => {
            storeMock.$isLocked.set(true);
            spectator.detectChanges();

            expect(trafficInput().disabled).toBe(true);
        });
    });
});
