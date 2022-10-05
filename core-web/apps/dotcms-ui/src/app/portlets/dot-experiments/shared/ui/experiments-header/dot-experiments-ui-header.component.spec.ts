import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DotExperimentsUiHeaderComponent } from './dot-experiments-ui-header.component';

xdescribe('ExperimentsHeaderComponent', () => {
    let component: DotExperimentsUiHeaderComponent;
    let fixture: ComponentFixture<DotExperimentsUiHeaderComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({}).compileComponents();

        fixture = TestBed.createComponent(DotExperimentsUiHeaderComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
