import { async, ComponentFixture, fakeAsync, tick } from '@angular/core/testing';
import { DOTTestBed } from '../../../../test/dot-test-bed';
import { DebugElement, Component} from '@angular/core';
import { ContentTypeFieldsTabComponent } from './';
import { By } from '@angular/platform-browser';
import { ContentTypeField, FieldTab } from '../';
import { IconButtonTooltipModule } from '@components/_common/icon-button-tooltip/icon-button-tooltip.module';
import { DotMessageService } from '@services/dot-messages-service';
import { MockDotMessageService } from '../../../../test/dot-message-service.mock';
import { DotAlertConfirmService } from '@services/dot-alert-confirm';

const tabField: ContentTypeField = {
    clazz: 'tab',
    name: 'fieldTab-1'
};
const mockFieldTab = new FieldTab(tabField);

@Component({
    selector: 'dot-test-host',
    template: '<dot-content-type-fields-tab [fieldTab]="data"></dot-content-type-fields-tab>'
})
class DotTestHostComponent {
    data: FieldTab;

    setData(data: FieldTab): void {
        this.data = data;
    }
}

describe('ContentTypeFieldsTabComponent', () => {
    let hostFixture: ComponentFixture<DotTestHostComponent>;
    let hostDe: DebugElement;
    let hostComp: DotTestHostComponent;

    let comp: ContentTypeFieldsTabComponent;
    let de: DebugElement;
    let dotDialogService: DotAlertConfirmService;

    const messageServiceMock = new MockDotMessageService({
        'contenttypes.action.delete': 'delete text',
        'contenttypes.confirm.message.delete.field': 'delete confirm text',
        'contenttypes.content.field': 'field text',
        'contenttypes.action.cancel': 'cancel text'
    });

    beforeEach(async(() => {
        DOTTestBed.configureTestingModule({
            declarations: [
                ContentTypeFieldsTabComponent,
                DotTestHostComponent
            ],
            imports: [IconButtonTooltipModule],
            providers: [
                DotAlertConfirmService,
                {
                    provide: DotMessageService,
                    useValue: messageServiceMock
                }
            ]
        });

        hostFixture = DOTTestBed.createComponent(DotTestHostComponent);
        hostComp = hostFixture.componentInstance;
        hostDe = hostFixture.debugElement;
        de = hostDe.query(By.css('dot-content-type-fields-tab'));
        comp = de.componentInstance;
        dotDialogService = de.injector.get(DotAlertConfirmService);
    }));

    beforeEach(() => {
        hostComp.setData(mockFieldTab);
        hostFixture.detectChanges();
    });

    it('should render component', () => {
        const deleteBtn = de.query(By.css('dot-icon-button-tooltip')).componentInstance;
        const labelInput = de.query(By.css('input')).nativeElement;

        expect(deleteBtn.tooltipText).toBe('delete text');
        expect(labelInput.outerHTML).toContain(tabField.name);
    });

    it('should emit change evt', fakeAsync(() => {
        spyOn(comp.editTab, 'emit');
        const labelInput = de.query(By.css('input')).nativeElement;
        labelInput.value = 'label changed';
        labelInput.dispatchEvent(new Event('input'));
        tick();
        expect(comp.editTab.emit).toHaveBeenCalledWith(tabField);
    }));

    it('should emit delete evt', () => {
        spyOn(dotDialogService, 'confirm').and.callFake((conf) => {
            conf.accept();
        });
        spyOn(comp.removeTab, 'emit');
        const deleteButton = de.query(By.css('dot-icon-button-tooltip')).nativeElement;
        deleteButton.click();
        expect(comp.removeTab.emit).toHaveBeenCalledWith(mockFieldTab);
    });

});
