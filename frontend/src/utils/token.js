const TOKEN_KEY = 'token'

/** 勾选「记住我」时存 localStorage，否则存 sessionStorage（关闭浏览器即失效） */
export function getToken() {
  return localStorage.getItem(TOKEN_KEY) || sessionStorage.getItem(TOKEN_KEY) || ''
}

export function isRemembered() {
  return !!localStorage.getItem(TOKEN_KEY)
}

export function setToken(token, remember) {
  removeToken()
  if (remember) {
    localStorage.setItem(TOKEN_KEY, token)
  } else {
    sessionStorage.setItem(TOKEN_KEY, token)
  }
}

/** 会话续期后写回原存储位置，保持「记住我」的选择不变 */
export function refreshToken(token) {
  setToken(token, isRemembered())
}

export function removeToken() {
  localStorage.removeItem(TOKEN_KEY)
  sessionStorage.removeItem(TOKEN_KEY)
}
