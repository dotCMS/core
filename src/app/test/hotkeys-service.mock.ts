import { Observable } from 'rxjs/Observable';
import { HotkeysService, Hotkey } from 'angular2-hotkeys';

/**
 * Mock of HotkeysService.
 * How to use:
 *      testHotKeysMock = new TestHotkeysMock();
 *      Call callback method that returns the callback of the keyboard event
 *      testHotKeysMock.callback('key');
 *
 * @export
 * @class TestHotkeysMock
 */

export class TestHotkeysMock {
    private hotkeys = [];

    /**
     * Add hotkey items to hotkeys array and then be able go through them to get combo keys
     * @param hotkey
     * @param specificEvent
     */
    add(hotkey: Hotkey | Hotkey[], specificEvent?: string | string[]): Observable<Hotkey[]> {
        this.hotkeys.push(hotkey);
        return Observable.of([]);
    }

    /**
     * Go through hotkeys items and push hotkey into hotKeyCombo if included in combo param
     * Return one hotkeyCombo or an array of hotkeyCombo
     * @param {string[]} combo
     * @returns {(Hotkey | Hotkey[])}
     * @memberof TestHotkeysMock
     */
    get(combo: string[]): Hotkey | Hotkey[] {
        const hotKeyCombo: Hotkey[] = [];
        this.hotkeys.forEach(hotkey => {
            hotkey.combo.forEach(hotkeyCombo => {
                if (combo.includes(hotkeyCombo)) {
                    hotKeyCombo.push(hotkey);
                }
            });
        });

        return hotKeyCombo.length === 1 ? hotKeyCombo[0] : hotKeyCombo;
    }

    /**
     * Check to see if the method was called
     * Example: spyOn(testHotKeysMock, 'remove');
     * @param {(Hotkey | Hotkey[])} [hotkey]
     * @returns {(Hotkey | Hotkey[])}
     * @memberof TestHotkeysMock
     */
    remove(hotkey?: Hotkey | Hotkey[]): Hotkey | Hotkey[] {
        return null;
    }

    /**
     * Call this method with a hotkey combo to test the key event was called
     * Example: testHotKeysMock.callback(['enter']);
     * @param {string[]} combo
     * @returns {(any|void)}
     * @memberof TestHotkeysMock
     */
    callback(combo: string[]): any|void {
        const hotkey: any = this.get(combo);
        if (hotkey) {
          return hotkey.callback(null, combo);
        }
    }
}
