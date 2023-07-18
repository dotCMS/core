import { ChartColors } from '@dotcms/dotcms-models';

interface DotExperimentsChartjsOptions {
    legendId: string;
    xAxisLabel: string;
    yAxisLabel: string;
}

/**
 * Generate the options to use in a line ChartJS with the
 * x-axis and y-axis translated
 *
 * @param xAxisLabel
 * @param yAxisLabel
 */
export const generateDotExperimentLineChartJsOptions = ({
    legendId,
    xAxisLabel,
    yAxisLabel
}: DotExperimentsChartjsOptions) => {
    return {
        responsive: true,
        plugins: {
            title: {
                display: false
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
                        return ChartColors.white;
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
                    color: ChartColors.ticks.hex,
                    autoSkip: true,

                    padding: 10,
                    align: 'center'
                },
                border: {
                    display: true,
                    color: ChartColors.xAxis.gridLine
                },
                grid: {
                    color: ChartColors.xAxis.gridLine,
                    lineWidth: 0.8
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
                    color: ChartColors.ticks.hex,
                    precision: 0
                },
                border: {
                    display: true,
                    dash: [6, 3]
                },
                grid: {
                    color: ChartColors.yAxis.gridLine,
                    lineWidth: [0.2],
                    borderDash: [8, 4]
                },

                crossAlign: 'center'
            }
        }
    };
};
