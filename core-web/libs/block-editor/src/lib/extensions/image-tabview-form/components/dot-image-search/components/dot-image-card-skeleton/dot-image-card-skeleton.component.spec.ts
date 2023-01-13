import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DotImageCardSkeletonComponent } from './dot-image-card-skeleton.component';

describe('DotImageCardSkeletonComponent', () => {
    let component: DotImageCardSkeletonComponent;
    let fixture: ComponentFixture<DotImageCardSkeletonComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            declarations: [DotImageCardSkeletonComponent]
        }).compileComponents();

        fixture = TestBed.createComponent(DotImageCardSkeletonComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
