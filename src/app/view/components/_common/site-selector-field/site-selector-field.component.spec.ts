import { By } from '@angular/platform-browser';
import { DOTTestBed } from '../../../../test/dot-test-bed';
import { DebugElement, OnInit, Component } from '@angular/core';
import { SiteSelectorFieldComponent } from './site-selector-field.component';
import { SiteSelectorModule } from '../site-selector/site-selector.module';
import { SiteService } from 'dotcms-js/dotcms-js';
import { SiteServiceMock } from '../../../../test/site-service.mock';
import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import { FormGroup, FormBuilder } from '@angular/forms';
import { PaginatorService } from '../../../../api/services/paginator/index';
import { SiteSelectorFieldModule } from './site-selector-field.module';

@Component({
    selector: 'dot-fake-form',
    template: `
        <form [formGroup]="form">
            <dot-site-selector-field formControlName="site"></dot-site-selector-field>
            {{form.value | json}}
        </form>
    `
})
class FakeFormComponent {
    form: FormGroup;

    constructor(private fb: FormBuilder) {
        /*
            This should go in the ngOnInit but I don't want to detectChanges everytime for
            this fake test component
        */
        this.form = this.fb.group({
            site: ''
        });
    }
}

describe('SiteSelectorFieldComponent', () => {
    let component: FakeFormComponent;
    let fixture: ComponentFixture<FakeFormComponent>;
    let de: DebugElement;
    const siteServiceMock = new SiteServiceMock();

    beforeEach(
        async(() => {
            siteServiceMock.setFakeCurrentSite();

            DOTTestBed.configureTestingModule({
                declarations: [FakeFormComponent],
                imports: [SiteSelectorFieldModule, SiteSelectorModule],
                providers: [{ provide: SiteService, useValue: siteServiceMock }]
            });
        })
    );

    beforeEach(() => {
        fixture = TestBed.createComponent(FakeFormComponent);
        component = fixture.componentInstance;
        de = fixture.debugElement;
        fixture.detectChanges();
    });

    it('should have a site selector component', () => {
        const siteSelector = de.query(By.css('dot-site-selector'));
        expect(siteSelector).not.toBe(null);
    });

    it('should have current site as default value', () => {
        siteServiceMock.setFakeCurrentSite();
        fixture.detectChanges();

        expect(component.form.value).toEqual({
            site: '123-xyz-567-xxl'
        });
    });

    it('should update the value when current site change', () => {
        siteServiceMock.setFakeCurrentSite();
        fixture.detectChanges();

        siteServiceMock.setFakeCurrentSite({
            identifier: 'new-identifier',
            hostname: 'hello.word.com',
            type: 'xyz'
        });

        expect(component.form.value).toEqual({
            site: 'new-identifier'
        });
    });

    it('should not update the value when current site change if already have a value', () => {
        component.form.setValue({
            site: 'abc'
        });
        fixture.detectChanges();

        siteServiceMock.setFakeCurrentSite();
        fixture.detectChanges();

        expect(component.form.value).toEqual({
            site: 'abc'
        });
    });


    it('should have undefined params by default', () => {
        const siteSelector = de.query(By.css('dot-site-selector'));

        expect(siteSelector.componentInstance.archive).toBeUndefined();
        expect(siteSelector.componentInstance.system).toBeUndefined();
        expect(siteSelector.componentInstance.live).toBeUndefined();
    });

    it('should bind params correctly', () => {
        const siteSelectorField: SiteSelectorFieldComponent = de.query(By.css('dot-site-selector-field'))
            .componentInstance;

        siteSelectorField.archive = true;
        siteSelectorField.system = false;
        siteSelectorField.live = false;

        fixture.detectChanges();

        const siteSelector = de.query(By.css('dot-site-selector'));

        expect(siteSelector.componentInstance.archive).toBe(true);
        expect(siteSelector.componentInstance.system).toBe(false);
        expect(siteSelector.componentInstance.live).toBe(false);
    });
});
