export const COMPONENTS: Record<string, any> = {
  Activity: {
    component: import('../contentlets/activity/activity.component').then(c => c.ActivityComponent),
  },
};