import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DotExperimentsPublishVariantComponent } from './dot-experiments-publish-variant.component';

describe('DotExperimentsPublishVariantComponent', () => {
    let component: DotExperimentsPublishVariantComponent;
    let fixture: ComponentFixture<DotExperimentsPublishVariantComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            declarations: [DotExperimentsPublishVariantComponent]
        }).compileComponents();

        fixture = TestBed.createComponent(DotExperimentsPublishVariantComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
