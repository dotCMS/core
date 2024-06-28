import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DotLocaleVariablesComponent } from './dot-locale-variables.component';

describe('DotLocaleVariablesComponent', () => {
    let component: DotLocaleVariablesComponent;
    let fixture: ComponentFixture<DotLocaleVariablesComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [DotLocaleVariablesComponent]
        }).compileComponents();

        fixture = TestBed.createComponent(DotLocaleVariablesComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
