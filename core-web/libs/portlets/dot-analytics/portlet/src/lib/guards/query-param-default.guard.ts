import { inject } from '@angular/core';
import { CanActivateFn, Router, UrlTree } from '@angular/router';


export const queryParamDefaultGuard: CanActivateFn = (route) => {
  const router = inject(Router);

  const queryParams = route.queryParamMap;

  console.log(queryParams);


  if (queryParams.has('time_range')) {
    return true;
  } else {
    const urlTree: UrlTree = router.createUrlTree(['/analytics/dashboard'], {
      queryParams: {
        time_range: 'last7days'
      },
      queryParamsHandling: 'merge',
    });
    return urlTree;
  }
};
