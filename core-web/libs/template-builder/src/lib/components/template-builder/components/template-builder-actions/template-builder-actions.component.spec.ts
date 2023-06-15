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

    beforeEach(
        () =>
            (spectator = createComponent({
                props: {
                    layoutProperties: {
                        footer: true,
                        header: false,
                        sidebar: {
                            location: 'left',
                            width: 'small',
                            containers: []
                        }
                    }
                }
            }))
    );

    it('should emit selectTheme event when style button is clicked', () => {
        const spy = jest.spyOn(spectator.component.selectTheme, 'emit');
        spectator.detectChanges();
        const btnSelectStyles = spectator.query(byTestId('btn-select-theme'));
        spectator.dispatchMouseEvent(btnSelectStyles, 'onClick');

        expect(spy).toHaveBeenCalled();
    });

    it('should open an overlayPanel event when layout button is clicked', () => {
        spectator.detectChanges();
        const btnSelectStyles = spectator.query(byTestId('btn-select-layout'));
        spectator.dispatchMouseEvent(btnSelectStyles, 'click');

        expect(spectator.query('p-overlaypanel')).toBeTruthy();
    });

    it('should emit changes everytime the layout properties changes', () => {
        const changesMock = jest.spyOn(spectator.component.layoutPropertiesChange, 'emit');
        spectator.component.group.setValue({
            footer: true,
            header: false,
            sidebar: {
                position: 'left',
                width: '25%',
                containers: []
            }
        });

        expect(changesMock).toHaveBeenCalled();
    });
});
