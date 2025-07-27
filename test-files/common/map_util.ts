export function mapToArray<T>(map: Map<string, T>): T[] {
    return Array.from(map.values());
}

export function arrayToMap<T>(array: T[], keyFn: (item: T) => string): Map<string, T> {
    const map = new Map<string, T>();
    array.forEach(item => map.set(keyFn(item), item));
    return map;
}