import {
    byTestId,
    createComponentFactory,
    mockProvider,
    Spectator,
    SpyObject
} from '@ngneat/spectator';
import { of } from 'rxjs';

import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

import { MessageService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { TableRadioButton } from 'primeng/table';
import { TagModule } from 'primeng/tag';

import { DotDialogComponent } from '@components/dot-dialog/dot-dialog.component';
import { DotDialogModule } from '@components/dot-dialog/dot-dialog.module';
import { DotMessageService } from '@dotcms/data-access';
import { MockDotMessageService } from '@dotcms/utils-testing';
import { DotMessagePipeModule } from '@pipes/dot-message/dot-message-pipe.module';
import { DotExperimentsReportsStore } from '@portlets/dot-experiments/dot-experiments-reports/store/dot-experiments-reports-store';
import { DotExperimentsService } from '@portlets/dot-experiments/shared/services/dot-experiments.service';
import { getExperimentMock, getExperimentResultsMock } from '@portlets/dot-experiments/test/mocks';
import { DotHttpErrorManagerService } from '@services/dot-http-error-manager/dot-http-error-manager.service';

import { DotExperimentsPublishVariantComponent } from './dot-experiments-publish-variant.component';

const messageServiceMock = new MockDotMessageService({
    'experiments.report.promote.variant': 'Promote Variant',
    'experiments.report.promote.variant.text': 'Text copy',
    cancel: 'Cancel'
});

const EXPERIMENT_MOCK = getExperimentMock(1);
const EXPERIMENT_RESULTS_MOCK = getExperimentResultsMock(0);

describe('DotExperimentsPublishVariantComponent', () => {
    let spectator: Spectator<DotExperimentsPublishVariantComponent>;
    let store: DotExperimentsReportsStore;
    let dotExperimentsService: SpyObject<DotExperimentsService>;

    const createComponent = createComponentFactory({
        component: DotExperimentsPublishVariantComponent,
        imports: [
            CommonModule,
            DotMessagePipeModule,
            TagModule,
            ButtonModule,
            ConfirmDialogModule,
            DotDialogModule,
            FormsModule
        ],
        providers: [
            DotExperimentsReportsStore,
            mockProvider(DotExperimentsService),
            mockProvider(DotHttpErrorManagerService),
            MessageService,
            {
                provide: DotMessageService,
                useValue: messageServiceMock
            }
        ]
    });

    beforeEach(async () => {
        spectator = createComponent({ detectChanges: false });

        store = spectator.inject(DotExperimentsReportsStore);
        dotExperimentsService = spectator.inject(DotExperimentsService);
    });

    describe('with no winner', () => {
        beforeEach(async () => {
            dotExperimentsService.getById.and.returnValue(of(EXPERIMENT_MOCK));
            dotExperimentsService.getResults.and.returnValue(of({ ...EXPERIMENT_RESULTS_MOCK }));

            store.loadExperimentAndResults(EXPERIMENT_MOCK.id);
        });
        it('should load correctly', () => {
            store.showPromoteDialog();
            spectator.detectComponentChanges();

            const dialog = spectator.query(DotDialogComponent);

            expect(dialog.header).toEqual('Promote Variant');
            expect(spectator.query(byTestId('variant-legend'))).toHaveText('Text copy');
            expect(dialog.actions.accept.disabled).toEqual(true);
            expect(dialog.actions.accept.label).toEqual('Promote Variant');
            expect(dialog.actions.cancel.label).toEqual('Cancel');
        });

        it('should render variants', () => {
            store.showPromoteDialog();
            spectator.detectComponentChanges();

            const radioButtons = spectator.queryAll(TableRadioButton);
            expect(radioButtons.length).toEqual(2);
            expect(radioButtons[0].disabled).toEqual(false);
            expect(radioButtons[1].disabled).toEqual(true);

            const variantNames = spectator.queryAll(byTestId('variant-name'));
            expect(variantNames.length).toEqual(2);
            expect(variantNames[1].innerHTML).toEqual(
                EXPERIMENT_RESULTS_MOCK.goals.primary.variants.DEFAULT.variantDescription
            );
            expect(variantNames[0].innerHTML).toEqual(
                EXPERIMENT_RESULTS_MOCK.goals.primary.variants['111'].variantDescription
            );

            const probabilities = spectator.queryAll(byTestId('variant-percent'));
            expect(probabilities[1].innerHTML).toEqual('60%');
            expect(probabilities[0].innerHTML).toEqual('40%');

            expect(spectator.queryAll(byTestId('variant-winner-tag')).length).toEqual(0);
            expect(spectator.queryAll(byTestId('variant-promoted-tag')).length).toEqual(0);
        });

        it('should store close the dialog', () => {
            store.showPromoteDialog();
            spectator.detectComponentChanges();

            const dialog = spectator.query(DotDialogComponent);

            expect(dialog.visible).toEqual(true);

            store.hidePromoteDialog();
            spectator.detectComponentChanges();

            expect(dialog.visible).toEqual(false);
        });

        describe('save action', () => {
            it('should allow save button when you select one option', async () => {
                store.showPromoteDialog();
                spectator.detectComponentChanges();

                const dialog = spectator.query(DotDialogComponent);

                await spectator.fixture.whenStable();

                spectator.click(spectator.query(byTestId('variant-radio-button')));
                spectator.detectComponentChanges();

                expect(dialog.actions.accept.disabled).toEqual(false);
            });
        });
    });
    describe('with winner and promoted', () => {
        beforeEach(async () => {
            const EXPERIMENT_MOCK = getExperimentMock(4);
            const EXPERIMENT_RESULTS_MOCK = getExperimentResultsMock(1);

            dotExperimentsService.getById.and.returnValue(of(EXPERIMENT_MOCK));
            dotExperimentsService.getResults.and.returnValue(of({ ...EXPERIMENT_RESULTS_MOCK }));

            store.loadExperimentAndResults(EXPERIMENT_MOCK.id);
        });

        it('should show winner tag and promoted tag', () => {
            store.showPromoteDialog();
            spectator.detectComponentChanges();

            expect(spectator.queryAll(byTestId('variant-winner-tag')).length).toEqual(1);
            expect(spectator.queryAll(byTestId('variant-promoted-tag')).length).toEqual(1);
        });
    });
});
