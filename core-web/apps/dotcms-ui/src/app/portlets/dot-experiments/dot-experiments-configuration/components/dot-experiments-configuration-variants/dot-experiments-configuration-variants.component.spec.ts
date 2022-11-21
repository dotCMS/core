import { DotExperimentsConfigurationVariantsComponent } from './dot-experiments-configuration-variants.component';
import { byTestId, createComponentFactory, mockProvider, Spectator } from '@ngneat/spectator';
import { ButtonModule } from 'primeng/button';
import { Card, CardModule } from 'primeng/card';
import { DotMessageService } from '@services/dot-message/dot-messages.service';
import { MockDotMessageService } from '@tests/dot-message-service.mock';
import { DotExperimentsConfigurationStore } from '@portlets/dot-experiments/dot-experiments-configuration/store/dot-experiments-configuration-store.service';
import { DotExperimentsSessionStorageService } from '@portlets/dot-experiments/shared/services/dot-experiments-session-storage.service';
import {
    DotExperimentsConfigurationStoreMock,
    ExperimentMocks
} from '@portlets/dot-experiments/test/mocks';
import { of } from 'rxjs';
import { DecimalPipe } from '@angular/common';
import { DEFAULT_VARIANT_ID } from '@portlets/dot-experiments/shared/models/dot-experiments-constants';
import { DotExperimentsConfigurationVariantsAddComponent } from '@portlets/dot-experiments/dot-experiments-configuration/components/dot-experiments-configuration-variants-add/dot-experiments-configuration-variants-add.component';
import { Router } from '@angular/router';
import SpyObj = jasmine.SpyObj;

