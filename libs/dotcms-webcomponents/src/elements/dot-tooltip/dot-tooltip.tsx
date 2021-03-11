import { Component, Prop, Element } from '@stencil/core';
import { getElement, getPosition, PositionX, PositionY, fadeIn } from './utils';

@Component({
    tag: 'dot-tooltip',
    styleUrl: 'dot-tooltip.scss',
    shadow: true
})
export class DotTooltip {
    @Element() el: HTMLElement;

    @Prop() content: string;
    @Prop() for: string;
    @Prop() delay: number;
    @Prop() position = 'center bottom';

    private targetEl: HTMLElement;
    private tooltipEl: HTMLElement;
    private showing = false;

    connectedCallback() {
        const selector = `#${this.for}`;
        this.targetEl = this.el.parentElement
            ? this.el.parentElement.querySelector(selector)
            : this.el.offsetParent.shadowRoot.querySelector(selector);

        this.bindEvents();
    }

    disconnectedCallback() {
        this.unBindEvents();
    }

    private appendTooltip() {
        this.tooltipEl = getElement(this.content);
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
        this.targetEl.addEventListener('mouseenter', this.showTooltip.bind(this));
        this.targetEl.addEventListener('mouseleave', this.removeToolTip.bind(this));
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
            this.tooltipEl = null;
        }
    }

    private unBindEvents() {
        this.targetEl.removeEventListener('mouseenter', this.showTooltip.bind(this));
        this.targetEl.removeEventListener('mouseleave', this.removeToolTip.bind(this));
        window.removeEventListener('scroll', this.removeToolTip.bind(this));
    }

    render() {
        return null;
    }
}
