import styles from './sdk-react.module.css';

/* eslint-disable-next-line */
export interface SdkReactProps {}

export function SdkReact(_props: SdkReactProps) {
  return (
    <div className={styles['container']}>
      <h1>Welcome to SdkReact!</h1>
    </div>
  );
}

export default SdkReact;
