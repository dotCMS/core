import { ChangeDetectionStrategy, Component, computed, Input } from '@angular/core';

import { BlockEditorMark } from '@dotcms/types';

@Component({
    selector: 'dotcms-block-editor-renderer-paragraph',
    standalone: true,
    changeDetection: ChangeDetectionStrategy.OnPush,
    template: `
        <p>
            <ng-content />
        </p>
    `
})
export class DotParagraphBlock {}

@Component({
    selector: 'dotcms-block-editor-renderer-heading',
    standalone: true,
    changeDetection: ChangeDetectionStrategy.OnPush,
    template: `
        @switch (level) {
            @case ('1') {
                <h1>
                    <ng-content />
                </h1>
            }
            @case ('2') {
                <h2>
                    <ng-content />
                </h2>
            }
            @case ('3') {
                <h3>
                    <ng-content />
                </h3>
            }
            @case ('4') {
                <h4>
                    <ng-content />
                </h4>
            }
            @case ('5') {
                <h5>
                    <ng-content />
                </h5>
            }
            @case ('6') {
                <h6>
                    <ng-content />
                </h6>
            }
            @default {
                <h1>
                    <ng-content />
                </h1>
            }
        }
    `
})
export class DotHeadingBlock {
    @Input() level!: string;
}

interface TextBlockProps {
    marks?: BlockEditorMark[];
    text?: string;
}

@Component({
    selector: 'dotcms-block-editor-renderer-text',
    standalone: true,
    changeDetection: ChangeDetectionStrategy.OnPush,
    template: `
        @switch (marks?.[0]?.type) {
            @case ('link') {
                <a
                    [attr.href]="$currentAttrs()['href'] || ''"
                    [attr.target]="$currentAttrs()['target'] || ''">
                    <dotcms-block-editor-renderer-text [marks]="$remainingMarks()" [text]="text" />
                </a>
            }
            @case ('bold') {
                <strong>
                    <dotcms-block-editor-renderer-text [marks]="$remainingMarks()" [text]="text" />
                </strong>
            }
            @case ('underline') {
                <u>
                    <dotcms-block-editor-renderer-text [marks]="$remainingMarks()" [text]="text" />
                </u>
            }
            @case ('italic') {
                <em>
                    <dotcms-block-editor-renderer-text [marks]="$remainingMarks()" [text]="text" />
                </em>
            }
            @case ('strike') {
                <s>
                    <dotcms-block-editor-renderer-text [marks]="$remainingMarks()" [text]="text" />
                </s>
            }
            @case ('superscript') {
                <sup>
                    <dotcms-block-editor-renderer-text [marks]="$remainingMarks()" [text]="text" />
                </sup>
            }
            @case ('subscript') {
                <sub>
                    <dotcms-block-editor-renderer-text [marks]="$remainingMarks()" [text]="text" />
                </sub>
            }
            @default {
                {{ text }}
            }
        }
    `
})
export class DotTextBlock {
    @Input() marks: TextBlockProps['marks'] = [];
    @Input() text = '';

    protected readonly $remainingMarks = computed(() => this.marks?.slice(1));

    protected readonly $currentAttrs = computed(() => {
        const attrs = { ...(this.marks?.[0]?.attrs || {}) };

        if (attrs['class']) {
            attrs['className'] = attrs['class'];
            delete attrs['class'];
        }

        return attrs;
    });
}
