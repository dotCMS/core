const COLOR_PALETTE_GRAY_100 = getComputedStyle(document.body).getPropertyValue(
    '--color-palette-black-op-10'
);

const getOrCreateLegendList = (chart) => {
    if (!chart || !chart.canvas) {
        return null;
    }
    
    // Start from the canvas and traverse up to find the parent container
    // The legend-wrapper is a sibling of the p-chart component
    let current = chart.canvas;
    let legendContainer = null;
    
    // Traverse up the DOM tree to find a parent that contains the legend-wrapper
    while (current && current.parentElement && !legendContainer) {
        current = current.parentElement;
        
        // Check if this parent has a legend-wrapper as a direct child or descendant
        legendContainer = current.querySelector('.legend-wrapper');
        
        // If found in a sibling, we need to go up one more level
        if (!legendContainer && current.parentElement) {
            const parent = current.parentElement;
            // Check siblings of current element
            const siblings = Array.from(parent.children);
            for (const sibling of siblings) {
                if (sibling instanceof Element && sibling.classList.contains('legend-wrapper')) {
                    legendContainer = sibling;
                    break;
                }
                if (sibling instanceof Element) {
                    const found = sibling.querySelector('.legend-wrapper');
                    if (found) {
                        legendContainer = found;
                        break;
                    }
                }
            }
        }
        
        // Stop if we've gone too far up
        if (current.tagName === 'BODY' || current.tagName === 'HTML') {
            break;
        }
    }
    
    // If still not found, return null to prevent errors
    if (!legendContainer) {
        return null;
    }

    let listContainer = legendContainer.querySelector('ul');

    if (!listContainer) {
        listContainer = document.createElement('ul');
        listContainer.style.display = 'flex';
        listContainer.style.flexDirection = 'row';
        listContainer.style.margin = '0';
        listContainer.style.padding = '0';
        listContainer.style.gap = '1rem';
        listContainer.style.listStyle = 'none';

        legendContainer.appendChild(listContainer);
    }

    return listContainer;
};

/**
 * Plugin to use with ChartJS to replace the default generation of legend to
 * create a dynamic ul with all the legends of the datasets
 */
export const htmlLegendPlugin = {
    id: 'dotHtmlLegend',
    afterUpdate(chart) {
        const ul = getOrCreateLegendList(chart);
        
        // If legend container not found, skip rendering
        if (!ul) {
            return;
        }

        while (ul.firstChild) {
            ul.firstChild.remove();
        }

        const items = chart.options.plugins.legend.labels.generateLabels(chart);

        items.forEach((item) => {
            const li = document.createElement('li');
            li.style.cursor = 'pointer';
            li.style.padding = '.5rem';
            li.style.background = item.fillStyle;
            li.style.borderRadius = '8px';
            li.style.backgroundColor = item.hidden ? COLOR_PALETTE_GRAY_100 : item.fillStyle;

            li.onclick = () => {
                chart.setDatasetVisibility(
                    item.datasetIndex,
                    !chart.isDatasetVisible(item.datasetIndex)
                );

                chart.update();
            };

            const textContainer = document.createElement('p');
            textContainer.style.color = item.fontColor;
            textContainer.style.margin = '0';
            textContainer.style.padding = '0';

            const text = document.createTextNode(item.text);
            textContainer.appendChild(text);

            li.appendChild(textContainer);
            ul.appendChild(li);
        });
    }
};
