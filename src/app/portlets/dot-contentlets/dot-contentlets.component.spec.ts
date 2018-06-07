import { DOTTestBed } from '../../test/dot-test-bed';
import { Injectable, DebugElement } from '@angular/core';
import { DotNavigationService } from '../../view/components/dot-navigation/dot-navigation.service';
import { ActivatedRoute } from '@angular/router';
import { DotContentletsComponent } from './dot-contentlets.component';
import { DotEditContentToolbarHtmlService } from '../dot-edit-page/content/services/html/dot-edit-content-toolbar-html.service';
import { DotContentletEditorService } from '../../view/components/dot-contentlet-editor/services/dot-contentlet-editor.service';
import { ComponentFixture } from '@angular/core/testing';

@Injectable()
class MockDotNavigationService {
    goToFirstPortlet = jasmine.createSpy('goToFirstPortlet');
}

@Injectable()
class MockDotContentletEditorService {
    edit = jasmine.createSpy('edit');
}

describe('DotContentletsComponent', () => {

    let fixture: ComponentFixture<DotContentletsComponent>;
    let de: DebugElement;

    let dotNavigationService: DotNavigationService;
    let dotContentletEditorService: DotContentletEditorService;

    beforeEach(() => {
        DOTTestBed.configureTestingModule({
            declarations: [DotContentletsComponent],
            providers: [
                DotContentletEditorService,
                {
                    provide: ActivatedRoute,
                    useValue: {
                        snapshot: {
                            params: {
                                inode: '5cd3b647-e465-4a6d-a78b-e834a7a7331a'
                            }
                        }
                    }
                },
                {
                    provide: DotContentletEditorService,
                    useClass: MockDotContentletEditorService
                },
                {
                    provide: DotNavigationService,
                    useClass: MockDotNavigationService
                }
            ]
        });

        fixture = DOTTestBed.createComponent(DotContentletsComponent);
        de = fixture.debugElement;
        dotNavigationService = de.injector.get(DotNavigationService);
        dotContentletEditorService = de.injector.get(DotContentletEditorService);
    });

    it('should call first portlet & contentlet modal', () => {
        fixture.detectChanges();

        const params = {
            data: {
                inode: '5cd3b647-e465-4a6d-a78b-e834a7a7331a'
            }
        };

        expect(dotNavigationService.goToFirstPortlet).toHaveBeenCalled();
        expect(dotContentletEditorService.edit).toHaveBeenCalledWith(params);
    });
});
