import { ChangeDetectionStrategy, Component } from '@angular/core';

/**
 * Internal semantic block components for the accessible Block Editor renderer.
 *
 * Each component uses an **attribute selector** so the host element *is* the real
 * semantic tag (`<ul>`, `<ol>`, `<li>`, ...). The template is just `<ng-content />`,
 * so no extra wrapper element is added to the DOM and the
 * `list → listitem` relationship required by the HTML spec and assistive
 * technology is preserved.
 *
 * These are not exported from the SDK; they are an implementation detail of
 * {@link DotCMSBlockEditorRendererNativeComponent}.
 *
 * @internal
 */
@Component({
    selector: 'ul[dotBulletList]',
    changeDetection: ChangeDetectionStrategy.OnPush,
    template: `
        <ng-content />
    `
})
export class DotSemanticBulletList {}

@Component({
    selector: 'ol[dotOrderedList]',
    changeDetection: ChangeDetectionStrategy.OnPush,
    template: `
        <ng-content />
    `
})
export class DotSemanticOrderedList {}

@Component({
    selector: 'li[dotListItem]',
    changeDetection: ChangeDetectionStrategy.OnPush,
    template: `
        <ng-content />
    `
})
export class DotSemanticListItem {}

@Component({
    selector: 'p[dotParagraph]',
    changeDetection: ChangeDetectionStrategy.OnPush,
    template: `
        <ng-content />
    `
})
export class DotSemanticParagraph {}

@Component({
    selector: 'blockquote[dotBlockQuote]',
    changeDetection: ChangeDetectionStrategy.OnPush,
    template: `
        <ng-content />
    `
})
export class DotSemanticBlockQuote {}

@Component({
    selector: 'pre[dotCodeBlock]',
    changeDetection: ChangeDetectionStrategy.OnPush,
    template: `
        <code><ng-content /></code>
    `
})
export class DotSemanticCodeBlock {}
