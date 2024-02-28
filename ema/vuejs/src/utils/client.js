import { dotcmsClient } from '@dotcms/client'

export const client = dotcmsClient.init({
  dotcmsUrl: import.meta.env.VITE_DOTCMS_HOST,
  authToken: import.meta.env.VITE_DOTCMS_TOKEN,
  requestOptions: {
    cache: 'no-cache'
  }
})
