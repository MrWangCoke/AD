<script setup>
import { computed, onMounted, onUnmounted, reactive, ref, watch } from 'vue'
import AuthScreen from './components/AuthScreen.vue'
import HomePage from './components/HomePage.vue'
import SubmitPage from './components/SubmitPage.vue'
import TicketsPage from './components/TicketsPage.vue'
import ConsultPage from './components/ConsultPage.vue'
import TopNav from './components/TopNav.vue'
import ProfileModal from './components/ProfileModal.vue'
import ToastStack from './components/ToastStack.vue'
import {
  connectionSteps,
  issueTypes,
  messageItems,
  navItems,
  supportEntries,
} from './data/content'
import {
  createNewUserBindTicket,
  createBroadbandPasswordResetTicket,
  ApiError,
  fetchUserTickets,
  loginUser,
  registerUser,
  updateProfile,
} from './lib/api'

const USER_STORAGE_KEY = 'ad-vue-user'

const activeView = ref('home')
const authMode = ref('login')
const isAuthLoading = ref(false)
const isBinding = ref(false)
const isTicketsLoading = ref(false)
const isSavingProfile = ref(false)
const notifications = ref([])
const tickets = ref([])
const currentUser = ref(loadLocalJson(USER_STORAGE_KEY, null))
const profileModalOpen = ref(false)
const avatarMenuOpen = ref(false)
const selectedIssueId = ref(issueTypes[0].id)
const redirectAfterAuth = ref('home')

const homeForm = reactive({
  studentId: '',
  campusPhone: '',
})

const latestSubmission = reactive({
  studentId: '',
  phone: '',
  ticketNo: '',
})

const type3SmsContent = ref('')
const isSubmittingType3 = ref(false)
const bindCooldownRemaining = ref(0)
let bindCooldownTimerId = null

const authForm = reactive({
  loginPhone: '',
  loginPassword: '',
  registerPhone: '',
  registerPassword: '',
  registerConfirmPassword: '',
})

const editForm = reactive({
  name: '',
  studentId: '',
  phone: '',
})

const selectedIssue = computed(() => {
  return issueTypes.find((item) => item.id === selectedIssueId.value) ?? issueTypes[0]
})

const ticketStats = computed(() => {
  return {
    total: tickets.value.length,
    pending: tickets.value.filter((ticket) => ticket.status === 0).length,
    queue: tickets.value.filter((ticket) => ticket.status === 1).length,
    done: tickets.value.filter((ticket) => ticket.status === 3).length,
  }
})

onMounted(async () => {
  const initialView = window.location.hash.replace('#', '')
  if (navItems.some((item) => item.id === initialView)) {
    activeView.value = initialView
  }

  syncEditForm()

  if (currentUser.value?.id) {
    await refreshTickets()
  }

  if (activeView.value === 'tickets' && !currentUser.value) {
    activeView.value = 'auth'
    authMode.value = 'login'
  }
})

watch(currentUser, (value) => {
  if (value) {
    localStorage.setItem(USER_STORAGE_KEY, JSON.stringify(value))
    syncEditForm()
  } else {
    localStorage.removeItem(USER_STORAGE_KEY)
    clearEditForm()
    tickets.value = []
  }
}, { deep: true })

watch(activeView, (value) => {
  if (value === 'auth') return
  const nextHash = `#${value}`
  if (window.location.hash !== nextHash) {
    window.history.replaceState(null, '', nextHash)
  }
})

function selectView(viewId) {
  avatarMenuOpen.value = false

  if (viewId === 'tickets' && !currentUser.value) {
    redirectAfterAuth.value = 'tickets'
    activeView.value = 'auth'
    authMode.value = 'login'
    pushToast('请先登录后再查看工单', 'info')
    return
  }

  activeView.value = viewId
}

async function handleLogin() {
  const phone = authForm.loginPhone.trim()
  const password = authForm.loginPassword.trim()

  if (!phone || !password) {
    pushToast('请输入手机号和密码', 'warning')
    return
  }
  if (!isValidMainlandPhone(phone)) {
    pushToast('请输入正确的11位手机号', 'warning')
    return
  }

  isAuthLoading.value = true
  try {
    const user = await loginUser({ phone, password })
    currentUser.value = user
    authForm.loginPassword = ''
    activeView.value = redirectAfterAuth.value
    pushToast('登录成功', 'success')
    await refreshTickets()
  } catch (error) {
    pushToast(error.message || '登录失败', 'error')
  } finally {
    isAuthLoading.value = false
  }
}

