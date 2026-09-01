import { ChartColors } from '@dotcms/dotcms-models';

interface DotExperimentsChartjsOptions {
    xAxisLabel: string;
    yAxisLabel: string;
    isLinearAxis: boolean;
}

/**
 * Generate the options to use in a line ChartJS with the
 * x-axis and y-axis translated
 *
 * @param xAxisLabel
 * @param yAxisLabel
 * @param isLinearAxis
 */
export const generateDotExperimentLineChartJsOptions = ({
    xAxisLabel,
    yAxisLabel,
    isLinearAxis
}: DotExperimentsChartjsOptions) => {
    // Default line chart options
    const defaultOptions = {
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

                        return `${label}: ${context.parsed.y + '%'}`;
                    },
                    labelColor: function (context) {
                        return {
                            borderColor: context.dataset.borderColor,
                            backgroundColor: context.dataset.borderColor,
                            borderWidth: 2.5,
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
            mode: 'nearest',
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
                    color: ChartColors.ticks.color,
                    autoSkip: true,

                    padding: 10,
                    align: 'center'
                },
                border: {
                    display: true,
                    color: ChartColors.xAxis.border
                },
                grid: {
                    color: ChartColors.xAxis.gridLine,
                    lineWidth: 0.8
                },
                begingAtZero: true
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
                    color: ChartColors.ticks.color,
                    precision: 0,
                    callback: function (value) {
                        return value.toFixed(0) + '%';
                    }
                },
                border: {
                    display: true,
                    dash: [6, 3],
                    color: ChartColors.yAxis.border
                },
                grid: {
                    color: ChartColors.yAxis.gridLine,
                    lineWidth: [0.3],
                    borderDash: [8, 4]
                },

                crossAlign: 'center'
            }
        }
    };

    // Options to use in a linear a chart (x-axis max 0-1)
    const linearChartOptions = {
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
                        return Math.round(context[0].label * 100) + '%';
                    },
                    label: function (context) {
                        const label = context.dataset.label || '';

                        return `${label}: ${context.parsed.y}`;
                    },
                    labelColor: function (context) {
                        return {
                            borderColor: context.dataset.borderColor,
                            backgroundColor: context.dataset.borderColor,
                            borderWidth: 2.5,
                            borderRadius: 5
                        };
                    },
                    labelTextColor: function () {
                        return ChartColors.white;
                    }
                }
            }
        },
        scales: {
            ...defaultOptions.scales,
            x: {
                ...defaultOptions.scales.x,
                type: 'linear',
                position: 'bottom',
                min: 0,
                max: 1,
                ticks: {
                    ...defaultOptions.scales.x.ticks,
                    callback: function (value) {
                        return (value * 100).toFixed(0) + '%';
                    }
                }
            },
            y: {
                ...defaultOptions.scales.y,
                type: 'linear',
                position: 'left',
                min: 0,
                ticks: { ...defaultOptions.scales.y.ticks, callback: (value: string) => value }
            }
        }
    };

    return isLinearAxis
        ? {
              ...defaultOptions,
              ...linearChartOptions
          }
        : defaultOptions;
};
