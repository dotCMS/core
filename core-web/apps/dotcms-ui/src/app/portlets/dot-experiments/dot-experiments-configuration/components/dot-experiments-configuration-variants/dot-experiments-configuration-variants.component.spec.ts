import { byTestId, createComponentFactory, Spectator } from '@ngneat/spectator';

import { DecimalPipe } from '@angular/common';

import { ButtonModule } from 'primeng/button';
import { Card, CardModule } from 'primeng/card';
import { Inplace, InplaceModule } from 'primeng/inplace';

import { DotCopyButtonComponent } from '@components/dot-copy-button/dot-copy-button.component';
import { DotCopyButtonModule } from '@components/dot-copy-button/dot-copy-button.module';
import { DotMessageService } from '@dotcms/data-access';
import {
    DEFAULT_VARIANT_ID,
    DEFAULT_VARIANT_NAME,
    ExperimentSteps,
    SidebarStatus,
    Status,
    Variant
} from '@dotcms/dotcms-models';
import { MockDotMessageService } from '@dotcms/utils-testing';
import { DotExperimentsConfigurationVariantsAddComponent } from '@portlets/dot-experiments/dot-experiments-configuration/components/dot-experiments-configuration-variants-add/dot-experiments-configuration-variants-add.component';

import { DotExperimentsConfigurationVariantsComponent } from './dot-experiments-configuration-variants.component';