async function handleRegister() {
  const phone = authForm.registerPhone.trim()
  const password = authForm.registerPassword.trim()
  const confirmPassword = authForm.registerConfirmPassword.trim()

  if (!phone || !password || !confirmPassword) {
    pushToast('请完整填写注册信息', 'warning')
    return
  }
  if (!isValidMainlandPhone(phone)) {
    pushToast('请输入正确的11位手机号', 'warning')
    return
  }
  if (password !== confirmPassword) {
    pushToast('两次密码不一致', 'warning')
    return
  }

  isAuthLoading.value = true
  try {
    const user = await registerUser({ phone, password, confirmPassword })
    currentUser.value = user
    authForm.registerPhone = ''
    authForm.registerPassword = ''
    authForm.registerConfirmPassword = ''
    authForm.loginPhone = user.phone
    activeView.value = redirectAfterAuth.value
    pushToast('注册成功', 'success')
    await refreshTickets()
  } catch (error) {
    pushToast(error.message || '注册失败', 'error')
  } finally {
    isAuthLoading.value = false
  }
}

async function handleBindSubmit() {
  if (!currentUser.value?.id) {
    redirectAfterAuth.value = 'home'
    activeView.value = 'auth'
    authMode.value = 'login'
    pushToast('请先登录后再提交工单', 'info')
    return
  }

  const studentId = homeForm.studentId.trim()
  const phone = homeForm.campusPhone.trim()

  if (!studentId) {
    pushToast('请输入学号', 'warning')
    return
  }
  if (!phone) {
    pushToast('请输入新办的电信校园卡手机号', 'warning')
    return
  }
  if (!isValidMainlandPhone(phone)) {
    pushToast('请输入正确的11位手机号', 'warning')
    return
  }

  isBinding.value = true
  try {
    const ticket = await createNewUserBindTicket({
      userId: currentUser.value.id,
      studentId,
      phone,
    })

    latestSubmission.studentId = ticket.studentId || studentId
    latestSubmission.phone = ticket.phone || phone
    latestSubmission.ticketNo = ticket.ticketNo || ''
    homeForm.studentId = ''
    homeForm.campusPhone = ''
    tickets.value = [ticket, ...tickets.value.filter((item) => item.id !== ticket.id)]
    const refreshedTickets = await refreshTickets()
    if (!isSubmittedTicketVisible(refreshedTickets, ticket)) {
      pushToast('提交返回成功，但刷新后未看到该工单，请检查后端接口地址或数据库连接', 'warning')
      return
    }
    startBindCooldown()
    pushToast('绑定工单已提交', 'success')
  } catch (error) {
    if (error instanceof ApiError && error.status === 429) {
      startBindCooldown()
      pushToast(error.message || '多次提交请等待一分钟', 'warning')
    } else {
      pushToast(error.message || '绑定失败', 'error')
    }
  } finally {
    isBinding.value = false
  }
}

async function handleSaveProfile() {
  if (!currentUser.value?.id) {
    pushToast('请先登录', 'warning')
    return
  }

  const name = editForm.name.trim()
  const studentId = editForm.studentId.trim()
  const phone = editForm.phone.trim()
  if (!name || !studentId || !phone) {
    pushToast('请完整填写资料', 'warning')
    return
  }
  if (!isValidMainlandPhone(phone)) {
    pushToast('请输入正确的11位手机号', 'warning')
    return
  }

  isSavingProfile.value = true
  try {
    const user = await updateProfile(currentUser.value.id, {
      phone,
      name,
      studentId,
    })
    currentUser.value = user
    pushToast('资料已保存', 'success')
    profileModalOpen.value = false
    await refreshTickets()
  } catch (error) {
    pushToast(error.message || '保存失败', 'error')
  } finally {
    isSavingProfile.value = false
  }
}

