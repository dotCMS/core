import { DotExperimentsConfigurationVariantsComponent } from './dot-experiments-configuration-variants.component';
import { byTestId, createComponentFactory, Spectator } from '@ngneat/spectator';
import { ButtonModule } from 'primeng/button';
import { Card, CardModule } from 'primeng/card';
import { DotMessageService } from '@services/dot-message/dot-messages.service';
import { MockDotMessageService } from '@tests/dot-message-service.mock';
import { DecimalPipe } from '@angular/common';
import { DEFAULT_VARIANT_ID } from '@portlets/dot-experiments/shared/models/dot-experiments-constants';
import { DotExperimentsConfigurationVariantsAddComponent } from '@portlets/dot-experiments/dot-experiments-configuration/components/dot-experiments-configuration-variants-add/dot-experiments-configuration-variants-add.component';
import { Status } from '@portlets/shared/models/shared-models';
import { ExperimentSteps } from '@portlets/dot-experiments/shared/models/dot-experiments.model';

const messageServiceMock = new MockDotMessageService({
    'experiments.configure.variants.weight': 'weight',
    'experiments.configure.variants.view': 'view',
    'experiments.configure.variants.edit': 'edit',
    'experiments.configure.variants.delete': 'delete',
    'experiments.configure.variants.add': 'Add new variant'
});
describe('DotExperimentsConfigurationVariantsComponent', () => {
    let spectator: Spectator<DotExperimentsConfigurationVariantsComponent>;

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
            }
        ]
    });

    beforeEach(() => {
        spectator = createComponent({
            detectChanges: false
        });
    });

    describe('should render', () => {
        it('a DEFAULT variant', () => {
            const variantsVm = {
                status: {
                    status: Status.IDLE,
                    step: ExperimentSteps.VARIANTS,
                    isOpenSidebar: false
                },
                variants: [{ id: DEFAULT_VARIANT_ID, name: 'a', weight: '100' }]
            };

            spectator.setInput(variantsVm);
            spectator.detectChanges();

            expect(spectator.query(byTestId('variant-name'))).toHaveText(
                variantsVm.variants[0].name
            );
            expect(spectator.query(byTestId('variant-weight'))).toHaveText(
                variantsVm.variants[0].weight + '.00% weight'
            );
            expect(spectator.query(byTestId('variant-preview-button'))).toExist();

            expect(spectator.query(byTestId('variant-title-step-done'))).not.toHaveClass('isDone');
        });

        it('the variant(s)', () => {
            const variantsVm = {
                status: {
                    status: Status.IDLE,
                    step: ExperimentSteps.VARIANTS,
                    isOpenSidebar: false
                },
                variants: [
                    { id: DEFAULT_VARIANT_ID, name: 'a', weight: '33.33', url: 'link1' },
                    { id: '1111111', name: 'b', weight: '33.33', url: 'link2' },
                    { id: '2222222', name: 'c', weight: '33.33', url: 'link3' }
                ]
            };

            spectator.setInput(variantsVm);

            spectator.detectChanges();

            expect(spectator.query(byTestId('variant-title-step-done'))).toHaveClass('isDone');
            expect(spectator.queryAll(Card).length).toBe(4);

            const variantsName = spectator.queryAll(byTestId('variant-name'));
            expect(variantsName[0]).toContainText(variantsVm.variants[0].name);
            expect(variantsName[1]).toContainText(variantsVm.variants[1].name);
            expect(variantsName[2]).toContainText(variantsVm.variants[2].name);

            const variantsUrl = spectator.queryAll(byTestId('variant-url'));
            expect(variantsUrl[0]).toContainText(variantsVm.variants[0].url);
            expect(variantsUrl[1]).toContainText(variantsVm.variants[1].url);
            expect(variantsUrl[2]).toContainText(variantsVm.variants[2].url);

            const variantsWeight = spectator.queryAll(byTestId('variant-weight'));
            expect(variantsWeight[0]).toContainText(variantsVm.variants[0].weight);
            expect(variantsWeight[1]).toContainText(variantsVm.variants[1].weight);
            expect(variantsWeight[2]).toContainText(variantsVm.variants[2].weight);

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
        beforeEach(() => {
            const variantsVm = {
                status: {
                    status: Status.IDLE,
                    step: ExperimentSteps.VARIANTS,
                    isOpenSidebar: false
                },
                variants: [
                    { id: DEFAULT_VARIANT_ID, name: 'a', weight: '33.33', url: 'link1' },
                    { id: '1111111', name: 'b', weight: '33.33', url: 'link2' }
                ]
            };

            spectator.setInput(variantsVm);

            spectator.detectChanges();
        });

        it('should call addNewVariant()', () => {
            const addNewVariantSpy = spyOn(spectator.component, 'showSidebar');
            const addButton = spectator.query(byTestId('variant-add-button')) as HTMLButtonElement;

            expect(addButton.disabled).not.toBe(true);
            spectator.click(addButton);

            expect(addNewVariantSpy).toHaveBeenCalledTimes(1);
        });

        it('should go to Edit Page with the queryParams editPageTab=preview and variationName set', () => {
            const viewButtontSpy = spyOn(spectator.component, 'goToEditPage');

            const viewButton = spectator.query(
                byTestId('variant-preview-button')
            ) as HTMLButtonElement;

            expect(viewButton.disabled).not.toBe(true);
            spectator.click(viewButton);

            expect(viewButtontSpy).toHaveBeenCalled();
        });
        it('should go to Edit Page with the queryParams editPageTab=edit and variationName set', () => {
            const editButtontSpy = spyOn(spectator.component, 'goToEditPage');

            const editButton = spectator.query(
                byTestId('variant-edit-button')
            ) as HTMLButtonElement;

            expect(editButton.disabled).not.toBe(true);
            spectator.click(editButton);

            expect(editButtontSpy).toHaveBeenCalledWith();
        });
        it('should delete a variant', () => {
            const deleteButtontSpy = spyOn(spectator.component, 'delete');

            const deleteButtons = spectator.queryAll(
                byTestId('variant-delete-button')
            ) as HTMLButtonElement[];

            expect(deleteButtons[0].disabled).toBe(true);
            expect(deleteButtons[1].disabled).not.toBe(true);

            spectator.click(deleteButtons[1]);

            expect(deleteButtontSpy).toHaveBeenCalled();
        });
    });
});
