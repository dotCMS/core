import Container from './container';

function Column({ column }) {
    // Calculate Tailwind classes for grid column span and start
    const widthClassMap = {
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

    const statrClassMap = {
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

    return (
        <div className={`${widthClass} ${startClass}`}>
            {column.containers.map((container, index) => (
                <Container key={container.identifier} containerRef={container} />
            ))}
        </div>
    );
}

export default Column;
