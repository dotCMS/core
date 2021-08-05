import { ComponentFixture, TestBed } from '@angular/core/testing';

import { SuggestionsListItemComponent } from './suggestions-list-item.component';

describe('SuggestionsListItemComponent', () => {
  let component: SuggestionsListItemComponent;
  let fixture: ComponentFixture<SuggestionsListItemComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ SuggestionsListItemComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(SuggestionsListItemComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
