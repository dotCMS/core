import { ComponentFixture, TestBed } from '@angular/core/testing';

import { SuggestionLoadingItemComponent } from './suggestion-loading-item.component';

describe('SuggestionSkeletonComponent', () => {
  let component: SuggestionLoadingItemComponent;
  let fixture: ComponentFixture<SuggestionLoadingItemComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ SuggestionLoadingItemComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(SuggestionLoadingItemComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
