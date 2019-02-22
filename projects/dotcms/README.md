# Add DotCMS Library to your JavaScript project

## Install

`npm install dotcms --save`

## Use

```javascript
import { initDotCMS } from 'dotcms';

const dotcms = initDotCMS({
    host: 'YOUR_DOTCMS_INSTANCE',
    token: 'YOUR AUTH TOKEN',
    environment: 'YOUR ENV'
});

// GET A PAGE:
dotcms.page
    .get({
        url: '/about-us'
    })
    .then((data) => {
        console.log(data);
    });
```

