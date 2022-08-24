import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ExperimentsUiHeaderComponent } from './experiments-ui-header.component';

xdescribe('ExperimentsHeaderComponent', () => {
    let component: ExperimentsUiHeaderComponent;
    let fixture: ComponentFixture<ExperimentsUiHeaderComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({}).compileComponents();

        fixture = TestBed.createComponent(ExperimentsUiHeaderComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
