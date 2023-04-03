import { byTestId, createComponentFactory, Spectator } from '@ngneat/spectator';

import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';

import { DotDialogComponent } from '@components/dot-dialog/dot-dialog.component';
import { DotDialogModule } from '@components/dot-dialog/dot-dialog.module';
import { DotMessageService } from '@dotcms/data-access';
import { MockDotMessageService } from '@dotcms/utils-testing';
import { DotMessagePipeModule } from '@pipes/dot-message/dot-message-pipe.module';
import { VARIANT_RESULT_MOCK_1 } from '@portlets/dot-experiments/test/mocks';

import { DotExperimentsPublishVariantComponent } from './dot-experiments-publish-variant.component';

const messageServiceMock = new MockDotMessageService({
    'experiments.report.promote.variant': 'Publish Variant',
    'experiments.report.promote.variant.text': 'Text copy',
    'experiments.report.promote.assign.variant': 'Assign Variant',
    cancel: 'Cancel'
});

describe('DotExperimentsPublishVariantComponent', () => {
    let spectator: Spectator<DotExperimentsPublishVariantComponent>;

    const createComponent = createComponentFactory({
        component: DotExperimentsPublishVariantComponent,
        imports: [
            CommonModule,
            DotMessagePipeModule,
            TableModule,
            TagModule,
            ButtonModule,
            ConfirmDialogModule,
            DotDialogModule,
            FormsModule
        ],
        providers: [
            {
                provide: DotMessageService,
                useValue: messageServiceMock
            }
        ]
    });

    beforeEach(async () => {
        spectator = createComponent({ detectChanges: false });
        spectator.component.data = VARIANT_RESULT_MOCK_1;
        spectator.detectChanges();
    });

    it('should load correctly', () => {
        const dialog = spectator.query(DotDialogComponent);

        expect(dialog.header).toEqual('Publish Variant');
        expect(spectator.query(byTestId('variant-legend'))).toHaveText('Text copy');
        expect(dialog.actions.accept.disabled).toEqual(true);
        expect(dialog.actions.accept.label).toEqual('Assign Variant');
        expect(dialog.actions.cancel.label).toEqual('Cancel');
    });

    it('should publish variant ', () => {
        spyOn(spectator.component.publish, 'emit');
        spectator.click(spectator.query('input[type="radio"]'));
        spectator.click(spectator.query(byTestId('dotDialogAcceptAction')));

        expect(spectator.component.publish.emit).toHaveBeenCalledWith(VARIANT_RESULT_MOCK_1[0].id);
    });

    it('should close the dialog', () => {
        spyOn(spectator.component.hide, 'emit');
        spectator.click(spectator.query(byTestId('dotDialogCancelAction')));
        expect(spectator.component.hide.emit).toHaveBeenCalledTimes(1);
    });
});
