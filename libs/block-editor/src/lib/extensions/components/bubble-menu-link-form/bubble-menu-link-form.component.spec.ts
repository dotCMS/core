import { ComponentFixture, TestBed } from '@angular/core/testing';

import { BubbleMenuLinkFormComponent } from './bubble-menu-link-form.component';

describe('BubbleMenuLinkFormComponent', () => {
  let component: BubbleMenuLinkFormComponent;
  let fixture: ComponentFixture<BubbleMenuLinkFormComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ BubbleMenuLinkFormComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(BubbleMenuLinkFormComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