async function handleType3Submit() {
  if (!currentUser.value?.id) {
    redirectAfterAuth.value = 'submit'
    activeView.value = 'auth'
    authMode.value = 'login'
    pushToast('请先登录后再提交工单', 'info')
    return
  }

  const studentId = currentUser.value.studentId?.trim() || ''
  const phone = currentUser.value.phone?.trim() || ''
  const smsContent = type3SmsContent.value.trim()

  if (!studentId) {
    pushToast('请先在个人中心完善学号', 'warning')
    return
  }
  if (!phone) {
    pushToast('请先在个人中心完善手机号', 'warning')
    return
  }
  if (!smsContent) {
    pushToast('请先粘贴短信内容', 'warning')
    return
  }

  const broadbandAccount = parseBroadbandAccount(smsContent)
  const newPassword = parseBroadbandPassword(smsContent)

  if (!broadbandAccount || !newPassword) {
    pushToast('短信内容识别失败，请检查是否为完整模板', 'error')
    return
  }

  isSubmittingType3.value = true
  try {
    const ticket = await createBroadbandPasswordResetTicket({
      userId: currentUser.value.id,
      studentId,
      phone,
      broadbandAccount,
      newPassword,
    })

    type3SmsContent.value = ''
    latestSubmission.ticketNo = ticket.ticketNo || latestSubmission.ticketNo
    tickets.value = [ticket, ...tickets.value.filter((item) => item.id !== ticket.id)]
    const refreshedTickets = await refreshTickets()
    if (!isSubmittedTicketVisible(refreshedTickets, ticket)) {
      pushToast('提交返回成功，但刷新后未看到该工单，请检查后端接口地址或数据库连接', 'warning')
      return
    }
    pushToast('宽带密码重置工单已提交', 'success')
  } catch (error) {
    pushToast(error.message || '工单提交失败', 'error')
  } finally {
    isSubmittingType3.value = false
  }
}

async function refreshTickets() {
  if (!currentUser.value?.id) return []

  isTicketsLoading.value = true
  try {
    tickets.value = await fetchUserTickets(currentUser.value.id)
    return tickets.value
  } catch (error) {
    pushToast(error.message || '工单加载失败', 'error')
    return null
  } finally {
    isTicketsLoading.value = false
  }
}

function isSubmittedTicketVisible(refreshedTickets, ticket) {
  if (!Array.isArray(refreshedTickets) || !ticket?.id) return false
  return refreshedTickets.some((item) => item.id === ticket.id)
}

function startBindCooldown(seconds = 60) {
  bindCooldownRemaining.value = Math.max(bindCooldownRemaining.value, seconds)
  if (bindCooldownTimerId) return

  bindCooldownTimerId = window.setInterval(() => {
    bindCooldownRemaining.value = Math.max(0, bindCooldownRemaining.value - 1)
    if (bindCooldownRemaining.value === 0) {
      window.clearInterval(bindCooldownTimerId)
      bindCooldownTimerId = null
    }
  }, 1000)
}

function openProfileModal() {
  if (!currentUser.value) {
    redirectAfterAuth.value = activeView.value === 'auth' ? 'home' : activeView.value
    activeView.value = 'auth'
    authMode.value = 'login'
    return
  }

  syncEditForm()
  avatarMenuOpen.value = false
  profileModalOpen.value = true
}

function handleLogout() {
  currentUser.value = null
  profileModalOpen.value = false
  avatarMenuOpen.value = false
  activeView.value = 'auth'
  authMode.value = 'login'
  redirectAfterAuth.value = 'home'
  pushToast('已退出登录', 'info')
}

function syncEditForm() {
  if (!currentUser.value) {
    clearEditForm()
    return
  }

  editForm.name = currentUser.value.name || ''
  editForm.studentId = currentUser.value.studentId || ''
  editForm.phone = currentUser.value.phone || ''
}

function clearEditForm() {
  editForm.name = ''
  editForm.studentId = ''
  editForm.phone = ''
}

async function handleCopySupport(entry) {
  try {
    await copyText(entry.value)
    pushToast(`已复制${entry.copyLabel}`, 'success')
  } catch {
    pushToast(`复制${entry.copyLabel}失败，请手动复制`, 'error')
  }
}

