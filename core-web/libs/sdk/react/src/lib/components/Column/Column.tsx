import styles from './column.module.css';

import { Container } from '../Container/Container';
import { PageProviderContext } from '../PageProvider/PageProvider';

interface ColumnProps {
    readonly column: PageProviderContext['layout']['body']['rows'][0]['columns'][0];
}

export function Column({ column }: ColumnProps) {
    const widthClassMap: Record<number, string> = {
        1: 'col-span-1',
        2: 'col-span-2',
        3: 'col-span-3',
        4: 'col-span-4',
        5: 'col-span-5',
        6: 'col-span-6',
        7: 'col-span-7',
        8: 'col-span-8',
        9: 'col-span-9',
        10: 'col-span-10',
        11: 'col-span-11',
        12: 'col-span-12'
    };

    const statrClassMap: Record<number, string> = {
        1: 'col-start-1',
        2: 'col-start-2',
        3: 'col-start-3',
        4: 'col-start-4',
        5: 'col-start-5',
        6: 'col-start-6',
        7: 'col-start-7',
        8: 'col-start-8',
        9: 'col-start-9',
        10: 'col-start-10',
        11: 'col-start-11',
        12: 'col-start-12'
    };

    const widthClass = widthClassMap[column.width];
    const startClass = statrClassMap[column.leftOffset];

    const combinedClasses = [styles[widthClass], styles[startClass], column.styleClass]
        .filter(Boolean)
        .join(' ');

    return (
        <div data-dot="column" data-testid="column" className={combinedClasses}>
            {column.containers.map((container) => (
                <Container
                    key={`${container.identifier}-${container.uuid}`}
                    containerRef={container}
                />
            ))}
        </div>
    );
}
