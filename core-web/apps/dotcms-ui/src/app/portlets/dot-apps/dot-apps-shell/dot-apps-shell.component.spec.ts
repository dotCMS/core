import { ComponentFixture, TestBed } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';

import { DotAppsShellComponent } from './dot-apps-shell.component';

describe('DotAppsShellComponent', () => {
    let component: DotAppsShellComponent;
    let fixture: ComponentFixture<DotAppsShellComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [DotAppsShellComponent, RouterTestingModule]
        }).compileComponents();

        fixture = TestBed.createComponent(DotAppsShellComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });

    it('should have router-outlet', () => {
        const routerOutlet = fixture.nativeElement.querySelector('router-outlet');
        expect(routerOutlet).toBeTruthy();
    });

    it('should have import-export dialog', () => {
        const dialog = fixture.nativeElement.querySelector('dot-apps-import-export-dialog');
        expect(dialog).toBeTruthy();
    });

    it('should have host styled with full height', () => {
        const hostStyles = getComputedStyle(fixture.nativeElement);
        expect(hostStyles.display).toBe('block');
        expect(hostStyles.height).toBe('100%');
    });
});
