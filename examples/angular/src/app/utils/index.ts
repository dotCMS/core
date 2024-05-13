export const COMPONENTS: Record<string, any> = {
  Activity: {
    // TODO: Discuss with the team if we should use the `import` function here.
    component: import('../contentlets/activity/activity.component').then(c => c.ActivityComponent),
  },
};