/**
 * receive an array containing multiple arrays
 * @param lists 
 */
export function emptyAllInnerLists(lists: any[][]): void {
    // Add a security check to prevent passing undefined/null
    if (!Array.isArray(lists)) return;

    lists.forEach(innerList => {
        if (Array.isArray(innerList)) {
            innerList.splice(0, innerList.length);
        }
    });
}
