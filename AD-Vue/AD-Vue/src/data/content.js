export const navItems = [
  { id: 'home', title: '首页' },
  { id: 'submit', title: '提交工单' },
  { id: 'tickets', title: '我的工单' },
  { id: 'consult', title: '在线咨询' },
]

export const connectionSteps = [
  {
    id: '第一步',
    icon: '📶',
    badge: '第一步',
    title: '连接校园网',
    highlight: '连接 WIFI：AHUT-wifi6',
    description:
      '先连接学校提供的无线网络，保持设备处于校园网环境中，再进入登录页面。使用移动数据或其它无线网无法完成登录。',
  },
  {
    id: '第二步',
    icon: '🔐',
    badge: '第二步',
    title: '登录校园网',
    highlight: '输入学号和密码，账号密码与智慧校园一致，并选择中国电信。',
    description:
      '正常连接后会弹出登录通知；如果没有弹出，可以在浏览器输入 10.255.255.154 进入登录页面。',
  },
  {
    id: '第三步',
    icon: '✅',
    badge: '第三步',
    title: '登录成功',
    highlight: '登录成功后即可正常上网。',
    description:
      '看到登录成功提示后，就可以在宿舍、教学楼和公共区域正常访问校园网。',
  },
]

export const issueTypes = [
  {
    id: 1,
    icon: '📶',
    mode: 'info',
    name: '连上 wifi 无法上网',
    description: '已连接校园网无线信号，但页面无法正常打开或无法访问外网。',
    method:
      '进入登录界面点击注销选择中国电信并登录，显示成功即可正常上网，显示别的可以查看下方处理方法',
  },
  {
    id: 2,
    icon: '💳',
    mode: 'info',
    name: '显示运营商账户欠费',
    description: '登录页面提示运营商账户欠费、套餐到期或余额不足。',
    method: '去查看话费余额是否正常，若充值后依旧无法上网请联系工作人员。',
  },
  {
    id: 3,
    icon: '🔐',
    mode: 'reset',
    name: '运营商宽带账户错误或宽带密码错误',
    description: '宽带密码错误需要重置，请按流程获取短信后填写宽带账号和新密码。',
    method:
      '用校园卡打 10000 号说重置宽带密码，然后把短信复制粘贴到该文本框并提交。这个新密码不是校园网登录密码。',
  },
  {
    id: 4,
    icon: '👤',
    mode: 'bind',
    name: '账户不存在',
    description: '登录页提示账号不存在，需要查看人工补充的处理指引。',
    method: '去首页填写学号和电信校园卡号码，点击立即绑定。',
  },
  {
    id: 5,
    icon: '📱',
    mode: 'info',
    name: '显示设备终端超出或者终端 IP 已在线',
    description: '旧设备未退出或终端占用异常，查看说明后可直接提交工单处理。',
    method:
      '可以打10000号转人工，和客服说清除设备，也可以在掌上安工大查看登录设备并清除无关设备。',
  },
  {
    id: 6,
    icon: '📞',
    mode: 'info',
    name: '显示请联系运营商解绑',
    description: '登录页面明确提示需联系运营商解绑，点击查看说明。',
    method: '联系工作人员。',
  },
  {
    id: 7,
    icon: '🎁',
    mode: 'info',
    name: '会员领取',
    description: '查看会员领取说明，并按说明领取相关权益。',
    method: '下载翼支付，在权益处领取，部分套餐无会员权益',
  },
  {
    id: 8,
    icon: '🧩',
    mode: 'info',
    name: '其它问题或者未解决',
    description: '未命中以上场景时，可先查看补充说明。',
    method: '请联系工作人员',
  },
]

export const messageItems = [
  {
    id: 'system',
    title: '系统通知',
    detail: '后端维护、远程自动化运行状态和平台更新说明会集中放在这里。',
  },
  {
    id: 'order',
    title: '订单消息',
    detail: '工单提交后会在这里提示状态变化，也方便桌面端统一回看处理进度。',
  },
  {
    id: 'activity',
    title: '活动提醒',
    detail: '与校园网套餐、会员领取和服务窗口相关的提醒会归到这一类。',
  },
]

export const supportEntries = [
  {
    id: 'qq',
    label: '售后 QQ 群号',
    value: '待填写',
    copyLabel: 'QQ 群号',
  },
  {
    id: 'wechat',
    label: '工作人员联系方式(WeChat)',
    value: 'WuWude-MrWang',
    copyLabel: '微信号',
  },
]

export function ticketTypeText(ticketType) {
  switch (ticketType) {
    case 1:
      return '新用户绑定'
    case 2:
      return '账户不存在'
    case 3:
      return '宽带账号或密码错误'
    default:
      return `工单类型 ${ticketType}`
  }
}

export function ticketStatusText(status) {
  switch (status) {
    case 0:
      return '待处理'
    case 1:
      return '排队中'
    case 2:
      return '处理中'
    case 3:
      return '已完成'
    default:
      return '待处理'
  }
}

export function ticketStatusClass(status) {
  switch (status) {
    case 0:
      return 'status-pending'
    case 1:
      return 'status-queue'
    case 2:
      return 'status-doing'
    case 3:
      return 'status-done'
    default:
      return 'status-pending'
  }
}

export function formatTicketTime(value) {
  if (!value) return '待同步'
  return value.replace('T', ' ').split('.')[0]
}
