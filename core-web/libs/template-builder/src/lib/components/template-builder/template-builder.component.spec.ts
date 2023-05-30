import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DotTemplateBuilderStore } from './store/template-builder.store';
import { TemplateBuilderComponent } from './template-builder.component';
import { FULL_DATA_MOCK } from './utils/mocks';

describe('TemplateBuilderComponent', () => {
    let component: TemplateBuilderComponent;
    let fixture: ComponentFixture<TemplateBuilderComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            declarations: [TemplateBuilderComponent],
            providers: [DotTemplateBuilderStore]
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
