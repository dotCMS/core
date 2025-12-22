import { byTestId, createComponentFactory, Spectator } from '@ngneat/spectator/jest';
import { of } from 'rxjs';

import { HttpClientTestingModule } from '@angular/common/http/testing';

import { DotMessageService, DotSystemConfigService } from '@dotcms/data-access';
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
            {
                provide: DotMessageService,
                useValue: DOT_MESSAGE_SERVICE_TB_MOCK
            },
            {
                provide: DotSystemConfigService,
                useValue: { getSystemConfig: () => of({}) }
            },
            DotTemplateBuilderStore
        ],
        imports: [HttpClientTestingModule, DotMessagePipe]
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

        spectator.component.onThemeChange('test-theme-id');

        expect(spy).toHaveBeenCalledWith('test-theme-id');
    });

    it('should open an overlayPanel event when layout button is clicked', () => {
        spectator.detectChanges();
        const btnSelectLayout = spectator.query(byTestId('btn-select-layout'));
        const actualButton = btnSelectLayout?.querySelector('button');
        if (actualButton) {
            spectator.click(actualButton);
        }
        spectator.detectChanges();

        // The component uses p-popover, not p-overlaypanel
        expect(spectator.query('p-popover')).toBeTruthy();
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
