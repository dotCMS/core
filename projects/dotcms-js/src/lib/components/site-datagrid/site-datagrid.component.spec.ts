import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { SiteDatagridComponent } from './site-datagrid.component';

describe('DataGridComponent', () => {
    let component: SiteDatagridComponent;
    let fixture: ComponentFixture<SiteDatagridComponent>;

    beforeEach(async(() => {
        TestBed.configureTestingModule({
            declarations: [SiteDatagridComponent]
        }).compileComponents();
    }));

    beforeEach(() => {
        fixture = TestBed.createComponent(SiteDatagridComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should be created', () => {
        expect(component).toBeTruthy();
    });
});
