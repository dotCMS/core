import { ComponentFixture, TestBed } from '@angular/core/testing';

import { TemplateBuilderActionsComponent } from './template-builder-actions.component';

describe('TemplateBuilderActionsComponent', () => {
    let component: TemplateBuilderActionsComponent;
    let fixture: ComponentFixture<TemplateBuilderActionsComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [TemplateBuilderActionsComponent]
        }).compileComponents();

        fixture = TestBed.createComponent(TemplateBuilderActionsComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
