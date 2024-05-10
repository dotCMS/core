import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DotLanguagesListComponent } from './dot-languages-list.component';

describe('DotLanguagesListComponent', () => {
    let component: DotLanguagesListComponent;
    let fixture: ComponentFixture<DotLanguagesListComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [DotLanguagesListComponent]
        }).compileComponents();

        fixture = TestBed.createComponent(DotLanguagesListComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
