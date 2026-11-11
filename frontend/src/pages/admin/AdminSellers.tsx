import { useEffect, useRef, useState, type FormEvent } from 'react'
import { api, errorMessage } from '../../api'
import { ErrorBox } from '../../components/Ui'
import { accountText as T } from '../../texts/account'
import type { Plan, Role } from '../../types'

interface Row {
  id: number
  email: string
  displayName: string
  /** 휴대폰 번호 010-1234-5678 모양, 없으면 null (2026-11-09) */
  phone: string | null
  role: Role
  plan: Plan
  trialEndsAt: string
  paidUntil: string | null
  expired: boolean
  suspended: boolean
  adminMemo: string | null
  createdAt: string
  groupBuyCount: number
  captureToday: number
  captureThisMonth: number
  dailyCaptureLimit: number
  customCaptureLimit: boolean
  /** 판매자별 댓글 주문 기능 허용 */
  commentFeatureEnabled: boolean
}

/** 서비스 전체 설정 (/api/admin/settings) */
interface Settings {
  commentFeatureOpen: boolean
  signupOpen: boolean
  /** 입금 대조 "은행앱 화면 캡처로 올리기" (2026-11-09) */
  depositCaptureOpen: boolean
}

/** 이메일이 칸보다 길면 "@" 앞에서 줄을 바꾼다 (글자 중간에서 끊기지 않게) */
const emailWithBreak = (email: string) => {
  const at = email.indexOf('@')
  return at > 0 ? <>{email.slice(0, at)}<wbr />{email.slice(at)}</> : email
}

/**
 * 판매자 계정 만들기·정보 수정 칸 (2026-11-09). 메일 등으로 요청받은 계정을 관리자가 직접 만들고, 잘못 넣은 정보를 고친다.
 * 수정할 때 비밀번호는 바꿀 때만 적는다. 관리자 계정 이메일은 서버 설정으로 정해서 못 바꾼다
 */
function SellerInfoForm({ target, onDone, onClose }: { target: Row | null; onDone: (message: string) => void; onClose: () => void }) {
  const [form, setForm] = useState({
    email: target?.email ?? '', password: '', displayName: target?.displayName ?? '', phone: target?.phone ?? '',
  })
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)
  const ref = useRef<HTMLFormElement>(null)
  // 표 아래쪽 줄에서 [정보 수정]을 눌러도 칸이 보이게. 중괄호로 감싸서 아무것도 돌려주지 않는다
  // (요즘 브라우저는 scrollIntoView 가 Promise 를 돌려줘서, 그대로 돌려주면 React 가 닫을 때 정리 함수로 불러 오류가 남)
  useEffect(() => {
    ref.current?.scrollIntoView({ block: 'nearest', behavior: 'smooth' })
  }, [])
  const adminEmail = target?.role === 'ADMIN'

  async function submit(e: FormEvent) {
    e.preventDefault()
    setBusy(true)
    setError(null)
    try {
      if (target) {
        await api.put(`/api/admin/sellers/${target.id}/info`,
          { email: form.email, displayName: form.displayName, phone: form.phone, newPassword: form.password || null })
        onDone(T.sellerInfoSaved)
      } else {
        await api.post('/api/admin/sellers', form)
        onDone(T.sellerCreated)
      }
    } catch (err) {
      setError(errorMessage(err))
    } finally {
      setBusy(false)
    }
  }

  return (
    <form className="card" ref={ref} onSubmit={submit}>
      <h3>{target ? '판매자 정보 수정' : '판매자 계정 만들기'}</h3>
      <p className="muted small">{target ? T.editSellerHelp : T.createSellerHelp}</p>
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '0 12px' }}>
        <label className="field"><span>이메일</span>
          <input type="email" value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} required
            disabled={adminEmail} title={adminEmail ? T.adminEmailFixed : undefined} />
        </label>
        <label className="field"><span>{target ? '새 비밀번호 (바꿀 때만)' : '비밀번호 (8자 이상)'}</span>
          <input type="text" minLength={8} value={form.password} onChange={(e) => setForm({ ...form, password: e.target.value })}
            required={!target} autoComplete="off" />
        </label>
        <label className="field"><span>판매자 이름</span>
          <input type="text" value={form.displayName} onChange={(e) => setForm({ ...form, displayName: e.target.value })} required />
        </label>
        <label className="field"><span>휴대폰 번호</span>
          <input type="tel" value={form.phone} onChange={(e) => setForm({ ...form, phone: e.target.value })} required={!target} placeholder="010-0000-0000" />
        </label>
      </div>
      <ErrorBox message={error} />
      <div className="row">
        <button className="btn primary" disabled={busy}>{target ? '저장' : '만들기'}</button>
        <button type="button" className="btn" onClick={onClose}>닫기</button>
      </div>
    </form>
  )
}

