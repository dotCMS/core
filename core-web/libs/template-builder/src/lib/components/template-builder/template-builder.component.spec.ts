import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DotMessageService } from '@dotcms/data-access';
import { DotMessagePipeModule } from '@dotcms/ui';

import { AddWidgetComponent } from './components/add-widget/add-widget.component';
import { TemplateBuilderRowComponent } from './components/template-builder-row/template-builder-row.component';
import { DotTemplateBuilderStore } from './store/template-builder.store';
import { TemplateBuilderComponent } from './template-builder.component';
import { FULL_DATA_MOCK, MESSAGES_MOCK } from './utils/mocks';

describe('TemplateBuilderComponent', () => {
    let component: TemplateBuilderComponent;
    let fixture: ComponentFixture<TemplateBuilderComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            declarations: [TemplateBuilderComponent],
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
            ],
            imports: [AddWidgetComponent, TemplateBuilderRowComponent, DotMessagePipeModule]
        }).compileComponents();

        fixture = TestBed.createComponent(TemplateBuilderComponent);
        component = fixture.componentInstance;

        component.templateLayout = {
            body: FULL_DATA_MOCK,
            footer: false,
            header: false,
            sidebar: {},
            title: '',
            width: ''
        };

        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
