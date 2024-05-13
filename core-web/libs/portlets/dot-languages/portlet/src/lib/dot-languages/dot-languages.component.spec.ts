import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DotLanguagesComponent } from './dot-languages.component';

describe('DotLanguagesComponent', () => {
    let component: DotLanguagesComponent;
    let fixture: ComponentFixture<DotLanguagesComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [DotLanguagesComponent]
        }).compileComponents();

        fixture = TestBed.createComponent(DotLanguagesComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
