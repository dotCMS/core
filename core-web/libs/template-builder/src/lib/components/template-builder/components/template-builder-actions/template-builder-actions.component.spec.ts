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

    it('should emit selectTheme event when style button is clicked', () => {
        const spy = jest.spyOn(spectator.component.selectTheme, 'emit');
        spectator.detectChanges();
        const btnSelectStyles = spectator.query(byTestId('btn-select-theme'));
        spectator.dispatchMouseEvent(btnSelectStyles, 'click');

        expect(spy).toHaveBeenCalled();
    });
});
