import { createComponentFactory, Spectator } from '@ngneat/spectator/jest';

import { DynamicDialogConfig } from 'primeng/dynamicdialog';

import { DotLegacyImageEditorDialogComponent } from './dot-legacy-image-editor-dialog.component';

describe('DotLegacyImageEditorDialogComponent', () => {
    let spectator: Spectator<DotLegacyImageEditorDialogComponent>;

    const createComponent = createComponentFactory({
        component: DotLegacyImageEditorDialogComponent,
        providers: [
            {
                provide: DynamicDialogConfig,
                useValue: {
                    data: {
                        inode: 'inode-1',
                        tempId: 'temp-1',
                        variable: 'binaryField'
                    }
                }
            }
        ]
    });

    beforeEach(() => {
        spectator = createComponent();
    });

    it('should build iframe src with inode, tempId, and variable params', () => {
        const iframe = spectator.query('[data-testid="legacy-image-editor-iframe"]');

        expect(iframe?.getAttribute('src')).toBe(
            '/html/js/dotcms/dijit/image/image-editor-standalone.jsp?inode=inode-1&tempId=temp-1&fieldName=binaryField&variable=binaryField'
        );
    });
});
