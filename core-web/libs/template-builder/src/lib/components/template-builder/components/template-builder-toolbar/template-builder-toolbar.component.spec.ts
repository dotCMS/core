import { ComponentFixture, TestBed } from '@angular/core/testing';

import { TemplateBuilderToolbarComponent } from './template-builder-toolbar.component';

describe('TemplateBuilderToolbarComponent', () => {
    let component: TemplateBuilderToolbarComponent;
    let fixture: ComponentFixture<TemplateBuilderToolbarComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [TemplateBuilderToolbarComponent]
        }).compileComponents();

        fixture = TestBed.createComponent(TemplateBuilderToolbarComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
