/**
 * Configure 'debug' (https://github.com/visionmedia/debug)
 * Note that the debug project maintainers have some 'different' ideas about the semantics of enable/disable. Which is probably why disable
 * literally does nothing useful unless it's the ONLY call to debug.
 *
 * So, to 'disable' debug logging, just don't include this file. Magic!
 * Fair warning though: the logging state is stored in local storage, so you do need to call disable to clear it.
 */
import XDebug from 'debug';

XDebug.enable([
  "DC.*"
].join(', '))

export default XDebug

