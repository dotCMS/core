import { ComponentFixture, TestBed } from '@angular/core/testing';

import { TemplateBuilderBoxComponent } from './template-builder-box.component';

describe('TemplateBuilderBoxComponent', () => {
    let component: TemplateBuilderBoxComponent;
    let fixture: ComponentFixture<TemplateBuilderBoxComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            declarations: [TemplateBuilderBoxComponent]
        }).compileComponents();

        fixture = TestBed.createComponent(TemplateBuilderBoxComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
