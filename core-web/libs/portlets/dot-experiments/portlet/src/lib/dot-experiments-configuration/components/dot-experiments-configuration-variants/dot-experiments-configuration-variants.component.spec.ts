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

import {
    DotExperimentsService,
    DotHttpErrorManagerService,
    DotMessageService
} from '@dotcms/data-access';
import {
    DEFAULT_VARIANT_ID,
    DEFAULT_VARIANT_NAME,
    DotExperimentStatus,
    DotPageMode,
    ExperimentSteps
} from '@dotcms/dotcms-models';
import { DotCopyButtonComponent, DotMessagePipe } from '@dotcms/ui';
import {
    ACTIVE_ROUTE_MOCK_CONFIG,
    getExperimentMock,
    MockDotMessageService,
    PARENT_RESOLVERS_ACTIVE_ROUTE_DATA
} from '@dotcms/utils-testing';

import { DotExperimentsConfigurationVariantsComponent } from './dot-experiments-configuration-variants.component';

import { DotExperimentsInlineEditTextComponent } from '../../../shared/ui/dot-experiments-inline-edit-text/dot-experiments-inline-edit-text.component';
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

const LOCAL_PARENT_RESOLVERS_ACTIVE_ROUTE_DATA = PARENT_RESOLVERS_ACTIVE_ROUTE_DATA;
LOCAL_PARENT_RESOLVERS_ACTIVE_ROUTE_DATA.parent.parent.snapshot.data.content.page.canLock = true;

const ActivatedRouteMock = {
    snapshot: {
        params: {
            experimentId: 'test'
        },
        data: ACTIVE_ROUTE_MOCK_CONFIG.snapshot.data
    },
    parent: {
        ...LOCAL_PARENT_RESOLVERS_ACTIVE_ROUTE_DATA
    }
};

const EXPERIMENT_MOCK = getExperimentMock(0);
const EXPERIMENT_MOCK_2 = getExperimentMock(2);

