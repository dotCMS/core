import { ComponentFixture, TestBed } from '@angular/core/testing';

import { TemplateBuilderRowComponent } from './template-builder-row.component';

describe('TemplateBuilderRowComponent', () => {
    let component: TemplateBuilderRowComponent;
    let fixture: ComponentFixture<TemplateBuilderRowComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [TemplateBuilderRowComponent]
        }).compileComponents();

        fixture = TestBed.createComponent(TemplateBuilderRowComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
