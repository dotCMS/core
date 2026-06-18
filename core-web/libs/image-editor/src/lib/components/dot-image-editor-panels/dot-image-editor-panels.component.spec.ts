import { byTestId, createComponentFactory, mockProvider, Spectator } from '@ngneat/spectator/jest';
import { MockComponent } from 'ng-mocks';

import { provideNoopAnimations } from '@angular/platform-browser/animations';

import { DotMessageService } from '@dotcms/data-access';

import { DotImageEditorAdjustPanelComponent } from './dot-image-editor-adjust-panel/dot-image-editor-adjust-panel.component';
import { DotImageEditorFileInfoPanelComponent } from './dot-image-editor-fileinfo-panel/dot-image-editor-fileinfo-panel.component';
import { DotImageEditorHistoryPanelComponent } from './dot-image-editor-history-panel/dot-image-editor-history-panel.component';
import { DotImageEditorPanelsComponent } from './dot-image-editor-panels.component';
import { DotImageEditorTransformPanelComponent } from './dot-image-editor-transform-panel/dot-image-editor-transform-panel.component';

describe('DotImageEditorPanelsComponent', () => {
    let spectator: Spectator<DotImageEditorPanelsComponent>;

    const createComponent = createComponentFactory({
        component: DotImageEditorPanelsComponent,
        providers: [
            provideNoopAnimations(),
            mockProvider(DotMessageService, { get: jest.fn((key: string) => key) })
        ],
        // The sub-panels own their own store/dispatch wiring; mock them so this
        // container spec stays isolated to the accordion layout.
        overrideComponents: [
            [
                DotImageEditorPanelsComponent,
                {
                    remove: {
                        imports: [
                            DotImageEditorAdjustPanelComponent,
                            DotImageEditorTransformPanelComponent,
                            DotImageEditorFileInfoPanelComponent,
                            DotImageEditorHistoryPanelComponent
                        ]
                    },
                    add: {
                        imports: [
                            MockComponent(DotImageEditorAdjustPanelComponent),
                            MockComponent(DotImageEditorTransformPanelComponent),
                            MockComponent(DotImageEditorFileInfoPanelComponent),
                            MockComponent(DotImageEditorHistoryPanelComponent)
                        ]
                    }
                }
            ]
        ]
    });

    beforeEach(() => {
        spectator = createComponent();
    });

    it('should render the four accordion sections in order', () => {
        expect(spectator.query(byTestId('image-editor-panel-adjust'))).toExist();
        expect(spectator.query(byTestId('image-editor-panel-transform'))).toExist();
        expect(spectator.query(byTestId('image-editor-panel-fileinfo'))).toExist();
        expect(spectator.query(byTestId('image-editor-panel-history'))).toExist();
    });
});
