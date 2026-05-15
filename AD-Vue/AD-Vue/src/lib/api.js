const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL || '').replace(/\/$/, '')
const REQUEST_TIMEOUT_MS = 8000

export class ApiError extends Error {
  constructor(message, status = 0, code = '') {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.code = code
  }
}

function buildUrl(path) {
  if (!API_BASE_URL) return path
  return `${API_BASE_URL}${path}`
}

async function request(path, options = {}) {
  let response
  const controller = new AbortController()
  const timeoutId = window.setTimeout(() => controller.abort(), REQUEST_TIMEOUT_MS)

  try {
    response = await fetch(buildUrl(path), {
      headers: {
        'Content-Type': 'application/json',
        ...(options.headers || {}),
      },
      ...options,
      signal: controller.signal,
    })
  } catch (error) {
    if (error?.name === 'AbortError') {
      throw new ApiError('请求超时，请检查网络后重试', 0, 'TIMEOUT')
    }
    throw new ApiError('无法连接后端服务', 0, 'NETWORK_ERROR')
  } finally {
    window.clearTimeout(timeoutId)
  }

  if (!response.ok) {
    const parsed = await parseError(response)
    throw new ApiError(parsed.message, response.status, parsed.code)
  }

  if (response.status === 204) return null
  return response.json()
}

async function parseError(response) {
  const text = await response.text()
  if (!text) return { message: '请求失败', code: '' }

  try {
    const payload = JSON.parse(text)
    return {
      message: payload.detail || payload.message || payload.title || '请求失败',
      code: payload.code || '',
    }
  } catch (_error) {
    return { message: text, code: '' }
  }
}

export function registerUser(payload) {
  return request('/api/auth/register', {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}

export function loginUser(payload) {
  return request('/api/auth/login', {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}

export function updateProfile(id, payload) {
  return request(`/api/auth/profile/${id}`, {
    method: 'PUT',
    body: JSON.stringify(payload),
  })
}

export function fetchUserTickets(userId) {
  return request(`/api/tickets/users/${userId}`)
}

export function createNewUserBindTicket({ userId, studentId, phone }) {
  return request('/api/tickets/new-user-bind', {
    method: 'POST',
    body: JSON.stringify({
      ticketType: 1,
      userId,
      studentId,
      phone,
    }),
  })
}

export function createBroadbandPasswordResetTicket({
  userId,
  studentId,
  phone,
  broadbandAccount,
  newPassword,
}) {
  return request('/api/tickets/broadband-password-reset', {
    method: 'POST',
    body: JSON.stringify({
      ticketType: 3,
      userId,
      studentId,
      phone,
      broadbandAccount,
      newPassword,
    }),
  })
}
