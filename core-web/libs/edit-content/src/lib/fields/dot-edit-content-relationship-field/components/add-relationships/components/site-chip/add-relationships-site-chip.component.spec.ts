import { describe, expect } from '@jest/globals';
import { Spectator, createComponentFactory, mockProvider } from '@openng/spectator/jest';
import { MockComponent } from 'ng-mocks';

import { AddRelationshipsSiteChipComponent } from './add-relationships-site-chip.component';

import { DotHostFolderFieldComponent } from '../../../../../dot-edit-content-host-folder-field/components/host-folder-field/host-folder-field.component';
import { AddRelationshipsStore } from '../../store/add-relationships.store';

/**
 * T105 — the scope chip must change the **search**, not just its own label.
 *
 * The first version wrote `site` through `DOT_FILTER_FACADE`, which the request builder never
 * reads, so the control moved a value nobody consumed and the results never changed. It also
 * hand-rolled a folder browser; this one delegates to the shared `dot-host-folder-field`, whose
 * value is `hostname:/path/`.
 */
describe('AddRelationshipsSiteChipComponent', () => {
    let spectator: Spectator<AddRelationshipsSiteChipComponent>;
    const setScope = jest.fn();

    const createComponent = createComponentFactory({
        component: AddRelationshipsSiteChipComponent,
        providers: [
            mockProvider(AddRelationshipsStore, {
                scopeLabel: () => 'demo.dotcms.com',
                assetPath: () => '//demo.dotcms.com/',
                setScope
            })
        ],
        // The shared browser is stubbed: this spec is about the scope the chip reports, and
        // mounting the real one drags in its whole store and service chain for nothing.
        overrideComponents: [
            [
                AddRelationshipsSiteChipComponent,
                {
                    remove: { imports: [DotHostFolderFieldComponent] },
                    add: { imports: [MockComponent(DotHostFolderFieldComponent)] }
                }
            ]
        ]
    });

    beforeEach(() => {
        setScope.mockClear();
        spectator = createComponent();
        spectator.detectChanges();
    });

    /**
     * Seeded in the shared browser's own `hostname:/path/` vocabulary, not in the chip's display
     * text: `writeValue` feeds that straight into `loadSites`, so any other shape opens an overlay
     * with no sites in it.
     */
    it('opens on the site currently being searched, in the browser vocabulary', () => {
        expect(spectator.component['scopeControl'].value).toBe('demo.dotcms.com:/');
    });

    it('re-scopes the search when a site is chosen', () => {
        spectator.component['scopeControl'].setValue('other.dotcms.com:/');

        expect(setScope).toHaveBeenCalledWith({ hostname: 'other.dotcms.com', path: undefined });
    });

    it('re-scopes to a folder when one is chosen', () => {
        spectator.component['scopeControl'].setValue('demo.dotcms.com:/blog/');

        expect(setScope).toHaveBeenCalledWith({
            hostname: 'demo.dotcms.com',
            path: '/blog/'
        });
    });

    it('ignores a value that carries no host', () => {
        spectator.component['scopeControl'].setValue(null);

        expect(setScope).not.toHaveBeenCalled();
    });
});
