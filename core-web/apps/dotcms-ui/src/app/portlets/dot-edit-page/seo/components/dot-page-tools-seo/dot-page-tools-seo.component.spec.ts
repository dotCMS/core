import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DotPageToolsSeoComponent } from './dot-page-tools-seo.component';

describe('DotPageToolsSeoComponent', () => {
    let component: DotPageToolsSeoComponent;
    let fixture: ComponentFixture<DotPageToolsSeoComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [DotPageToolsSeoComponent]
        }).compileComponents();

        fixture = TestBed.createComponent(DotPageToolsSeoComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
