import { ComponentFixture, TestBed } from '@angular/core/testing';

import { TemplateBuilderThemeSelectorComponent } from './template-builder-theme-selector.component';

describe('TemplateBuilderThemeSelectorComponent', () => {
    let component: TemplateBuilderThemeSelectorComponent;
    let fixture: ComponentFixture<TemplateBuilderThemeSelectorComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [TemplateBuilderThemeSelectorComponent]
        }).compileComponents();

        fixture = TestBed.createComponent(TemplateBuilderThemeSelectorComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
