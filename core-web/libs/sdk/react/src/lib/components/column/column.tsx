import styles from './column.module.css';

import { ColumnModel } from '../../types/page.model';

/* eslint-disable-next-line */
interface ColumnProps {
  column: ColumnModel;
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
    12: 'col-span-12',
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
    12: 'col-start-12',
  };

  const widthClass = widthClassMap[column.width];
  const startClass = statrClassMap[column.leftOffset];

  return (
    <div
      data-dot="column"
      className={`${styles[widthClass]} ${styles[startClass]}`}
    >
      {column.containers.map((_container) => (
        <h3>Container</h3>
      ))}
    </div>
  );
}

export default Column;
