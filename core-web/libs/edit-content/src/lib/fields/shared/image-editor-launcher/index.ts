export { IMAGE_EDITOR_LAUNCHER } from './image-editor-launcher.token';
// `export type` for the two interfaces: consumers compiled with `isolatedModules` cannot tell
// a type re-export from a value one.
export type { DotImageEditorLauncher, ImageEditorOpenParams } from './image-editor-launcher.token';
export { AngularImageEditorLauncher } from './angular-image-editor.launcher';
