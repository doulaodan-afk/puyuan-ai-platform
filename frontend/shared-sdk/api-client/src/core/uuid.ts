/**
 * 生成 UUID v4 的兼容实现，不依赖 crypto.randomUUID()
 * 适用于非安全上下文（如 http://localhost）
 */
export function generateUUID(): string {
  try {
    return crypto.randomUUID();
  } catch {
    return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (c) => {
      const r = (crypto.getRandomValues(new Uint8Array(1))[0] & 15) >> (c === 'x' ? 0 : 3);
      return (c === 'x' ? r : (r & 0x3) | 0x8).toString(16);
    });
  }
}