describe('DotExperimentsConfigurationVariantsComponent', () => {
    let spectator: Spectator<DotExperimentsConfigurationVariantsComponent>;
    let store: DotExperimentsConfigurationStore;
    let dotExperimentsService: SpyObject<DotExperimentsService>;
    let router: Router;

    let confirmationService: ConfirmationService;

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

        confirmationService = spectator.inject(ConfirmationService);

        dotExperimentsService = spectator.inject(DotExperimentsService);
        dotExperimentsService.getById.mockReturnValue(of(EXPERIMENT_MOCK));
        dotExperimentsService.editVariant.mockReturnValue(of(EXPERIMENT_MOCK));
        dotExperimentsService.removeVariant.mockReturnValue(of(EXPERIMENT_MOCK));

        store.loadExperiment(EXPERIMENT_MOCK.id);
        spectator.detectChanges();
    });

    describe('should render', () => {
        it('a DEFAULT variant', () => {
            expect(spectator.queryAll(byTestId('variant-name-original')).length).toBe(1);
            expect(spectator.query(byTestId('variants-card-header'))).toHaveClass(
                "after:content-['*']"
            );

            expect(spectator.query(byTestId('variant-weight'))).toHaveText(
                EXPERIMENT_MOCK.trafficProportion.variants[0].weight + '.00% weight'
            );
            expect(spectator.query(byTestId('variant-preview-button'))).toExist();

            expect(spectator.query(byTestId('variant-title-step-done'))).toHaveClass(
                'text-gray-500'
            );
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

            expect(spectator.query(byTestId('variant-title-step-done'))).toHaveClass(
                'text-green-600'
            );
            expect(spectator.queryAll(Card).length).toBe(4);

            expect(spectator.query(byTestId('variant-name-original'))).toContainText(
                variants[0].name
            );
            expect(spectator.queryAll(DotExperimentsInlineEditTextComponent).length).toBe(2);

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

            const variantsDeleteButtonWrappers = spectator.queryAll(
                byTestId('variant-delete-button')
            );
            expect(variantsDeleteButtonWrappers.length).toBe(3);
            expect(variantsDeleteButtonWrappers[0]).toContainText('delete');

            const deleteButton0 =
                variantsDeleteButtonWrappers[0].querySelector('button') ||
                variantsDeleteButtonWrappers[0];
            const deleteButton1 =
                variantsDeleteButtonWrappers[1].querySelector('button') ||
                variantsDeleteButtonWrappers[1];
            const deleteButton2 =
                variantsDeleteButtonWrappers[2].querySelector('button') ||
                variantsDeleteButtonWrappers[2];
            expect(deleteButton0.hasAttribute('disabled')).toBe(true);
            expect(deleteButton1.hasAttribute('disabled')).toBe(false);
            expect(deleteButton2.hasAttribute('disabled')).toBe(false);

            const addVariantButtonWrapper = spectator.query(byTestId('variant-add-button'));
            const addVariantButton =
                addVariantButtonWrapper.querySelector('button') || addVariantButtonWrapper;
            expect(addVariantButton.hasAttribute('disabled')).toBe(true);
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
            {
                id: '1111111',
                name: 'test',
                weight: 33.33,
                url: 'link2',
                promoted: false
            }
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

            const addButtonWrapper = spectator.query(byTestId('variant-add-button'));
            const addButton = addButtonWrapper.querySelector('button') || addButtonWrapper;
            spectator.click(addButton);

            expect(store.openSidebar).toHaveBeenCalledWith(ExperimentSteps.VARIANTS);
        });

        it('should open sideBar to edit the variant weight ', () => {
            jest.spyOn(store, 'openSidebar');

            const variantWeightWrapper = spectator.query(byTestId('variant-weight'));
            const variantWeightButton =
                variantWeightWrapper.querySelector('button') || variantWeightWrapper;
            spectator.click(variantWeightButton);

            expect(store.openSidebar).toHaveBeenCalledWith(ExperimentSteps.TRAFFICS_SPLIT);
        });

        it('should goToEditPage emit a variant and mode(preview) when View button is clicked', () => {
            const previewButtonWrapper = spectator.query(byTestId('variant-preview-button'));
            const previewButton =
                previewButtonWrapper.querySelector('button') || previewButtonWrapper;
            spectator.click(previewButton);

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
            const editButtonWrapper = spectator.query(byTestId('variant-edit-button'));
            const editButton = editButtonWrapper.querySelector('button') || editButtonWrapper;
            spectator.click(editButton);

            expect(router.navigate).toHaveBeenCalledWith(['edit-page/content'], {
                queryParams: {
                    mode: DotPageMode.EDIT,
                    variantName: variants[1].id,
                    experimentId: 'test'
                },
                queryParamsHandling: 'merge'
            });
        });

        it('should has `DotExperimentsInlineEditTextComponent` component', () => {
            expect(spectator.query(DotExperimentsInlineEditTextComponent)).not.toBeNull();
        });

        it('should confirm before delete a variant', () => {
            jest.spyOn(store, 'deleteVariant');
            jest.spyOn(confirmationService, 'confirm');

            const buttonWrapper = spectator.queryLast(byTestId('variant-delete-button'));
            const button = buttonWrapper.querySelector('button') || buttonWrapper;

            spectator.click(button);

            expect(confirmationService.confirm).toHaveBeenCalled();
        });

        it('should disable tooltip if not have a valid error label', () => {
            spectator.detectChanges();

            spectator
                .queryAll(Tooltip)
                .filter((tooltip) => tooltip.disabled != undefined)
                .forEach((tooltip) => {
                    expect(tooltip.disabled).toEqual(true);
                });
        });

        it('should disable button and show tooltip when experiment have an error label', () => {
            dotExperimentsService.getById.mockReturnValue(
                of({
                    ...EXPERIMENT_MOCK_2,
                    ...{ status: DotExperimentStatus.RUNNING }
                })
            );

            store.loadExperiment(EXPERIMENT_MOCK_2.id);
            spectator.detectChanges();

            // Check variant weight buttons are disabled
            const variantWeights = spectator.queryAll(byTestId('variant-weight'));
            variantWeights.forEach((weight) => {
                const button = weight.querySelector('button') || weight;
                expect(button.hasAttribute('disabled')).toBe(true);
            });

            // Check variant delete buttons are disabled
            const deleteButtonWrappers = spectator.queryAll(byTestId('variant-delete-button'));
            deleteButtonWrappers.forEach((wrapper) => {
                const button = wrapper.querySelector('button') || wrapper;
                expect(button.hasAttribute('disabled')).toBe(true);
            });

            // Check add variant button is disabled
            const addButtonWrapper = spectator.query(byTestId('variant-add-button'));
            const addButton = addButtonWrapper.querySelector('button') || addButtonWrapper;
            expect(addButton.hasAttribute('disabled')).toBe(true);

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
                    ...{ status: DotExperimentStatus.RUNNING }
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
