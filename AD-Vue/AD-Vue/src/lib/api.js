const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL || '').replace(/\/$/, '')

function buildUrl(path) {
  if (!API_BASE_URL) return path
  return `${API_BASE_URL}${path}`
}

async function request(path, options = {}) {
  let response

  try {
    response = await fetch(buildUrl(path), {
      headers: {
        'Content-Type': 'application/json',
        ...(options.headers || {}),
      },
      ...options,
    })
  } catch (_error) {
    throw new Error('无法连接后端服务')
  }

  if (!response.ok) {
    throw new Error(await parseError(response))
  }

  if (response.status === 204) return null
  return response.json()
}

async function parseError(response) {
  const text = await response.text()
  if (!text) return '请求失败'

  try {
    const payload = JSON.parse(text)
    return payload.detail || payload.message || payload.title || '请求失败'
  } catch (_error) {
    return text
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
