import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DotcmsLayoutComponent } from './dotcms-layout.component';

describe('DotcmsLayoutComponent', () => {
    let component: DotcmsLayoutComponent;
    let fixture: ComponentFixture<DotcmsLayoutComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [DotcmsLayoutComponent]
        }).compileComponents();

        fixture = TestBed.createComponent(DotcmsLayoutComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
