import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { BreadcrumbComponent } from './breadcrumb.component';
import {BreadcrumbHostselectorComponent} from './breadcrumb-hostselector.component';

describe('BreadcrumbHostselectorComponent', () => {
  let component: BreadcrumbHostselectorComponent;
  let fixture: ComponentFixture<BreadcrumbHostselectorComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ BreadcrumbHostselectorComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(BreadcrumbHostselectorComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should be created', () => {
    expect(component).toBeTruthy();
  });
});
