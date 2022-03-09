import { ComponentFixture, TestBed } from '@angular/core/testing';

import { BubbleChangeDropdownComponent } from './bubble-change-dropdown.component';

describe('BubbleChangeDropdownComponent', () => {
  let component: BubbleChangeDropdownComponent;
  let fixture: ComponentFixture<BubbleChangeDropdownComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ BubbleChangeDropdownComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(BubbleChangeDropdownComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
