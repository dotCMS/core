import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DotResultsSeoToolComponent } from './dot-results-seo-tool.component';

describe('DotResultsSeoToolComponent', () => {
    let component: DotResultsSeoToolComponent;
    let fixture: ComponentFixture<DotResultsSeoToolComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [DotResultsSeoToolComponent]
        }).compileComponents();

        fixture = TestBed.createComponent(DotResultsSeoToolComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
