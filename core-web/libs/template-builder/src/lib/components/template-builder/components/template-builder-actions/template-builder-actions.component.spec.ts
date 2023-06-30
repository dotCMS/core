import { Spectator, byTestId, createComponentFactory } from '@ngneat/spectator/jest';

import { HttpClientTestingModule } from '@angular/common/http/testing';

import { DotMessageService } from '@dotcms/data-access';
import { DotMessagePipe } from '@dotcms/ui';

import { TemplateBuilderActionsComponent } from './template-builder-actions.component';

import { DotTemplateBuilderStore } from '../../store/template-builder.store';
import { DOT_MESSAGE_SERVICE_TB_MOCK } from '../../utils/mocks';

describe('TemplateBuilderActionsComponent', () => {
    let spectator: Spectator<TemplateBuilderActionsComponent>;
    let store: DotTemplateBuilderStore;
    const createComponent = createComponentFactory({
        component: TemplateBuilderActionsComponent,
        providers: [
            DotMessagePipe,
            {
                provide: DotMessageService,
                useValue: DOT_MESSAGE_SERVICE_TB_MOCK
            },
            DotTemplateBuilderStore
        ],
        imports: [HttpClientTestingModule]
    });

    beforeEach(() => {
        spectator = createComponent({
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
        });

        store = spectator.inject(DotTemplateBuilderStore);
    });

    it('should emit selectTheme event when style button is clicked', () => {
        const spy = jest.spyOn(spectator.component.selectTheme, 'emit');
        spectator.detectChanges();
        const btnSelectStyles = spectator.query(byTestId('btn-select-theme'));
        spectator.dispatchMouseEvent(btnSelectStyles, 'click');

        expect(spy).toHaveBeenCalled();
    });

    it('should open an overlayPanel event when layout button is clicked', () => {
        spectator.detectChanges();
        const btnSelectStyles = spectator.query(byTestId('btn-select-layout'));
        spectator.dispatchMouseEvent(btnSelectStyles, 'click');

        expect(spectator.query('p-overlaypanel')).toBeTruthy();
    });

    it('should emit changes everytime the layout properties changes', () => {
        const changesMock = jest.spyOn(store, 'updateLayoutProperties');
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
