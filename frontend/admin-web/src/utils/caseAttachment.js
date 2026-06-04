/** 案件附件分组（处置照片按上传时间批次） */

export function formatUploadTime(value) {
  if (!value) return '—'
  const s = String(value)
  return s.length >= 19 ? s.slice(0, 19).replace('T', ' ') : s.replace('T', ' ')
}

export function parseUploadTime(value) {
  if (!value) return 0
  const t = new Date(String(value).replace(' ', 'T')).getTime()
  return Number.isNaN(t) ? 0 : t
}

export function isHandleFinishAttachment(a) {
  return a?.nodeCode === 'handle_finish'
}

export function isReportCaseAttachment(a) {
  const code = a?.nodeCode
  if (!code) return true
  if (code === 'reported') return true
  return code !== 'handle_finish'
}

/** 同一批上传（间隔 1 分钟内）归为一组，返回从旧到新 */
export function groupHandleAttachments(attachments) {
  const list = (attachments || [])
    .filter(isHandleFinishAttachment)
    .slice()
    .sort((a, b) => parseUploadTime(a.createTime) - parseUploadTime(b.createTime))

  const batches = []
  for (const item of list) {
    const t = parseUploadTime(item.createTime)
    let batch = batches[batches.length - 1]
    if (!batch || t - batch.endTime > 60000) {
      batch = {
        timeKey: `${t}-${item.id}`,
        timeLabel: formatUploadTime(item.createTime),
        endTime: t,
        items: []
      }
      batches.push(batch)
    } else {
      batch.endTime = t
    }
    batch.items.push(item)
  }
  return batches
}

export function handlePhotoSectionTitle(timeLabel) {
  return `处置照片 · ${timeLabel}`
}
