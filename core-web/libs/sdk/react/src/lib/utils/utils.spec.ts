import { getPageElementBound } from './utils'; // Adjust the import path based on your file structure.

describe('getPageElementBound', () => {
    beforeAll(() => {
        // Mock getBoundingClientRect
        Element.prototype.getBoundingClientRect = jest.fn(() => {
            return {
                x: 100,
                y: 200,
                width: 500,
                height: 300,
                top: 200,
                left: 100,
                right: 600,
                bottom: 500,
                toJSON: jest.fn()
            };
        });
    });

    it('calculates bounds for rows and their children', () => {
        // Create a mock DOM structure
        document.body.innerHTML = `
          <div id="row">
            <div class="column" data-dot="container">
              <div class="contentlet" data-dot="contentlet"></div>
            </div>
          </div>
        `;

        const rows = [document.getElementById('row')] as HTMLDivElement[];
        const result = getPageElementBound(rows);

        expect(result).toEqual([
            {
                x: 100,
                y: 200,
                width: 500,
                height: 300,
                columns: [
                    {
                        x: 0,
                        y: 0,
                        width: 500,
                        height: 300,
                        containers: []
                    }
                ]
            }
        ]);
    });
});
