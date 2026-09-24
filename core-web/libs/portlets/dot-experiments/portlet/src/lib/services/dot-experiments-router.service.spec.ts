import { createServiceFactory, SpectatorService } from '@openng/spectator/vitest';
import { Mock, vi } from 'vitest';

import { ActivatedRoute, Params, Router } from '@angular/router';

import { DotExperimentsPanelStore } from '@dotcms/portlets/dot-experiments/data-access';

import { DotExperimentsRouter } from './dot-experiments-router.service';

const EXPERIMENT_ID = 'exp-1';

/**
 * The fork that used to be copied into every screen (#37478).
 *
 * Both worlds are exercised from one place, which is the point of the service existing: a
 * destination cannot now be taught to the panel and left navigating somewhere else.
 */
describe('DotExperimentsRouter', () => {
    let spectator: SpectatorService<DotExperimentsRouter>;
    let navigate: Mock;
    let panelStore: Record<string, Mock> | null;
    let queryParams: Params;

    const createService = createServiceFactory({
        service: DotExperimentsRouter,
        providers: [
            { provide: Router, useFactory: () => ({ navigate }) },
            {
                provide: ActivatedRoute,
                useValue: {
                    snapshot: {
                        get queryParams() {
                            return queryParams;
                        }
                    }
                }
            },
            { provide: DotExperimentsPanelStore, useFactory: () => panelStore }
        ]
    });

    const inPanel = () => {
        panelStore = {
            showConfigure: vi.fn(),
            showResults: vi.fn(),
            showCreate: vi.fn(),
            backToList: vi.fn()
        };

        return createService();
    };

    beforeEach(() => {
        navigate = vi.fn();
        panelStore = null;
        queryParams = {};
    });

    describe('in the portlet', () => {
        beforeEach(() => {
            spectator = createService();
        });

        it.each([
            {
                what: 'a configuration',
                go: () => spectator.service.toConfiguration(EXPERIMENT_ID),
                commands: ['/experiments', EXPERIMENT_ID, 'configuration']
            },
            {
                what: 'results',
                go: () => spectator.service.toResults(EXPERIMENT_ID),
                commands: ['/experiments', EXPERIMENT_ID, 'results']
            },
            { what: 'the list', go: () => spectator.service.toList(), commands: ['/experiments'] },
            {
                what: 'creation',
                go: () => spectator.service.toCreate(),
                commands: ['/experiments', 'new']
            }
        ])('should route to $what', ({ go, commands }) => {
            go();

            expect(navigate).toHaveBeenCalledWith(commands, expect.anything());
        });

        /**
         * FR-021c. A screen reached from a narrowed list has to be able to get back to that same
         * narrowed list, so the narrowing travels in the address it is opened with.
         */
        it('should carry the narrowing it arrived on', () => {
            queryParams = { pageId: 'page-1', language_id: '2' };

            spectator.service.toResults(EXPERIMENT_ID);

            expect(navigate).toHaveBeenCalledWith(expect.anything(), {
                queryParams: { pageId: 'page-1', language_id: '2' }
            });
        });

        /** The list holds its filter in its own store, not on the route it is about to leave. */
        it('should prefer a narrowing the caller knows better', () => {
            queryParams = { pageId: 'from-the-address' };

            spectator.service.toResults(EXPERIMENT_ID, { pageId: 'from-the-caller' });

            expect(navigate).toHaveBeenCalledWith(expect.anything(), {
                queryParams: { pageId: 'from-the-caller' }
            });
        });

        /**
         * Not a destination but a correction: `replaceUrl` keeps `/experiments/new` out of the
         * history, so Back leaves the screen instead of returning to a creation form for something
         * that already exists.
         */
        it('should replace the creation URL once the experiment exists', () => {
            spectator.service.afterCreated(EXPERIMENT_ID);

            expect(navigate).toHaveBeenCalledWith(
                ['..', EXPERIMENT_ID, 'configuration'],
                expect.objectContaining({ replaceUrl: true, queryParamsHandling: 'preserve' })
            );
        });
    });

    describe('in the panel', () => {
        beforeEach(() => {
            spectator = inPanel();
        });

        it.each([
            {
                what: 'a configuration',
                go: () => spectator.service.toConfiguration(EXPERIMENT_ID),
                method: 'showConfigure',
                args: [EXPERIMENT_ID]
            },
            {
                what: 'results',
                go: () => spectator.service.toResults(EXPERIMENT_ID),
                method: 'showResults',
                args: [EXPERIMENT_ID]
            },
            {
                what: 'the list',
                go: () => spectator.service.toList(),
                method: 'backToList',
                args: []
            },
            {
                what: 'creation',
                go: () => spectator.service.toCreate(),
                method: 'showCreate',
                args: []
            },
            {
                what: 'a new experiment',
                go: () => spectator.service.afterCreated(EXPERIMENT_ID),
                method: 'showConfigure',
                args: [EXPERIMENT_ID]
            }
        ])('should show $what without navigating', ({ go, method, args }) => {
            go();

            expect(panelStore?.[method]).toHaveBeenCalledWith(...args);
            expect(navigate).not.toHaveBeenCalled();
        });

        /**
         * The whole contract in one assertion: nothing the panel does reaches the router. Written
         * as a sweep so a destination added later fails here rather than shipping as the one that
         * ejects the editor.
         */
        it('should never navigate, whichever destination is asked for', () => {
            spectator.service.toConfiguration(EXPERIMENT_ID);
            spectator.service.toResults(EXPERIMENT_ID);
            spectator.service.toList();
            spectator.service.toCreate();
            spectator.service.afterCreated(EXPERIMENT_ID);

            expect(navigate).not.toHaveBeenCalled();
        });
    });
});
