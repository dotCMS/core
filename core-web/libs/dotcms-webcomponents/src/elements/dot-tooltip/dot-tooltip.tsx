import { Component, Prop, Element } from '@stencil/core';
import { getElement, getPosition, PositionX, PositionY, fadeIn } from './utils';

@Component({
    tag: 'dot-tooltip',
    styleUrl: 'dot-tooltip.scss',
    shadow: true
})
export class DotTooltip {
    @Element() el!: HTMLElement;

    @Prop() content?: string;
    @Prop() for?: string;
    @Prop() delay?: number;
    @Prop() position = 'center bottom';

    /** Null when `for` names an element that is not in this tooltip's parent or shadow root. */
    private targetEl: HTMLElement | null = null;
    private tooltipEl?: HTMLElement;
    private showing = false;

    connectedCallback() {
        const selector = `#${this.for}`;
        this.targetEl = this.el.parentElement
            ? this.el.parentElement.querySelector(selector)
            : // `offsetParent` is null for a hidden element, and only an open shadow root is
              // reachable — either way there is nothing to attach to.
              (this.el.offsetParent?.shadowRoot?.querySelector<HTMLElement>(selector) ?? null);

        this.bindEvents();
    }

    disconnectedCallback() {
        this.unBindEvents();
    }

    private appendTooltip() {
        if (!this.targetEl) {
            return;
        }

        this.tooltipEl = getElement(this.content ?? '');
        document.body.appendChild(this.tooltipEl);

        const [x, y] = this.position.split(' ');

        const { left, top } = getPosition({
            tooltipEl: this.tooltipEl,
            targetEl: this.targetEl,
            position: {
                x: x as PositionX,
                y: y as PositionY
            }
        });

        this.tooltipEl.style.left = `${left}px`;
        this.tooltipEl.style.top = `${top}px`;
        fadeIn(this.tooltipEl);
    }

    private bindEvents() {
        // Guarded rather than left to throw. An exception from `connectedCallback` is reported to
        // the global error handler and the element stays connected with nothing bound, so the
        // outcome was already "this tooltip does nothing" — this just says so without the noise.
        this.targetEl?.addEventListener('mouseenter', this.showTooltip.bind(this));
        this.targetEl?.addEventListener('mouseleave', this.removeToolTip.bind(this));
        window.addEventListener('scroll', this.removeToolTip.bind(this));
    }

    private showTooltip() {
        this.showing = true;
        if (this.delay) {
            setTimeout(() => {
                if (this.showing) {
                    this.appendTooltip();
                }
            }, this.delay);
        } else {
            this.appendTooltip();
        }
    }

    private removeToolTip() {
        this.showing = false;
        if (this.tooltipEl) {
            document.body.removeChild(this.tooltipEl);
            this.tooltipEl = undefined;
        }
    }

    private unBindEvents() {
        // NOTE: `.bind(this)` builds a new function each call, so these three removals have never
        // matched the listeners `bindEvents` added. Pre-existing; holding the bound references is a
        // behaviour change, not a typing one.
        this.targetEl?.removeEventListener('mouseenter', this.showTooltip.bind(this));
        this.targetEl?.removeEventListener('mouseleave', this.removeToolTip.bind(this));
        window.removeEventListener('scroll', this.removeToolTip.bind(this));
    }

    render() {
        return null;
    }
}
