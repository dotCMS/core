import { ComponentFixture, TestBed } from '@angular/core/testing';
import {
    DotContentCompareComponent,
    DotContentCompareEvent
} from './dot-content-compare.component';
import { DotContentCompareStore } from '@components/dot-content-compare/store/dot-content-compare.store';
import { Component, DebugElement, Input } from '@angular/core';
import { of } from 'rxjs';
import { dotContentCompareTableDataMock } from '@components/dot-content-compare/components/dot-content-compare-table/dot-content-compare-table.component.spec';
import { MockDotMessageService } from '@tests/dot-message-service.mock';
import { DotMessageService } from '@services/dot-message/dot-messages.service';

import { DotContentCompareModule } from '@components/dot-content-compare/dot-content-compare.module';
import { DotContentCompareTableComponent } from '@components/dot-content-compare/components/dot-content-compare-table/dot-content-compare-table.component';
import { By } from '@angular/platform-browser';
import { DotCMSContentlet } from '@dotcms/dotcms-models';

const DotContentCompareEventMOCK = {
    inode: '1',
    identifier: '2',
    language: 'es'
};

const storeMock = jasmine.createSpyObj(
    'DotContentCompareStore',
    ['loadData', 'updateShowDiff', 'updateCompare'],
    {
        vm$: of({ data: dotContentCompareTableDataMock, showDiff: false })
    }
);

@Component({
    selector: 'dot-test-host-component',
    template: '<dot-content-compare [data]="data" ></dot-content-compare>'
})
class TestHostComponent {
    @Input() data: DotContentCompareEvent;
}

describe('DotContentCompareComponent', () => {
    let hostComponent: TestHostComponent;
    let hostFixture: ComponentFixture<TestHostComponent>;
    let de: DebugElement;
    let dotContentCompareStore: DotContentCompareStore;
    let contentCompareTableComponent: DotContentCompareTableComponent;
    const messageServiceMock = new MockDotMessageService({});

    beforeEach(() => {
        TestBed.configureTestingModule({
            declarations: [DotContentCompareComponent, TestHostComponent],
            imports: [DotContentCompareModule],
            providers: [{ provide: DotMessageService, useValue: messageServiceMock }]
        });
        TestBed.overrideProvider(DotContentCompareStore, { useValue: storeMock });

        hostFixture = TestBed.createComponent(TestHostComponent);
        de = hostFixture.debugElement;
        dotContentCompareStore = TestBed.inject(DotContentCompareStore);
        hostComponent = hostFixture.componentInstance;
        hostComponent.data = DotContentCompareEventMOCK;
        hostFixture.detectChanges();
        contentCompareTableComponent = de.query(By.css('dot-content-compare-table'))
            .componentInstance;
    });

    it('should pass data correctly', () => {
        expect(dotContentCompareStore.loadData).toHaveBeenCalledWith(DotContentCompareEventMOCK);
        expect(contentCompareTableComponent.data).toEqual(dotContentCompareTableDataMock);
        expect(contentCompareTableComponent.showDiff).toEqual(false);
    });

    it('should update diff flag', () => {
        contentCompareTableComponent.changeDiff.emit(true);
        expect(dotContentCompareStore.updateShowDiff).toHaveBeenCalledOnceWith(true);
    });

    it('should update compare content', () => {
        contentCompareTableComponent.changeVersion.emit(('value' as unknown) as DotCMSContentlet);
        expect(dotContentCompareStore.updateCompare).toHaveBeenCalledOnceWith(
            ('value' as unknown) as DotCMSContentlet
        );
    });
});
