const COLOR_PALETTE_GRAY_100 = '#fafafb';

const getOrCreateLegendList = (chart, id) => {
    const legendContainer = document.getElementById(id);
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
    afterUpdate(chart, args, options) {
        const ul = getOrCreateLegendList(chart, options.containerID);

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
