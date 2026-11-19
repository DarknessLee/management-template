/**
 * 后端返回的是服务端相对路径（如 /uploads/avatar/x.png），
 * 浏览器需要通过 /api 前缀访问，与 vite 代理、后端 context-path 保持一致。
 */
export function assetUrl(path) {
  if (!path) return ''
  if (/^https?:\/\//.test(path)) return path
  return `/api${path.startsWith('/') ? path : `/${path}`}`
}
