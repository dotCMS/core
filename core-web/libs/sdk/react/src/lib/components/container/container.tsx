import styles from './container.module.css';

/* eslint-disable-next-line */
export interface ContainerProps {}

export function Container(_props: ContainerProps) {
  return (
    <div className={styles['container']}>
      <h1>Welcome to Container!</h1>
    </div>
  );
}

export default Container;
