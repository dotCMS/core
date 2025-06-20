import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DotContentDriveShellComponent } from './dot-content-drive-shell.component';

describe('DotContentDriveShellComponent', () => {
    let component: DotContentDriveShellComponent;
    let fixture: ComponentFixture<DotContentDriveShellComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [DotContentDriveShellComponent]
        }).compileComponents();

        fixture = TestBed.createComponent(DotContentDriveShellComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
