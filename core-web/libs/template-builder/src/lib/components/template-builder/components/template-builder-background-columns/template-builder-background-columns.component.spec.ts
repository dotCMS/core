import { ComponentFixture, TestBed } from '@angular/core/testing';

import { TemplateBuilderBackgroundColumnsComponent } from './template-builder-background-columns.component';

describe('TemplateBuilderBackgroundColumnsComponent', () => {
    let component: TemplateBuilderBackgroundColumnsComponent;
    let fixture: ComponentFixture<TemplateBuilderBackgroundColumnsComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            declarations: [TemplateBuilderBackgroundColumnsComponent]
        }).compileComponents();

        fixture = TestBed.createComponent(TemplateBuilderBackgroundColumnsComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
