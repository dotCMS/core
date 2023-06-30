# Dot Experiments Portlet

This lib contain all related to Experiments in dotCMS, has 2 main libraries:

| Folder name   | Project Name                           | Description                                                                |
|---------------|----------------------------------------|----------------------------------------------------------------------------|
| `portlet`     | `portlets-dot-experiments-portlet`     | All related to the portlet of experiment                                   |
| `data-access` | `portlets-dot-experiments-data-access` | All related to the data access of experiment (services, resolvers, guards) |  

## Running Unit Tests

- For run the portlet unit test do: `nx run portlets-dot-experiments-portlet:test`.
- For run the data-access unit test do: `nx run portlets-dot-experiments-data-access:test`.

## Running lint

- For run the portlet unit test do: `nx run portlets-dot-experiments-portlet:lint`.
- For run the data-access unit test do: `nx run portlets-dot-experiments-data-access:lint`.

## Portlet Library Structure

| Folder name                     | Project Name                                                               |
|---------------------------------|----------------------------------------------------------------------------|
| `dot-experiments-configuration` | Container of Experiment Configuration and all components related to it.    |
| `dot-experiments-list`          | Container of Experiment List and all components related to it.             | 
| `dot-experiments-reports`       | Container of Experiment Reports and all components related to it.          | 
| `dot-experiments-shell`         | Shell of the Experiment Library and the Featured Component Store           | 
| `shared`                        | Reusable components, directives and utils with specific use in Experiment. | 

## Data Access Library Structure

| Folder name | Project Name               |
|-------------|----------------------------|
| `resolvers` | Resolvers of experiments   |
| `services`  | Main service of experiment | 
