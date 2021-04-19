export interface ChartDataSet {
    label: string;
    data: number[];
    borderColor: string;
    fill: boolean;
}
export interface ChartData {
    labels: string[];
    datasets: ChartDataSet[];
}

export interface ChartOptions {
    title: {
        display: boolean;
        text: string;
        fontSize: number;
    };
    legend: {
        position: string;
    };
}

export interface SelectValues {
    name: string;
    value: string;
}
