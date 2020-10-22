# DotCMS Javascript Library

Client and node library that allows easy interaction with several [DotCMS Rest APIS](https://dotcms.com/docs/latest/rest-api).

## Install

`npm install dotcms node-fetch --save`

## Use

```javascript
import { initDotCMS } from 'dotcms';

const dotcms = initDotCMS({
    host: 'YOUR_DOTCMS_INSTANCE', // Non required, will be using in the requests if you pass it
    token: 'YOUR AUTH TOKEN'
});

// Example
dotcms.page
    .get({
        url: '/about-us'
    })
    .then((data) => {
        console.log(data);
    })
    .catch((err) => {
        console.error(err.status, err.message);
    });
```

## Documentation

[Full Documentation](https://dotcms.github.io/core-web/docs/dotcms/globals.html)
