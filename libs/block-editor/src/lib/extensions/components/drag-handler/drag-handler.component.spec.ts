import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DragHandlerComponent } from './drag-handler.component';

describe('DragHandlerComponent', () => {
  let component: DragHandlerComponent;
  let fixture: ComponentFixture<DragHandlerComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ DragHandlerComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(DragHandlerComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
