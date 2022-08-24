import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DotExperimentsListComponent } from './dot-experiments-list.component';

describe('ExperimentsListComponent', () => {
    let component: DotExperimentsListComponent;
    let fixture: ComponentFixture<DotExperimentsListComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            declarations: [DotExperimentsListComponent]
        }).compileComponents();

        fixture = TestBed.createComponent(DotExperimentsListComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
