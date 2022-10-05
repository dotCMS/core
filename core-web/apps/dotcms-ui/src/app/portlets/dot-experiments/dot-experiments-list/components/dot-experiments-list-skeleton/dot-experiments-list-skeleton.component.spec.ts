import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DotExperimentsListSkeletonComponent } from './dot-experiments-list-skeleton.component';

describe('DotExperimentsSkeletonComponent', () => {
    let component: DotExperimentsListSkeletonComponent;
    let fixture: ComponentFixture<DotExperimentsListSkeletonComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            declarations: [DotExperimentsListSkeletonComponent]
        }).compileComponents();

        fixture = TestBed.createComponent(DotExperimentsListSkeletonComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