const messageServiceMock = new MockDotMessageService({
    'experiments.configure.variants.weight': 'weight',
    'experiments.configure.variants.view': 'view',
    'experiments.action.edit': 'edit',
    'experiments.configure.variants.delete': 'delete',
    'experiments.configure.variants.add': 'Add new variant'
});
describe('DotExperimentsConfigurationVariantsComponent', () => {
    let spectator: Spectator<DotExperimentsConfigurationVariantsComponent>;

    let configurationVariantsAddComponent: DotExperimentsConfigurationVariantsAddComponent;

    const createComponent = createComponentFactory({
        imports: [
            ButtonModule,
            CardModule,
            InplaceModule,
            DecimalPipe,
            DotExperimentsConfigurationVariantsAddComponent,
            DotCopyButtonModule
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
                stepStatus: {
                    status: Status.IDLE,
                    experimentStep: ExperimentSteps.VARIANTS,
                    isOpen: false
                },
                variants: [{ id: DEFAULT_VARIANT_ID, name: DEFAULT_VARIANT_NAME, weight: '100' }]
            };

            spectator.setInput(variantsVm);
            spectator.detectChanges();

            expect(spectator.queryAll(byTestId('variant-name')).length).toBe(1);

            expect(spectator.query(byTestId('variant-weight'))).toHaveText(
                variantsVm.variants[0].weight + '.00% weight'
            );
            expect(spectator.query(byTestId('variant-preview-button'))).toExist();

            expect(spectator.query(byTestId('variant-title-step-done'))).not.toHaveClass('isDone');
        });

        it('should load the variant(s)', () => {
            const variantsVm = {
                stepStatus: {
                    status: Status.IDLE,
                    experimentStep: ExperimentSteps.VARIANTS,
                    isOpen: false
                },
                variants: [
                    { id: '0000000', name: DEFAULT_VARIANT_NAME, weight: '33.33', url: 'link1' },
                    { id: '1111111', name: 'b', weight: '33.33', url: 'link2' },
                    { id: '2222222', name: 'c', weight: '33.33', url: 'link3' }
                ]
            };

            spectator.setInput(variantsVm);

            spectator.detectComponentChanges();

            expect(spectator.query(byTestId('variant-title-step-done'))).toHaveClass('isDone');
            expect(spectator.queryAll(Card).length).toBe(4);

            const variantsName = spectator.queryAll(byTestId('variant-name'));
            expect(variantsName[0]).toContainText(variantsVm.variants[0].name);
            expect(variantsName[1]).toContainText(variantsVm.variants[1].name);
            expect(variantsName[2]).toContainText(variantsVm.variants[2].name);

            expect(spectator.queryAll(DotCopyButtonComponent).length).toBe(3);

            expect(spectator.queryAll(Inplace).length).toBe(2);

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
        const variantsVm = {
            stepStatus: {
                status: Status.IDLE,
                experimentStep: ExperimentSteps.VARIANTS,
                isOpen: false
            },
            variants: [
                {
                    id: DEFAULT_VARIANT_ID,
                    name: DEFAULT_VARIANT_NAME,
                    weight: '33.33',
                    url: 'link1'
                },
                { id: '1111111', name: 'b', weight: '33.33', url: 'link2' }
            ]
        };
        beforeEach(() => {
            spectator.setInput(variantsVm);

            spectator.detectChanges();
            configurationVariantsAddComponent = spectator.query(
                DotExperimentsConfigurationVariantsAddComponent
            );
        });

        it('should sidebarStatusChanged emit OPEN when add new variant button is enable and clicked', () => {
            let output;
            spectator.output('sidebarStatusChanged').subscribe((result) => (output = result));

            const addButton = spectator.query(byTestId('variant-add-button')) as HTMLButtonElement;

            expect(addButton.disabled).not.toBe(true);
            spectator.click(addButton);

            expect(output).toEqual(SidebarStatus.OPEN);
        });

        it('should goToEditPage emit a variant and mode(preview) when View button is clicked', () => {
            let output;
            spectator.output('goToEditPage').subscribe((result) => (output = result));

            const viewButton = spectator.query(
                byTestId('variant-preview-button')
            ) as HTMLButtonElement;

            expect(viewButton.disabled).not.toBe(true);
            spectator.click(viewButton);

            expect(output).toEqual({ variant: variantsVm.variants[0], mode: 'preview' });
        });

        it('should goToEditPage emit a variant and mode(edit) when edit button is clicked', () => {
            let output;
            spectator.output('goToEditPage').subscribe((result) => (output = result));

            const viewButton = spectator.query(
                byTestId('variant-edit-button')
            ) as HTMLButtonElement;

            expect(viewButton.disabled).not.toBe(true);
            spectator.click(viewButton);

            expect(output).toEqual({ variant: variantsVm.variants[1], mode: 'edit' });
        });

        it('should edit output emit the new name', () => {
            const newVariantName = 'new name';
            const variants: Variant[] = [
                { id: '1', name: DEFAULT_VARIANT_NAME, weight: '50.00', url: 'url' },
                { id: '2', name: 'to edit', weight: '50.00', url: 'url' }
            ];

            let output;
            spectator.output('edit').subscribe((result) => (output = result));

            spectator.setInput({
                variants
            });

            spectator.query(Inplace).activate();

            spectator.detectComponentChanges();

            const viewButton = spectator.query(
                byTestId('variant-save-name-btn')
            ) as HTMLButtonElement;

            const inplaceInput = spectator.query(byTestId('inplace-input')) as HTMLInputElement;
            inplaceInput.value = newVariantName;

            expect(viewButton.disabled).not.toBe(true);
            spectator.click(viewButton);

            expect(output).toEqual({ ...variants[1], name: newVariantName });
        });

        it('should delete a variant', () => {
            let output;
            spectator.output('delete').subscribe((result) => (output = result));

            const deleteButtons = spectator.queryAll(
                byTestId('variant-delete-button')
            ) as HTMLButtonElement[];

            expect(deleteButtons[0].disabled).toBe(true);
            expect(deleteButtons[1].disabled).not.toBe(true);

            spectator.click(deleteButtons[1]);

            expect(output).toEqual(variantsVm.variants[1]);
        });

        it('should emit a the form values when when save', () => {
            let output;
            spectator.output('save').subscribe((result) => (output = result));

            configurationVariantsAddComponent.form.patchValue({ name: 'value' });
            configurationVariantsAddComponent.saveForm();
            spectator.detectChanges();

            expect(output).toEqual({ name: 'value' });
        });
    });
});
