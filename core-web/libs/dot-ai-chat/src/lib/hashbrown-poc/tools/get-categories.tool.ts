import { createTool } from '@hashbrownai/angular';

export const getCategoriesTool = createTool({
    name: 'getCategories',
    description: 'Get categories from API',
    handler: async () => {
        const response = await fetch('https://api.escuelajs.co/api/v1/categories');
        return await response.json();
    }
});
