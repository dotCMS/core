import { Spectator, byTestId, createComponentFactory } from '@ngneat/spectator/jest';

import { DotMessageService } from '@dotcms/data-access';
import { DotMessagePipe } from '@dotcms/ui';

import { TemplateBuilderActionsComponent } from './template-builder-actions.component';

import { DOT_MESSAGE_SERVICE_TB_MOCK } from '../../utils/mocks';

describe('TemplateBuilderActionsComponent', () => {
    let spectator: Spectator<TemplateBuilderActionsComponent>;
    const createComponent = createComponentFactory({
        component: TemplateBuilderActionsComponent,
        providers: [
            DotMessagePipe,
            {
                provide: DotMessageService,
                useValue: DOT_MESSAGE_SERVICE_TB_MOCK
            }
        ]
    });

    beforeEach(() => (spectator = createComponent()));

    it('should create', () => {
        expect(spectator.component).toBeTruthy();
    });

    it('should emit selectLayout event when layout button is clicked', () => {
        const spy = jest.spyOn(spectator.component.selectLayout, 'emit');
        spectator.detectChanges();
        const btnSelectLayout = spectator.query(byTestId('btn-select-layout'));
        spectator.dispatchMouseEvent(btnSelectLayout, 'onClick');
        expect(spy).toHaveBeenCalled();
    });

    it('should emit selectStyles event when style button is clicked', () => {
        const spy = jest.spyOn(spectator.component.selectStyles, 'emit');
        spectator.detectChanges();
        const btnSelectStyles = spectator.query(byTestId('btn-select-styles'));
        spectator.dispatchMouseEvent(btnSelectStyles, 'onClick');

        expect(spy).toHaveBeenCalled();
    });
});
