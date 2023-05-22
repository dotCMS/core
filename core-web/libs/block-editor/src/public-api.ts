/*
 * Public API Surface of ngx-tiptap
 */
export * from './lib/block-editor.module';
export * from './lib/AngularRenderer';
export * from './lib/NodeViewRenderer';

export { getEditorBlockOptions } from './lib/shared/utils/suggestion.utils';

//Editor
export * from './lib/components/dot-block-editor/dot-block-editor.component';

// Editor Extensions
export * from './lib/extensions';

// Editor Nodes
export * from './lib/nodes';

// Shared
export * from './lib/shared';
