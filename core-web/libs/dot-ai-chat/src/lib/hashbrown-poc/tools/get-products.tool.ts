import { createTool } from '@hashbrownai/angular';

export const getProductsTool = createTool({
    name: 'getProducts',
    description: 'Get products from API',
    handler: async () => {
        const response = await fetch('https://api.escuelajs.co/api/v1/products');
        return await response.json();
    }
});
