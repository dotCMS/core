import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DotTemplateBuilderComponent } from './dot-template-builder.component';

describe('DotTemplateBuilderComponent', () => {
    let component: DotTemplateBuilderComponent;
    let fixture: ComponentFixture<DotTemplateBuilderComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            declarations: [DotTemplateBuilderComponent]
        }).compileComponents();

        fixture = TestBed.createComponent(DotTemplateBuilderComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
