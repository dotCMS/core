import { waitForAsync, ComponentFixture, TestBed } from '@angular/core/testing';

import { DotAutocompleteTagsComponent } from './dot-autocomplete-tags.component';

describe('DotAutocompleteTagsComponent', () => {
    let component: DotAutocompleteTagsComponent;
    let fixture: ComponentFixture<DotAutocompleteTagsComponent>;

    beforeEach(waitForAsync(() => {
        TestBed.configureTestingModule({
            declarations: [DotAutocompleteTagsComponent]
        }).compileComponents();
    }));

    beforeEach(() => {
        fixture = TestBed.createComponent(DotAutocompleteTagsComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
