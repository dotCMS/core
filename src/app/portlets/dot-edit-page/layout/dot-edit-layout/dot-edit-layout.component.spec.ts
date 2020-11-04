import { of as observableOf } from 'rxjs';
import { waitForAsync, ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute } from '@angular/router';
import { DotEditLayoutComponent } from './dot-edit-layout.component';
import { Component, DebugElement, Input } from '@angular/core';
import { By } from '@angular/platform-browser';
import { mockDotRenderedPage } from '../../../../test/dot-page-render.mock';
import { DotPageRenderState } from '@portlets/dot-edit-page/shared/models';

@Component({
    selector: 'dot-edit-layout-designer',
    template: ''
})
export class DotEditLayoutDesignerComponentMock {
    @Input()
    pageState: DotPageRenderState;
}

let fixture: ComponentFixture<DotEditLayoutComponent>;

describe('DotEditLayoutComponent', () => {
    let layoutDesigner: DebugElement;

    beforeEach(
        waitForAsync(() => {
            TestBed.configureTestingModule({
                declarations: [DotEditLayoutDesignerComponentMock, DotEditLayoutComponent],
                providers: [
                    {
                        provide: ActivatedRoute,
                        useValue: {
                            parent: {
                                parent: {
                                    data: observableOf({
                                        content: {
                                            ...mockDotRenderedPage()
                                        }
                                    })
                                }
                            }
                        }
                    }
                ]
            });
        })
    );

    beforeEach(() => {
        fixture = TestBed.createComponent(DotEditLayoutComponent);
        fixture.detectChanges();
        layoutDesigner = fixture.debugElement.query(By.css('dot-edit-layout-designer'));
    });

    it('should be 100% min-width in the host', () => {
        // https://github.com/dotCMS/core/issues/19540
        expect(fixture.debugElement.nativeElement.style.minWidth).toBe('100%');
    });

    it('should have dot-edit-layout-designer', () => {
        expect(layoutDesigner).toBeTruthy();
    });

    it('should pass pageState to the dot-edit-layout-designer', () => {
        expect(layoutDesigner.componentInstance.pageState).toEqual(mockDotRenderedPage());
    });
});
