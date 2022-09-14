import { By } from '@angular/platform-browser';
import { DOTTestBed } from '../../../../test/dot-test-bed';
import { DebugElement, Component, Input } from '@angular/core';
import { DotSiteSelectorFieldComponent } from './dot-site-selector-field.component';
import { SiteService } from '@dotcms/dotcms-js';
import { SiteServiceMock } from '../../../../test/site-service.mock';
import { waitForAsync, ComponentFixture, TestBed } from '@angular/core/testing';
import { UntypedFormGroup, UntypedFormBuilder } from '@angular/forms';

@Component({
    selector: 'dot-fake-form',
    template: `
        <form [formGroup]="form">
            <dot-site-selector-field formControlName="site"></dot-site-selector-field>
            {{ form.value | json }}
        </form>
    `
})
class FakeFormComponent {
    form: UntypedFormGroup;

    constructor(private fb: UntypedFormBuilder) {
        /*
            This should go in the ngOnInit but I don't want to detectChanges everytime for
            this fake test component
        */
        this.form = this.fb.group({
            site: ''
        });
    }
}

@Component({
    selector: 'dot-site-selector',
    template: ''
})
export class SiteSelectorComponent {
    @Input()
    archive: boolean;
    @Input()
    id: string;
    @Input()
    live: boolean;
    @Input()
    system: boolean;
    @Input()
    asField: boolean;
}

describe('SiteSelectorFieldComponent', () => {
    let component: FakeFormComponent;
    let fixture: ComponentFixture<FakeFormComponent>;
    let de: DebugElement;
    const siteServiceMock = new SiteServiceMock();

    beforeEach(
        waitForAsync(() => {
            siteServiceMock.setFakeCurrentSite();

            DOTTestBed.configureTestingModule({
                declarations: [
                    FakeFormComponent,
                    SiteSelectorComponent,
                    DotSiteSelectorFieldComponent
                ],
                imports: [],
                providers: [{ provide: SiteService, useValue: siteServiceMock }]
            });
        })
    );

    beforeEach(() => {
        fixture = TestBed.createComponent(FakeFormComponent);
        component = fixture.componentInstance;
        de = fixture.debugElement;
    });

    it('should have a site selector component', () => {
        const siteSelector = de.query(By.css('dot-site-selector'));
        expect(siteSelector).not.toBe(null);
    });

    it('should have current site as default value', () => {
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
            type: 'xyz',
            archived: false
        });

        expect(component.form.value).toEqual({
            site: 'new-identifier'
        });
    });

    it('should not update the value when current site change if already have a value', () => {
        fixture.detectChanges();
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
        const siteSelectorField: DotSiteSelectorFieldComponent = de.query(
            By.css('dot-site-selector-field')
        ).componentInstance;

        siteSelectorField.archive = true;
        siteSelectorField.system = false;
        siteSelectorField.live = false;

        fixture.detectChanges();

        const siteSelector = de.query(By.css('dot-site-selector'));

        expect(siteSelector.componentInstance.archive).toBe(true);
        expect(siteSelector.componentInstance.system).toBe(false);
        expect(siteSelector.componentInstance.live).toBe(false);
        expect(siteSelector.componentInstance.asField).toBe(true);
    });

    it('should not set current site when already hava a value', () => {
        component.form.get('site').setValue('1234');
        fixture.detectChanges();

        const siteSelectorField: DotSiteSelectorFieldComponent = de.query(
            By.css('dot-site-selector-field')
        ).componentInstance;

        expect(siteSelectorField.value).toEqual('1234');
    });

    it('should not set current site when already hava a value and current site request is getting after onInit', () => {
        spyOnProperty(siteServiceMock, 'currentSite', 'get').and.returnValue(null);
        component.form.get('site').setValue('1234');
        fixture.detectChanges();

        const siteSelectorField: DotSiteSelectorFieldComponent = de.query(
            By.css('dot-site-selector-field')
        ).componentInstance;

        expect(siteSelectorField.value).toEqual('1234');
    });
});
