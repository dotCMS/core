import { useContext } from 'react';

import styles from './column.module.css';

import { PageContext } from '../../contexts/PageContext';
import { combineClasses, getPositionStyleClasses } from '../../utils/utils';
import { Container } from '../Container/Container';
import { DotCMSPageContext } from '../PageProvider/PageProvider';

export interface ColumnProps {
    readonly column: DotCMSPageContext['pageAsset']['layout']['body']['rows'][0]['columns'][0];
}

export function Column({ column }: ColumnProps) {
    const { isInsideEditor } = useContext(PageContext) as DotCMSPageContext;

    const { startClass, endClass } = getPositionStyleClasses(
        column.leftOffset,
        column.width + column.leftOffset
    );

    const combinedClasses = combineClasses([
        styles[endClass],
        styles[startClass],
        column.styleClass
    ]);

    const columnProps = isInsideEditor
        ? {
              'data-dot': 'column',
              'data-testid': 'column'
          }
        : {};

    return (
        <div {...columnProps} className={combinedClasses}>
            {column.containers.map((container) => (
                <Container
                    key={`${container.identifier}-${container.uuid}`}
                    containerRef={container}
                />
            ))}
        </div>
    );
}
