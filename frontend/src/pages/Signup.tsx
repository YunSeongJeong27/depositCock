import { useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { api, errorMessage } from '../api'
import { useAuth } from '../auth'
import { ErrorBox } from '../components/Ui'
import { accountText as T } from '../texts/account'
import type { Me } from '../types'

/**
 * 회원가입 (2026-11-20 아이디·휴대폰·이메일 인증 추가).
 * 이메일로 받은 인증번호를 확인해야 가입된다. 인증 뒤 이메일을 고치면 다시 인증해야 한다
 */
export default function Signup() {
  const { setMe } = useAuth()
  const navigate = useNavigate()
  const [form, setForm] = useState({ loginId: '', password: '', displayName: '', email: '', phone: '' })
  const [code, setCode] = useState('')
  /** 인증번호를 보낸 이메일 / 인증을 마친 이메일 */
  const [sentTo, setSentTo] = useState<string | null>(null)
  const [sentNote, setSentNote] = useState<string | null>(null)
  const [verifiedEmail, setVerifiedEmail] = useState<string | null>(null)
  const [agree, setAgree] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [codeError, setCodeError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  const verified = verifiedEmail !== null && verifiedEmail === form.email.trim()

  async function sendCode() {
    setCodeError(null)
    setSentNote(null)
    setBusy(true)
    try {
      const email = form.email.trim()
      const res = await api.post<{ validMinutes: number }>('/api/auth/signup/email-code', { email })
      setSentTo(email)
      setVerifiedEmail(null)
      setCode('')
      setSentNote(T.codeSent(res.validMinutes))
    } catch (err) {
      setCodeError(errorMessage(err))
    } finally {
      setBusy(false)
    }
  }

  async function verifyCode() {
    if (!sentTo) return
    setCodeError(null)
    setBusy(true)
    try {
      await api.post('/api/auth/signup/email-verify', { email: sentTo, code })
      setVerifiedEmail(sentTo)
      setSentNote(null)
      setError(null) // 인증 전에 가입을 눌러 뜬 "이메일 인증을 먼저" 안내가 남지 않게
    } catch (err) {
      setCodeError(errorMessage(err))
    } finally {
      setBusy(false)
    }
  }

  async function submit(e: FormEvent) {
    e.preventDefault()
    if (!verified) {
      setError(T.verifyEmailFirst)
      return
    }
    if (!agree) {
      setError(T.agreeRequired)
      return
    }
    setBusy(true)
    setError(null)
    try {
      const me = await api.post<Me>('/api/auth/signup', form)
      setMe(me)
      // 가입 직후 첫 화면은 "주문서 링크 만들기" (정리본 1장 제안)
      navigate('/seller/group-buys/new?welcome=1')
    } catch (err) {
      setError(errorMessage(err))
    } finally {
      setBusy(false)
    }
  }

  const codeOpen = sentTo !== null && sentTo === form.email.trim() && !verified

  return (
    <div className="container narrow">
      <h1 className="mt"><Link to="/" style={{ color: 'inherit' }}>📌 입금콕</Link> 회원가입</h1>
      <form className="card" onSubmit={submit}>
        <label className="field"><span>아이디</span>
          <input type="text" value={form.loginId} onChange={(e) => setForm({ ...form, loginId: e.target.value })} required
            autoComplete="username" autoCapitalize="none" maxLength={20} />
          <span className="muted small" style={{ fontWeight: 400 }}>{T.loginIdHint}</span>
        </label>
        <label className="field"><span>비밀번호 (8자 이상)</span>
          <input type="password" minLength={8} value={form.password} onChange={(e) => setForm({ ...form, password: e.target.value })} required autoComplete="new-password" />
        </label>
        <label className="field"><span>판매자 이름</span>
          <input type="text" value={form.displayName} onChange={(e) => setForm({ ...form, displayName: e.target.value })} required placeholder="예: 달콤 과일가게" />
          <span className="muted small" style={{ fontWeight: 400 }}>{T.displayNameHint}</span>
        </label>
        <div className="field">
          <label htmlFor="signup-email">이메일</label>
          <div className="row">
            {/* 이메일·인증번호 칸의 엔터는 가입이 아니라 인증번호 받기·확인으로 */}
            <input id="signup-email" type="email" value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} required
              autoComplete="email" style={{ flex: 1, minWidth: 200 }}
              onKeyDown={(e) => { if (e.key === 'Enter' && !verified) { e.preventDefault(); if (form.email.trim() && !busy) sendCode() } }} />
            {verified
              ? <span className="badge badge-PAID">인증 완료</span>
              : <button type="button" className="btn" disabled={busy || !form.email.trim()} onClick={sendCode}>
                  {sentTo === form.email.trim() ? '인증번호 다시 받기' : '인증번호 받기'}
                </button>}
          </div>
          {codeOpen && (
            <div className="row mt">
              <input type="text" inputMode="numeric" autoComplete="one-time-code" placeholder="인증번호 6자리" maxLength={7}
                aria-label="인증번호" value={code} onChange={(e) => setCode(e.target.value)} style={{ flex: 1, minWidth: 140 }}
                onKeyDown={(e) => { if (e.key === 'Enter') { e.preventDefault(); if (code.trim() && !busy) verifyCode() } }} />
              <button type="button" className="btn primary" disabled={busy || !code.trim()} onClick={verifyCode}>확인</button>
            </div>
          )}
          {sentNote && codeOpen && <span className="muted small" style={{ fontWeight: 400 }}>{sentNote}</span>}
          {verified && <span className="muted small" style={{ fontWeight: 400 }}>{T.emailVerified}</span>}
          <ErrorBox message={codeError} />
        </div>
        <label className="field"><span>휴대폰 번호</span>
          <input type="tel" value={form.phone} onChange={(e) => setForm({ ...form, phone: e.target.value })} required
            placeholder="010-0000-0000" autoComplete="tel" />
          <span className="muted small" style={{ fontWeight: 400 }}>{T.phoneHint}</span>
        </label>
        <label className="check">
          <input type="checkbox" checked={agree} onChange={(e) => setAgree(e.target.checked)} />
          <span><Link to="/terms" target="_blank">이용약관</Link>과 <Link to="/privacy" target="_blank">개인정보처리방침</Link>에 동의합니다.</span>
        </label>
        <ErrorBox message={error} />
        <button className="btn primary block" disabled={busy}>가입하고 첫 달 무료로 시작</button>
      </form>
      <p className="muted">이미 계정이 있나요? <Link to="/login">로그인</Link></p>
    </div>
  )
}
