import { Spectator, createComponentFactory } from '@ngneat/spectator';

import { DotMessageService } from '@dotcms/data-access';
import { DotMessagePipeModule } from '@dotcms/ui';

import { AddWidgetComponent } from './components/add-widget/add-widget.component';
import { TemplateBuilderRowComponent } from './components/template-builder-row/template-builder-row.component';
import { DotTemplateBuilderStore } from './store/template-builder.store';
import { TemplateBuilderComponent } from './template-builder.component';
import { FULL_DATA_MOCK, MESSAGES_MOCK } from './utils/mocks';

describe('TemplateBuilderComponent', () => {
    let spectator: Spectator<TemplateBuilderComponent>;
    const createComponent = createComponentFactory({
        component: TemplateBuilderComponent,
        imports: [
            AddWidgetComponent,
            TemplateBuilderRowComponent,
            TemplateBuilderRowComponent,
            DotMessagePipeModule
        ],
        providers: [
            DotTemplateBuilderStore,
            {
                provide: DotMessageService,
                useValue: {
                    get(key: string, ..._args: string[]): string {
                        return MESSAGES_MOCK[key];
                    },
                    init() {
                        /* */
                    }
                }
            }
        ]
    });

    beforeEach(() => {
        spectator = createComponent({
            props: {
                templateLayout: {
                    body: FULL_DATA_MOCK,
                    footer: false,
                    header: false,
                    sidebar: {},
                    title: '',
                    width: ''
                }
            }
        });
    });

    it('should call deleteRow', () => {
        const deleteRowMock = jest.spyOn(spectator.component, 'deleteRow');
        spectator.component.deleteRow('123');
        expect(deleteRowMock).toHaveBeenCalledWith('123');
    });
});
