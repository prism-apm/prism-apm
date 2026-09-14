export function orderBy(a: string | number, b: string | number, order: 'asc' | 'desc'): number {
    if (parseFloat(a as string) > parseFloat(b as string)) {
        return order === 'asc' ? 1 : -1
    } else {
        return order === 'asc' ? -1 : 1
    }
}
