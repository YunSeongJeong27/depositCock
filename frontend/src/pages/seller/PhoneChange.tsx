import { useState, type FormEvent } from 'react'
import { api, errorMessage } from '../../api'
import { useAuth } from '../../auth'
import { ErrorBox } from '../../components/Ui'
import { accountText as T } from '../../texts/account'
import type { Me } from '../../types'

/**
 * 내 정보의 휴대폰 번호 변경 (2026-11-20 사용자 결정): 가입 이메일로 받은 인증번호를 넣어야 바뀐다.
 * [변경] → 새 번호 → [인증번호 받기] → 인증번호 → [변경하기]. 엔터는 지금 단계 버튼으로
 */
export default function PhoneChange() {
  const { me, setMe } = useAuth()
  const [open, setOpen] = useState(false)
  const [phone, setPhone] = useState('')
  const [code, setCode] = useState('')
  const [sentNote, setSentNote] = useState<string | null>(null)
  const [done, setDone] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  if (!me) return null

  function start() {
    setOpen(true)
    setDone(false)
    setPhone('')
    setCode('')
    setSentNote(null)
    setError(null)
  }

  async function run(action: () => Promise<void>) {
    setBusy(true)
    setError(null)
    try {
      await action()
    } catch (err) {
      setError(errorMessage(err))
    } finally {
      setBusy(false)
    }
  }

  const sendCode = () => run(async () => {
    const res = await api.post<{ validMinutes: number }>('/api/seller/phone/email-code')
    setCode('')
    setSentNote(T.codeSent(res.validMinutes))
  })

  const submit = (e: FormEvent) => {
    e.preventDefault()
    if (!sentNote) {
      sendCode()
      return
    }
    run(async () => {
      setMe(await api.put<Me>('/api/seller/phone', { phone, code }))
      setOpen(false)
      setDone(true)
    })
  }

  return (
    <div className="card">
      <h3>휴대폰 번호</h3>
      <div className="row between">
        <b>{me.phone ?? '없음'}</b>
        {!open && <button type="button" className="btn small" onClick={start}>변경</button>}
      </div>
      <div className="muted small">{T.phoneHint}</div>
      {done && <div className="success-box">{T.phoneChanged}</div>}
      {open && (
        <form className="mt" onSubmit={submit}>
          <p className="muted small">{T.phoneChangeHelp(me.email)}</p>
          <label className="field"><span>새 휴대폰 번호</span>
            <input type="tel" value={phone} onChange={(e) => setPhone(e.target.value)} required placeholder="010-0000-0000" autoComplete="tel" />
          </label>
          <div className="field">
            <label htmlFor="phone-code">인증번호</label>
            <div className="row">
              {sentNote ? <>
                <input id="phone-code" type="text" inputMode="numeric" autoComplete="one-time-code" placeholder="인증번호 6자리" maxLength={7}
                  value={code} onChange={(e) => setCode(e.target.value)} style={{ flex: 1, minWidth: 140 }} />
                <button type="submit" className="btn primary" disabled={busy || !code.trim() || !phone.trim()}>변경하기</button>
                <button type="button" className="btn" disabled={busy} onClick={sendCode}>인증번호 다시 받기</button>
              </> : <button type="submit" className="btn" disabled={busy || !phone.trim()}>인증번호 받기</button>}
            </div>
            {sentNote && <span className="muted small">{sentNote}</span>}
          </div>
          <ErrorBox message={error} />
          <button type="button" className="link-button" onClick={() => setOpen(false)}>취소</button>
        </form>
      )}
    </div>
  )
}
