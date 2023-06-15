import {
    byTestId,
    createComponentFactory,
    mockProvider,
    Spectator,
    SpyObject
} from '@ngneat/spectator/jest';
import { of } from 'rxjs';

import { ActivatedRoute, Router } from '@angular/router';

import { ConfirmationService, MessageService } from 'primeng/api';
import { Card } from 'primeng/card';
import { ConfirmPopup } from 'primeng/confirmpopup';
import { Inplace } from 'primeng/inplace';
import { Tooltip } from 'primeng/tooltip';

import { DotCopyButtonComponent } from '@components/dot-copy-button/dot-copy-button.component';
import { DotMessageService, DotSessionStorageService } from '@dotcms/data-access';
import {
    DEFAULT_VARIANT_ID,
    DEFAULT_VARIANT_NAME,
    DotExperimentStatusList,
    DotPageMode,
    ExperimentSteps
} from '@dotcms/dotcms-models';
import { DotExperimentsService } from '@dotcms/portlets/dot-experiments/data-access';
import { DotMessagePipe } from '@dotcms/ui';
import {
    ACTIVE_ROUTE_MOCK_CONFIG,
    getExperimentMock,
    MockDotMessageService
} from '@dotcms/utils-testing';
import { DotHttpErrorManagerService } from '@services/dot-http-error-manager/dot-http-error-manager.service';

import { DotExperimentsConfigurationVariantsComponent } from './dot-experiments-configuration-variants.component';

import { DotExperimentsConfigurationStore } from '../../store/dot-experiments-configuration-store';

const messageServiceMock = new MockDotMessageService({
    'experiments.configure.variants.weight': 'weight',
    'experiments.configure.variants.view': 'view',
    'experiments.action.edit': 'edit',
    'experiments.configure.variants.delete': 'delete',
    'experiments.configure.variants.add': 'Add new variant',
    'experiments.configure.scheduling.start': 'When the experiment start',
    'experiments.configure.variant.delete.confirm': 'Are you sure you want to delete this variant?',
    delete: 'Delete',
    'dot.common.dialog.reject': 'Cancel'
});

const ActivatedRouteMock = {
    snapshot: {
        params: {
            experimentId: 'test'
        },
        data: ACTIVE_ROUTE_MOCK_CONFIG.snapshot.data
    }
};

const EXPERIMENT_MOCK = getExperimentMock(0);
const EXPERIMENT_MOCK_2 = getExperimentMock(2);