const messageServiceMock = new MockDotMessageService({
    'experiments.configure.variants.weight': 'weight',
    'experiments.configure.variants.view': 'view',
    'experiments.configure.variants.edit': 'edit',
    'experiments.configure.variants.delete': 'delete',
    'experiments.configure.variants.add': 'Add new variant'
});
describe('DotExperimentsConfigurationVariantsComponent', () => {
    let spectator: Spectator<DotExperimentsConfigurationVariantsComponent>;
    let dotExperimentsSessionStorageService: SpyObj<DotExperimentsSessionStorageService>;
    let dotExperimentsConfigurationStore: SpyObj<DotExperimentsConfigurationStore>;
    let router: SpyObj<Router>;

    const createComponent = createComponentFactory({
        imports: [
            ButtonModule,
            CardModule,
            DecimalPipe,
            DotExperimentsConfigurationVariantsAddComponent
        ],
        component: DotExperimentsConfigurationVariantsComponent,
        providers: [
            {
                provide: DotMessageService,
                useValue: messageServiceMock
            },

            {
                provide: DotExperimentsConfigurationStore,
                useValue: DotExperimentsConfigurationStoreMock
            },
            mockProvider(Router),
            mockProvider(DotExperimentsSessionStorageService)
        ]
    });

    beforeEach(() => {
        spectator = createComponent({
            detectChanges: false
        });

        dotExperimentsSessionStorageService = spectator.inject(DotExperimentsSessionStorageService);
        dotExperimentsConfigurationStore = spectator.inject(DotExperimentsConfigurationStore);
        router = spectator.inject(Router);
    });

    describe('should render', () => {
        it('a DEFAULT variant', () => {
            const variantsVm = {
                isSidebarOpen: false,
                isSaving: false,
                trafficProportion: {
                    ...ExperimentMocks[0].trafficProportion,
                    variants: [{ id: DEFAULT_VARIANT_ID, name: 'a', weight: '100' }]
                },
                isVariantStepDone: false
            };
            spectator.component.vm$ = of(variantsVm);

            spectator.detectChanges();

            expect(spectator.query(byTestId('variant-name'))).toHaveText(
                variantsVm.trafficProportion.variants[0].name
            );
            expect(spectator.query(byTestId('variant-weight'))).toHaveText(
                variantsVm.trafficProportion.variants[0].weight + '.00% weight'
            );
            expect(spectator.query(byTestId('variant-preview-button'))).toExist();

            expect(spectator.query(byTestId('variant-title-step-done'))).not.toHaveClass('isDone');
        });

        it('the variant(s)', () => {
            const variantsVm = {
                isSidebarOpen: false,
                isSaving: false,
                trafficProportion: {
                    ...ExperimentMocks[0].trafficProportion,
                    variants: [
                        { id: DEFAULT_VARIANT_ID, name: 'a', weight: '33.33', url: 'link1' },
                        { id: '1111111', name: 'b', weight: '33.33', url: 'link2' },
                        { id: '2222222', name: 'c', weight: '33.33', url: 'link3' }
                    ]
                },
                isVariantStepDone: true
            };

            spectator.component.vm$ = of(variantsVm);
            spectator.detectChanges();

            expect(spectator.query(byTestId('variant-title-step-done'))).toHaveClass('isDone');
            expect(spectator.queryAll(Card).length).toBe(4);

            const variantsName = spectator.queryAll(byTestId('variant-name'));
            expect(variantsName[0]).toContainText(variantsVm.trafficProportion.variants[0].name);
            expect(variantsName[1]).toContainText(variantsVm.trafficProportion.variants[1].name);
            expect(variantsName[2]).toContainText(variantsVm.trafficProportion.variants[2].name);

            const variantsUrl = spectator.queryAll(byTestId('variant-url'));
            expect(variantsUrl[0]).toContainText(variantsVm.trafficProportion.variants[0].url);
            expect(variantsUrl[1]).toContainText(variantsVm.trafficProportion.variants[1].url);
            expect(variantsUrl[2]).toContainText(variantsVm.trafficProportion.variants[2].url);

            const variantsWeight = spectator.queryAll(byTestId('variant-weight'));
            expect(variantsWeight[0]).toContainText(
                variantsVm.trafficProportion.variants[0].weight
            );
            expect(variantsWeight[1]).toContainText(
                variantsVm.trafficProportion.variants[1].weight
            );
            expect(variantsWeight[2]).toContainText(
                variantsVm.trafficProportion.variants[2].weight
            );

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
        let variantsVm;
        beforeEach(() => {
            variantsVm = {
                trafficProportion: {
                    ...ExperimentMocks[0].trafficProportion,
                    variants: [
                        { id: DEFAULT_VARIANT_ID, name: 'a', weight: '33.33', url: 'link1' },
                        { id: '1111111', name: 'b', weight: '33.33', url: 'link2' }
                    ]
                },
                isVariantStepDone: true
            };

            spectator.component.vm$ = of(variantsVm);
            spectator.detectChanges();
        });

        it('should call addNewVariant()', () => {
            const addNewVariantSpy = spyOn(spectator.component, 'addNewVariant');
            const addButton = spectator.query(byTestId('variant-add-button')) as HTMLButtonElement;

            expect(addButton.disabled).not.toBe(true);
            spectator.click(addButton);

            expect(addNewVariantSpy).toHaveBeenCalledTimes(1);
        });

        it('should go to Edit Page with the queryParams editPageTab=preview and variationName set', () => {
            const viewButton = spectator.query(
                byTestId('variant-preview-button')
            ) as HTMLButtonElement;

            expect(viewButton.disabled).not.toBe(true);
            spectator.click(viewButton);

            expect(dotExperimentsSessionStorageService.setVariationId).toHaveBeenCalledWith(
                DEFAULT_VARIANT_ID
            );
            expect(router.navigate).toHaveBeenCalledWith(['edit-page/content'], {
                queryParamsHandling: 'merge',
                queryParams: {
                    editPageTab: 'preview',
                    variationName: variantsVm.trafficProportion.variants[0].id
                }
            });
        });
        it('should go to Edit Page with the queryParams editPageTab=edit and variationName set', () => {
            const editButton = spectator.query(
                byTestId('variant-edit-button')
            ) as HTMLButtonElement;

            expect(editButton.disabled).not.toBe(true);
            spectator.click(editButton);

            expect(dotExperimentsSessionStorageService.setVariationId).toHaveBeenCalledWith(
                variantsVm.trafficProportion.variants[1].id
            );
            expect(router.navigate).toHaveBeenCalledWith(['edit-page/content'], {
                queryParamsHandling: 'merge',
                queryParams: {
                    editPageTab: 'edit',
                    variationName: variantsVm.trafficProportion.variants[1].id
                }
            });
        });
        it('should delete a variant', () => {
            spyOn(dotExperimentsConfigurationStore, 'deleteVariant');
            const deleteButtons = spectator.queryAll(
                byTestId('variant-delete-button')
            ) as HTMLButtonElement[];

            expect(deleteButtons[0].disabled).toBe(true);
            expect(deleteButtons[1].disabled).not.toBe(true);

            spectator.click(deleteButtons[1]);

            expect(dotExperimentsConfigurationStore.deleteVariant).toHaveBeenCalledWith(
                variantsVm.trafficProportion.variants[1]
            );
        });
    });
});