function pushToast(message, tone = 'info') {
  const id = `${Date.now()}-${Math.random().toString(36).slice(2, 7)}`
  notifications.value = [...notifications.value, { id, message, tone }]
  window.setTimeout(() => {
    notifications.value = notifications.value.filter((item) => item.id !== id)
  }, 2800)
}

function removeToast(id) {
  notifications.value = notifications.value.filter((item) => item.id !== id)
}

function isValidMainlandPhone(value) {
  return /^1[3-9]\d{9}$/.test(value)
}

function parseBroadbandAccount(message) {
  const match = /为(\d+)的/.exec(message)
  return match?.[1] || null
}

function parseBroadbandPassword(message) {
  const match = /重置为(\d{6})/.exec(message)
  return match?.[1]?.length === 6 ? match[1] : null
}

function loadLocalJson(key, fallbackValue) {
  try {
    const raw = localStorage.getItem(key)
    return raw ? JSON.parse(raw) : fallbackValue
  } catch {
    return fallbackValue
  }
}

onUnmounted(() => {
  if (bindCooldownTimerId) {
    window.clearInterval(bindCooldownTimerId)
  }
})

async function copyText(value) {
  if (navigator.clipboard?.writeText) {
    await navigator.clipboard.writeText(value)
    return
  }

  const textarea = document.createElement('textarea')
  textarea.value = value
  textarea.setAttribute('readonly', '')
  textarea.style.position = 'absolute'
  textarea.style.left = '-9999px'
  document.body.appendChild(textarea)
  textarea.select()
  document.execCommand('copy')
  document.body.removeChild(textarea)
}
</script>

<template>
  <AuthScreen
    v-if="activeView === 'auth'"
    :mode="authMode"
    :form-state="authForm"
    :is-loading="isAuthLoading"
    @set-mode="authMode = $event"
    @login="handleLogin"
    @register="handleRegister"
  />

  <div v-else class="campus-net-prototype">
    <TopNav
      :items="navItems"
      :active-view="activeView"
      :current-user="currentUser"
      :ticket-count="ticketStats.total"
      @select-view="selectView"
      @open-profile="openProfileModal"
      @logout="handleLogout"
      @toggle-avatar-menu="avatarMenuOpen = !avatarMenuOpen"
      :avatar-menu-open="avatarMenuOpen"
    />

    <HomePage
      v-if="activeView === 'home'"
      :home-form="homeForm"
      :is-binding="isBinding"
      :bind-cooldown-remaining="bindCooldownRemaining"
      :latest-submission="latestSubmission"
      :connection-steps="connectionSteps"
      @submit-bind="handleBindSubmit"
      @go-submit="selectView('submit')"
      @go-tickets="selectView('tickets')"
    />

    <SubmitPage
      v-else-if="activeView === 'submit'"
      :issue-types="issueTypes"
      :selected-issue="selectedIssue"
      :support-entries="supportEntries"
      :current-user="currentUser"
      :type3-sms-content="type3SmsContent"
      :is-submitting-type3="isSubmittingType3"
      @select-issue="selectedIssueId = $event"
      @copy-support="handleCopySupport"
      @update:type3SmsContent="type3SmsContent = $event"
      @submit-type3="handleType3Submit"
      @go-home="selectView('home')"
      @go-auth="redirectAfterAuth = 'submit'; activeView = 'auth'; authMode = 'login'"
    />

    <TicketsPage
      v-else-if="activeView === 'tickets'"
      :current-user="currentUser"
      :tickets="tickets"
      :is-loading="isTicketsLoading"
      :ticket-stats="ticketStats"
      @refresh="refreshTickets"
      @go-submit="selectView('submit')"
      @open-profile="openProfileModal"
    />

    <ConsultPage
      v-else
      :message-items="messageItems"
      :support-entries="supportEntries"
      @copy-support="handleCopySupport"
      @go-submit="selectView('submit')"
    />

    <ProfileModal
      :visible="profileModalOpen"
      :current-user="currentUser"
      :edit-form="editForm"
      :is-saving="isSavingProfile"
      @close="profileModalOpen = false"
      @save="handleSaveProfile"
    />
  </div>

  <ToastStack :items="notifications" @dismiss="removeToast" />
</template>
