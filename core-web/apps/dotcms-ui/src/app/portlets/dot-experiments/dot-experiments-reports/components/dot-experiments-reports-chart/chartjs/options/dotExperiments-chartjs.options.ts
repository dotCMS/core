interface DotExperimentsChartjsOptions {
    xAxisLabel: string;
    yAxisLabel: string;
}

export const dotExperimentChartJsOptions = ({
    xAxisLabel,
    yAxisLabel
}: DotExperimentsChartjsOptions) => {
    return {
        responsive: true,
        plugins: {
            title: {
                display: false
            },
            dotHtmlLegend: {
                containerID: 'legend-container'
            },
            legend: {
                display: false
            },
            tooltip: {
                callbacks: {
                    title: function (context) {
                        const [, title] = context[0].label.split(',');

                        return title;
                    },
                    label: function (context) {
                        const label = context.dataset.label || '';

                        return `${label}: ${context.parsed.y}`;
                    },
                    labelColor: function (context) {
                        return {
                            borderColor: context.dataset.borderColor,
                            backgroundColor: context.dataset.borderColor,
                            borderWidth: 2.4,
                            borderRadius: 5
                        };
                    },
                    labelTextColor: function () {
                        return '#fff';
                    }
                }
            }
        },
        interaction: {
            mode: 'index',
            intersect: false
        },

        scales: {
            x: {
                title: {
                    display: true,
                    text: xAxisLabel,
                    font: {
                        size: 15,
                        lineHeight: 1.2
                    },
                    padding: 5
                },
                ticks: {
                    callback: function (val, index) {
                        return index % 2 === 0 ? this.getLabelForValue(val as number) : '';
                    },
                    color: '#524E5C',
                    autoSkip: true,
                    autoSkipPadding: 3,
                    maxTicksLimit: 10,
                    padding: 17
                },
                border: {
                    display: true,
                    color: 'black'
                },
                grid: {
                    display: true,
                    color: '#AFB3C0',
                    lineWidth: 0.8,
                    drawTicks: false
                }
            },
            y: {
                title: {
                    display: true,
                    text: yAxisLabel,
                    font: {
                        size: 15,
                        lineHeight: 1.2
                    },
                    padding: 5
                },
                ticks: {
                    color: '#524E5C'
                },
                border: {
                    display: true,
                    dash: [6, 3]
                },
                grid: {
                    color: '#3D404D',
                    lineWidth: [0.2]
                }
            }
        }
    };
};
