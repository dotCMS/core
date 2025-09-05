import {
  DotCMSComposedPageResponse,
  DotCMSNavigationItem,
  DotCMSExtendedPageResponse,
} from '@dotcms/types';

export type PageError = {
  message: string;
  status: number | string;
};

export type ComposedPageResponse<T extends DotCMSExtendedPageResponse> =
  DotCMSComposedPageResponse<T>;

export type PageState<T extends DotCMSExtendedPageResponse> = {
  pageResponse?: ComposedPageResponse<T> | null;
  nav?: DotCMSNavigationItem;
  error?: PageError;
  status: 'idle' | 'success' | 'error' | 'loading';
};
