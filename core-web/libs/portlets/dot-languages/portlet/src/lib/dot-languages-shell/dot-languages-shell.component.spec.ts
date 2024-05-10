import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DotLanguagesShellComponent } from './dot-languages-shell.component';

describe('DotLanguagesShellComponent', () => {
    let component: DotLanguagesShellComponent;
    let fixture: ComponentFixture<DotLanguagesShellComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [DotLanguagesShellComponent]
        }).compileComponents();

        fixture = TestBed.createComponent(DotLanguagesShellComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