/** 슈퍼 관리자: 판매자 목록, 플랜(무료 체험/유료)·유료 만료일·사용 제한 수동 변경, 계정 만들기·정보 수정 */
export default function AdminSellers() {
  const [rows, setRows] = useState<Row[]>([])
  const [editing, setEditing] = useState<Record<number, Row>>({})
  const [error, setError] = useState<string | null>(null)
  const [filter, setFilter] = useState<'all' | 'expired' | 'suspended'>('all')
  /** 서비스 전체 설정: 댓글 주문 기능 전체 공개(꺼져 있으면 아래에서 허용한 판매자만 씀), 새 회원가입 받기 */
  const [settings, setSettings] = useState<Settings | null>(null)
  /** 계정 만들기·정보 수정 칸: 닫힘 null, 만들기 'new', 수정이면 그 판매자 */
  const [infoTarget, setInfoTarget] = useState<Row | 'new' | null>(null)
  const [notice, setNotice] = useState<string | null>(null)

  const load = () => api.get<Row[]>('/api/admin/sellers').then(setRows).catch((e) => setError(errorMessage(e)))
  useEffect(() => {
    load()
    api.get<Settings>('/api/admin/settings').then(setSettings).catch((e) => setError(errorMessage(e)))
  }, [])

  /** 보낸 설정만 바뀐다 */
  async function changeSetting(change: Partial<Settings>, confirmText: string) {
    if (!confirm(confirmText)) return
    setError(null)
    try {
      setSettings(await api.put<Settings>('/api/admin/settings', change))
    } catch (e) {
      setError(errorMessage(e))
    }
  }

  async function save(r: Row) {
    setError(null)
    try {
      await api.put(`/api/admin/sellers/${r.id}`, {
        plan: r.plan, paidUntil: r.paidUntil || null, trialEndsAt: r.trialEndsAt || null, suspended: r.suspended, adminMemo: r.adminMemo,
        // 기본값을 쓰던 판매자는 제한을 바꾸지 않았으면 그대로 기본값(null) 유지
        dailyCaptureLimit: r.customCaptureLimit ? r.dailyCaptureLimit : null,
        commentFeatureEnabled: r.commentFeatureEnabled,
      })
      setEditing((e) => {
        const next = { ...e }
        delete next[r.id]
        return next
      })
      load()
    } catch (e) {
      setError(errorMessage(e))
    }
  }

  async function remove(r: Row) {
    if (!confirm(T.confirmRemoveSeller(r.email))) return
    try {
      await api.del(`/api/admin/sellers/${r.id}`)
      load()
    } catch (e) {
      setError(errorMessage(e))
    }
  }

  const shown = rows.filter((r) => filter === 'all' || (filter === 'expired' ? r.expired : r.suspended))

  return (
    <div>
      <h1>판매자 관리</h1>
      <p className="muted small">{T.captureLimitHelp}</p>
      <p className="muted small">{T.planHelp}</p>
      <div className="card">
        <div className="row between">
          <div>
            <b>댓글 주문 기능</b> <span className="small muted">· {T.commentFeatureNote}</span>
            <div className="small muted">
              {settings === null ? '불러오는 중…' : settings.commentFeatureOpen
                ? T.commentOpenAll
                : T.commentClosed}
            </div>
          </div>
          {settings !== null && (
            <label className="check" style={{ whiteSpace: 'nowrap' }}>
              <input type="checkbox" checked={settings.commentFeatureOpen}
                onChange={(e) => changeSetting({ commentFeatureOpen: e.target.checked }, e.target.checked ? T.confirmCommentOpen : T.confirmCommentClose)} /> 전체 공개
            </label>
          )}
        </div>
      </div>
      <div className="card">
        <div className="row between">
          <div>
            <b>회원가입</b>
            <div className="small muted">
              {settings === null ? '불러오는 중…' : settings.signupOpen ? T.signupOpenNow : T.signupClosedNow}
            </div>
          </div>
          {settings !== null && (
            <label className="check" style={{ whiteSpace: 'nowrap' }}>
              <input type="checkbox" checked={settings.signupOpen}
                onChange={(e) => changeSetting({ signupOpen: e.target.checked }, e.target.checked ? T.confirmSignupOpen : T.confirmSignupClose)} /> 가입 받기
            </label>
          )}
        </div>
      </div>
      <div className="card">
        <div className="row between">
          <div>
            <b>은행앱 화면 캡처로 올리기</b> <span className="small muted">· {T.depositCaptureNote}</span>
            <div className="small muted">
              {settings === null ? '불러오는 중…' : settings.depositCaptureOpen ? T.depositCaptureOnNow : T.depositCaptureOffNow}
            </div>
          </div>
          {settings !== null && (
            <label className="check" style={{ whiteSpace: 'nowrap' }}>
              <input type="checkbox" checked={settings.depositCaptureOpen}
                onChange={(e) => changeSetting({ depositCaptureOpen: e.target.checked }, e.target.checked ? T.confirmDepositCaptureOpen : T.confirmDepositCaptureClose)} /> 사용
            </label>
          )}
        </div>
      </div>
      {infoTarget !== null && (
        <SellerInfoForm key={infoTarget === 'new' ? 'new' : infoTarget.id} target={infoTarget === 'new' ? null : infoTarget}
          onClose={() => setInfoTarget(null)}
          onDone={(message) => {
            setInfoTarget(null)
            setNotice(message)
            load()
          }} />
      )}
      <div className="row between mb">
        <select value={filter} onChange={(e) => setFilter(e.target.value as typeof filter)} style={{ width: 200 }}>
          <option value="all">전체 ({rows.length})</option>
          <option value="expired">기간 만료 ({rows.filter((r) => r.expired).length})</option>
          <option value="suspended">사용 제한 ({rows.filter((r) => r.suspended).length})</option>
        </select>
        <button className="btn" onClick={() => { setNotice(null); setInfoTarget('new') }}>+ 계정 만들기</button>
      </div>
      {notice && <div className="success-box">{notice}</div>}
      <ErrorBox message={error} />
      <div className="table-wrap">
        <table className="stack admin-table">
          <thead>
            <tr><th>판매자</th><th>휴대폰</th><th>플랜</th><th>무료 체험 끝</th><th>유료 만료일</th><th>상태</th><th>캡처 사용량·하루 제한</th><th>메모</th><th></th></tr>
          </thead>
          <tbody>
            {shown.map((row) => {
              const r = editing[row.id] ?? row
              const isEditing = Boolean(editing[row.id])
              const update = (patch: Partial<Row>) => setEditing((e) => ({ ...e, [row.id]: { ...r, ...patch } }))
              return (
                <tr key={row.id}>
                  <td className="stack-title">{row.displayName}<div className="muted small">{emailWithBreak(row.email)}{row.role === 'ADMIN' && ' · 관리자'}</div>
                    <div className="muted small">가입 {row.createdAt.substring(0, 10)} · 공구 {row.groupBuyCount}개</div></td>
                  <td data-label="휴대폰" style={{ whiteSpace: 'nowrap' }}>{row.phone ?? <span className="muted">-</span>}</td>
                  <td data-label="플랜">
                    <select value={r.plan} onChange={(e) => update({ plan: e.target.value as Plan })} disabled={row.role === 'ADMIN'} style={{ minWidth: 120 }}>
                      <option value="TRIAL">무료 체험</option>
                      <option value="PAID">유료</option>
                    </select>
                  </td>
                  <td data-label="무료 체험 끝"><input type="date" className="date-compact" value={r.trialEndsAt.substring(0, 10)} onChange={(e) => {
                    // 날짜를 지우면 'T23:59:59' 만 보내져 저장이 400 으로 실패하므로 무시한다 (무료 체험 종료일은 필수)
                    if (e.target.value) update({ trialEndsAt: e.target.value + 'T23:59:59' })
                  }} disabled={row.role === 'ADMIN'} /></td>
                  <td data-label="유료 만료일"><input type="date" className="date-compact" value={r.paidUntil ?? ''} onChange={(e) => update({ paidUntil: e.target.value })} disabled={row.role === 'ADMIN'} /></td>
                  {/* 폰 카드에서는 칸 이름 옆이 좁아서, 체크 두 개·사용량과 입력칸을 한 덩어리로 묶어 넘치면 다음 줄로 보낸다 */}
                  <td data-label="상태">
                    <div className="row" style={{ gap: '0 12px' }}>
                      {row.expired && <div className="badge badge-CLOSED">만료</div>}
                      <label className="check small" style={{ whiteSpace: 'nowrap' }}><input type="checkbox" checked={r.suspended} onChange={(e) => update({ suspended: e.target.checked })} disabled={row.role === 'ADMIN'} /> 사용 제한</label>
                      <label className="check small" style={{ whiteSpace: 'nowrap' }} title={T.commentFeatureTitle}>
                        <input type="checkbox" checked={r.commentFeatureEnabled} onChange={(e) => update({ commentFeatureEnabled: e.target.checked })} disabled={row.role === 'ADMIN'} /> 댓글 기능
                      </label>
                    </div>
                  </td>
                  <td data-label="캡처">
                    <div className="row" style={{ gap: '4px 10px' }}>
                      <div className="small" style={{ whiteSpace: 'nowrap' }}>오늘 {row.captureToday} / 이번 달 {row.captureThisMonth}장</div>
                      <input type="number" min={0} className="capture-limit" aria-label="캡처 하루 제한"
                        value={r.customCaptureLimit ? r.dailyCaptureLimit : ''}
                        placeholder="기본값"
                        onChange={(e) => update(e.target.value === ''
                          ? { customCaptureLimit: false }
                          : { customCaptureLimit: true, dailyCaptureLimit: Number(e.target.value) })} />
                    </div>
                  </td>
                  <td data-label="메모"><input type="text" value={r.adminMemo ?? ''} onChange={(e) => update({ adminMemo: e.target.value })} placeholder="입금 기록 등" style={{ minWidth: 120 }} /></td>
                  <td className="stack-action">
                    {isEditing && <button className="btn small primary" onClick={() => save(r)}>저장</button>}
                    <button className="btn small" onClick={() => { setNotice(null); setInfoTarget(row) }}>정보 수정</button>
                    {row.role !== 'ADMIN' && <button className="btn small danger" onClick={() => remove(row)}>탈퇴</button>}
                  </td>
                </tr>
              )
            })}
          </tbody>
        </table>
      </div>
    </div>
  )
}
