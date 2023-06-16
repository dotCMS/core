import { ComponentFixture, TestBed } from '@angular/core/testing';

import { TemplateBuilderSidebarComponent } from './template-builder-sidebar.component';

describe('TemplateBuilderSidebarComponent', () => {
    let component: TemplateBuilderSidebarComponent;
    let fixture: ComponentFixture<TemplateBuilderSidebarComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [TemplateBuilderSidebarComponent]
        }).compileComponents();

        fixture = TestBed.createComponent(TemplateBuilderSidebarComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
