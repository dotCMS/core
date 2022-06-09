export type PositionX = 'left' | 'right' | 'center';
export type PositionY = 'bottom' | 'top';

interface PositionParams {
    tooltipEl: HTMLElement;
    targetEl: HTMLElement;
    position: { x: PositionX; y: PositionY };
}

export const fadeIn = (el: HTMLElement) => {
    el.style.opacity = '0';

    (function fade() {
        let val = parseFloat(el.style.opacity);
        if (!((val += 0.1) > 1)) {
            el.style.opacity = val.toString();
            requestAnimationFrame(fade);
        }
    })();
};

export const getElement = (content: string): HTMLElement => {
    const el = document.createElement('span');
    el.style.padding = '2px 5px';
    el.style.backgroundColor = '#444';
    el.style.borderRadius = '2px';
    el.style.color = '#fff';
    el.style.position = 'absolute';
    el.style.opacity = '0';
    el.style.whiteSpace = 'nowrap';

    el.innerText = content;
    return el;
};

export const getPosition = (params: PositionParams): { top: number; left: number } => {
    let finalLeft = getPositionX(params);
    let finalTop = getPositionY(params);

    return {
        top: finalTop,
        left: finalLeft
    };
};

export const getPositionX = ({
    tooltipEl: tooltip,
    targetEl: target,
    position
}: PositionParams): number => {
    const tooltipPos = tooltip.getBoundingClientRect();
    const targetPos = target.getBoundingClientRect();
    let result = targetPos.left; // default left positioned

    if (position.x === 'center') {
        const targetCenter = targetPos.width / 2 + targetPos.left;
        const toolTipHalf = tooltipPos.width / 2;
        result = targetCenter - toolTipHalf;
    } else if (position.x === 'right') {
        result = targetPos.right - tooltipPos.width;
    }

    // Fix if the tooltip is out of the window
    if (result + tooltipPos.width > window.innerWidth) {
        result = targetPos.right - tooltipPos.width;
    }

    return result;
};

export const getPositionY = ({
    tooltipEl: tooltip,
    targetEl: target,
    position
}: PositionParams): number => {
    const MARGIN = 4; // this might be an attr in the future
    const tooltipPos = tooltip.getBoundingClientRect();
    const targetPos = target.getBoundingClientRect();
    let result = targetPos.bottom + MARGIN; // default bottom positioned

    if (position.y === 'top') {
        result = targetPos.top - tooltipPos.height - MARGIN;
    }

    return result;
};
