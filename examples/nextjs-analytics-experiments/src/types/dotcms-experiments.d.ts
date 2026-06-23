declare module "@dotcms/experiments" {
  import type { DotCMSLayoutBodyProps } from "@dotcms/react";
  import type { ComponentType } from "react";

  export interface DotExperimentConfig {
    apiKey: string;
    server: string;
    debug?: boolean;
    redirectFn?: (url: string) => void;
    trackPageView?: boolean;
  }

  export function withExperiments(
    WrappedComponent: ComponentType<DotCMSLayoutBodyProps>,
    config: DotExperimentConfig,
  ): (props: DotCMSLayoutBodyProps) => React.JSX.Element;
}
