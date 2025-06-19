import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DotListViewComponent } from './dot-list-view.component';

describe('DotListViewComponent', () => {
    let component: DotListViewComponent;
    let fixture: ComponentFixture<DotListViewComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [DotListViewComponent]
        }).compileComponents();

        fixture = TestBed.createComponent(DotListViewComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