describe('DotExperimentsConfigurationVariantsComponent', () => {
    let spectator: Spectator<DotExperimentsConfigurationVariantsComponent>;
    let store: DotExperimentsConfigurationStore;
    let dotExperimentsService: SpyObject<DotExperimentsService>;
    let router: Router;

    let dotSessionStorageService: DotSessionStorageService;

    const createComponent = createComponentFactory({
        component: DotExperimentsConfigurationVariantsComponent,
        providers: [
            DotExperimentsConfigurationStore,
            ConfirmationService,
            DotMessagePipe,
            {
                provide: DotMessageService,
                useValue: messageServiceMock
            },
            {
                provide: ActivatedRoute,
                useValue: ActivatedRouteMock
            },
            mockProvider(DotExperimentsService),
            mockProvider(MessageService),
            mockProvider(DotHttpErrorManagerService),
            mockProvider(DotSessionStorageService),
            mockProvider(Router)
        ]
    });

    beforeEach(() => {
        spectator = createComponent({
            detectChanges: false
        });

        jest.spyOn(ConfirmPopup.prototype, 'bindScrollListener').mockImplementation(jest.fn());

        store = spectator.inject(DotExperimentsConfigurationStore);
        router = spectator.inject(Router);

        dotSessionStorageService = spectator.inject(DotSessionStorageService);

        dotExperimentsService = spectator.inject(DotExperimentsService);
        dotExperimentsService.getById.mockReturnValue(of(EXPERIMENT_MOCK));
        dotExperimentsService.editVariant.mockReturnValue(of(EXPERIMENT_MOCK));
        dotExperimentsService.removeVariant.mockReturnValue(of(EXPERIMENT_MOCK));

        store.loadExperiment(EXPERIMENT_MOCK.id);
        spectator.detectChanges();
    });

    describe('should render', () => {
        it('a DEFAULT variant', () => {
            expect(spectator.queryAll(byTestId('variant-name')).length).toBe(1);
            expect(spectator.query(byTestId('variants-card-header'))).toHaveClass(
                'p-label-input-required'
            );

            expect(spectator.query(byTestId('variant-weight'))).toHaveText(
                EXPERIMENT_MOCK.trafficProportion.variants[0].weight + '.00% weight'
            );
            expect(spectator.query(byTestId('variant-preview-button'))).toExist();

            expect(spectator.query(byTestId('variant-title-step-done'))).not.toHaveClass('isDone');
        });

        it('should load the variant(s)', () => {
            const variants = [
                {
                    id: '0000000',
                    name: DEFAULT_VARIANT_NAME,
                    weight: 33.33,
                    url: 'link1'
                },
                { id: '1111111', name: 'b', weight: 33.33, url: 'link2' },
                { id: '2222222', name: 'c', weight: 33.33, url: 'link3' }
            ];

            loadExperiment(EXPERIMENT_MOCK, variants);

            expect(spectator.query(byTestId('variant-title-step-done'))).toHaveClass('isDone');
            expect(spectator.queryAll(Card).length).toBe(4);

            const variantsName = spectator.queryAll(byTestId('variant-name'));
            expect(variantsName[0]).toContainText(variants[0].name);
            expect(variantsName[1]).toContainText(variants[1].name);
            expect(variantsName[2]).toContainText(variants[2].name);

            expect(spectator.queryAll(DotCopyButtonComponent).length).toBe(3);

            expect(spectator.queryAll(Inplace).length).toBe(2);

            const variantsWeight = spectator.queryAll(byTestId('variant-weight'));
            expect(variantsWeight[0]).toContainText(variants[0].weight.toString());
            expect(variantsWeight[1]).toContainText(variants[1].weight.toString());
            expect(variantsWeight[2]).toContainText(variants[2].weight.toString());

            const variantsViewButton = spectator.queryAll(
                byTestId('variant-preview-button')
            ) as HTMLButtonElement[];

            expect(variantsViewButton.length).toBe(1);
            expect(variantsViewButton[0]).toContainText('view');

            const variantsEditButton = spectator.queryAll(
                byTestId('variant-edit-button')
            ) as HTMLButtonElement[];

            expect(variantsEditButton.length).toBe(2);
            expect(variantsEditButton[0]).toContainText('edit');

            const variantsDeleteButton = spectator.queryAll(
                byTestId('variant-delete-button')
            ) as HTMLButtonElement[];
            expect(variantsDeleteButton.length).toBe(3);
            expect(variantsDeleteButton[0]).toContainText('delete');
            expect(variantsDeleteButton[0].disabled).toBe(true);
            expect(variantsDeleteButton[1].disabled).toBe(false);
            expect(variantsDeleteButton[2].disabled).toBe(false);

            const addVariantButton = spectator.query(
                byTestId('variant-add-button')
            ) as HTMLButtonElement;
            expect(addVariantButton.disabled).toBe(true);
        });
    });

    describe('interactions', () => {
        const variants = [
            {
                id: DEFAULT_VARIANT_ID,
                name: DEFAULT_VARIANT_NAME,
                weight: 33.33,
                url: 'link1',
                promoted: false
            },
            { id: '1111111', name: 'test', weight: 33.33, url: 'link2', promoted: false }
        ];
        beforeEach(() => {
            loadExperiment(EXPERIMENT_MOCK, variants);
            spectator.detectChanges();
        });

        it('should open sideBar to add a variant ', () => {
            loadExperiment(EXPERIMENT_MOCK, [
                {
                    id: DEFAULT_VARIANT_ID,
                    name: DEFAULT_VARIANT_NAME,
                    weight: 33.33,
                    url: 'link1'
                }
            ]);
            jest.spyOn(store, 'openSidebar');
            spectator.click(byTestId('variant-add-button'));

            expect(store.openSidebar).toHaveBeenCalledWith(ExperimentSteps.VARIANTS);
        });

        it('should open sideBar to edit the variant weight ', () => {
            jest.spyOn(store, 'openSidebar');
            spectator.click(byTestId('variant-weight'));

            expect(store.openSidebar).toHaveBeenCalledWith(ExperimentSteps.TRAFFICS_SPLIT);
        });

        it('should goToEditPage emit a variant and mode(preview) when View button is clicked', () => {
            spectator.click(byTestId('variant-preview-button'));

            expect(dotSessionStorageService.setVariationId).toHaveBeenCalledWith(variants[0].id);
            expect(router.navigate).toHaveBeenCalledWith(['edit-page/content'], {
                queryParams: {
                    mode: DotPageMode.PREVIEW,
                    variantName: variants[0].id,
                    experimentId: 'test'
                },
                queryParamsHandling: 'merge'
            });
        });

        it('should goToEditPage emit a variant and mode(edit) when edit button is clicked', () => {
            spectator.click(byTestId('variant-edit-button'));

            expect(dotSessionStorageService.setVariationId).toHaveBeenCalledWith(variants[1].id);
            expect(router.navigate).toHaveBeenCalledWith(['edit-page/content'], {
                queryParams: {
                    mode: DotPageMode.EDIT,
                    variantName: variants[1].id,
                    experimentId: 'test'
                },
                queryParamsHandling: 'merge'
            });
        });

        it('should edit output emit the new name', () => {
            jest.spyOn(store, 'editVariant');

            const newVariantName = 'new name';

            spectator.query(Inplace).activate();

            spectator.detectComponentChanges();

            const viewButton = spectator.query(
                byTestId('variant-save-name-btn')
            ) as HTMLButtonElement;

            const inplaceInput = spectator.query(byTestId('inplace-input')) as HTMLInputElement;
            inplaceInput.value = newVariantName;
            spectator.detectComponentChanges();

            expect(viewButton.disabled).not.toBe(true);

            spectator.click(viewButton);

            spectator.detectComponentChanges();

            expect(store.editVariant).toHaveBeenCalledWith({
                data: { ...variants[1], name: newVariantName },
                experimentId: EXPERIMENT_MOCK.id
            });
        });

        it('should save when press enter', async () => {
            jest.spyOn(store, 'editVariant');

            spectator.query(Inplace).activate();

            spectator.detectComponentChanges();

            await spectator.fixture.whenStable();

            const inplaceInput = spectator.query(byTestId('inplace-input')) as HTMLInputElement;
            inplaceInput.value = 'new value';
            spectator.detectComponentChanges();

            spectator.dispatchKeyboardEvent(inplaceInput, 'keydown', 'Enter');

            spectator.detectComponentChanges();

            expect(store.editVariant).toHaveBeenCalledWith({
                data: { ...variants[1], name: 'new value' },
                experimentId: EXPERIMENT_MOCK.id
            });
        });

        it('should confirm before delete a variant', () => {
            jest.spyOn(store, 'deleteVariant');

            const button = spectator.queryLast(byTestId('variant-delete-button'));

            spectator.click(button);

            spectator.query(ConfirmPopup).accept();

            expect(store.deleteVariant).toHaveBeenCalledWith({
                experimentId: '111',
                variant: variants[1]
            });
        });

        it('should disable tooltip if is on draft', () => {
            spectator.detectChanges();

            spectator
                .queryAll(Tooltip)
                .filter((tooltip) => tooltip.disabled != undefined)
                .forEach((tooltip) => {
                    expect(tooltip.disabled).toEqual(true);
                });
        });

        it('should disable button and show tooltip when experiment is nos on draft', () => {
            dotExperimentsService.getById.mockReturnValue(
                of({
                    ...EXPERIMENT_MOCK_2,
                    ...{ status: DotExperimentStatusList.RUNNING }
                })
            );

            store.loadExperiment(EXPERIMENT_MOCK_2.id);
            spectator.detectChanges();

            expect(spectator.queryAll(byTestId('variant-weight'))).toHaveAttribute('disabled');
            expect(spectator.queryAll(byTestId('variant-delete-button'))).toHaveAttribute(
                'disabled'
            );
            expect(spectator.query(byTestId('variant-add-button'))).toHaveAttribute('disabled');

            const enableTooltips = spectator
                .queryAll(Tooltip)
                .filter((tooltip) => tooltip.disabled == false);

            // Two: variant weight
            // One: Delete variant
            // One: Add New Variant.
            expect(enableTooltips.length).toEqual(4);
        });

        it('should view button on all variants when experiment is not on draft', () => {
            dotExperimentsService.getById.mockReturnValue(
                of({
                    ...EXPERIMENT_MOCK_2,
                    ...{ status: DotExperimentStatusList.RUNNING }
                })
            );

            store.loadExperiment(EXPERIMENT_MOCK_2.id);

            spectator.detectChanges();

            const variantsViewButton = spectator.queryAll(
                byTestId('variant-preview-button')
            ) as HTMLButtonElement[];

            expect(variantsViewButton.length).toBe(2);
        });
    });

    function loadExperiment(mock, variants) {
        dotExperimentsService.getById.mockReturnValue(
            of({
                ...mock,
                trafficProportion: {
                    ...mock.trafficProportion,
                    variants: variants
                }
            })
        );

        store.loadExperiment(mock.id);
        spectator.detectComponentChanges();
    }
});
