import { useEffect, useState, type FormEvent } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { api, errorMessage } from '../api'
import { useAuth } from '../auth'
import { Brand, ErrorBox } from '../components/Ui'
import { accountText as T } from '../texts/account'

type Tab = 'id' | 'reset' | 'change'
const TABS: { key: Tab; label: string }[] = [
  { key: 'id', label: '아이디 찾기' },
  { key: 'reset', label: '비밀번호 찾기' },
  { key: 'change', label: '비밀번호 변경' },
]

/**
 * 아이디 찾기 · 비밀번호 찾기 · 비밀번호 변경 (2026-11-20 사용자 결정). 로그인 없이 쓴다.
 * - 아이디 찾기: 가입 이메일 + 휴대폰이 맞으면 아이디를 메일로 바로 보냄
 * - 비밀번호 찾기·변경: 아이디 + 가입 이메일 + 휴대폰이 맞으면 인증번호를 메일로 → 확인 → 임시 비밀번호 메일 / 새 비밀번호
 * 로그인한 상태(내 정보의 [비밀번호 변경])로 오면 아이디·이메일·휴대폰을 미리 채운다
 */
export default function FindAccount() {
  const { me } = useAuth()
  const [params, setParams] = useSearchParams()
  const tab: Tab = (['id', 'reset', 'change'] as const).find((t) => t === params.get('tab')) ?? 'id'
  const [account, setAccount] = useState({ loginId: '', email: '', phone: '' })
  const [code, setCode] = useState('')
  const [codeSent, setCodeSent] = useState<string | null>(null)
  const [verified, setVerified] = useState(false)
  const [newPassword, setNewPassword] = useState('')
  const [newPassword2, setNewPassword2] = useState('')
  const [done, setDone] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  const meId = me?.id
  useEffect(() => {
    if (me) setAccount({ loginId: me.loginId, email: me.email, phone: me.phone ?? '' })
  }, [meId])

  function reset() {
    setCode('')
    setCodeSent(null)
    setVerified(false)
    setNewPassword('')
    setNewPassword2('')
    setDone(null)
    setError(null)
  }

  function choose(next: Tab) {
    reset()
    setParams({ tab: next }, { replace: true })
  }

  /** 아이디·이메일·휴대폰을 고치면 인증을 처음부터 다시 */
  function change(patch: Partial<typeof account>) {
    setAccount({ ...account, ...patch })
    if (codeSent || verified) reset()
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

  const findId = (e: FormEvent) => {
    e.preventDefault()
    run(async () => {
      await api.post('/api/auth/find-login-id', { email: account.email, phone: account.phone })
      setDone(T.findIdSent)
    })
  }

  const sendCode = () => run(async () => {
    const res = await api.post<{ validMinutes: number }>('/api/auth/password/email-code', account)
    setCode('')
    setVerified(false)
    setCodeSent(T.codeSent(res.validMinutes))
  })

  const verifyCode = () => run(async () => {
    await api.post('/api/auth/password/email-verify', { ...account, code })
    setVerified(true)
    setCodeSent(null)
  })

  const issueTemp = () => run(async () => {
    await api.post('/api/auth/password/reset', account)
    setDone(T.resetSent)
  })

  const changePassword = () => {
    if (newPassword !== newPassword2) {
      setError(T.passwordMismatch)
      return
    }
    run(async () => {
      await api.post('/api/auth/password/change', { ...account, newPassword })
      setDone(T.changeDone)
    })
  }

  /** 엔터를 누르면 지금 단계의 버튼: 인증번호 받기 → 확인 → 임시 비밀번호 받기 / 비밀번호 변경 */
  const submitPassword = (e: FormEvent) => {
    e.preventDefault()
    if (!verified) {
      if (codeSent && code.trim()) verifyCode()
      else sendCode()
    } else if (tab === 'change') {
      changePassword()
    } else {
      issueTemp()
    }
  }

  const accountFields = (
    <>
      <label className="field"><span>아이디</span>
        <input type="text" value={account.loginId} onChange={(e) => change({ loginId: e.target.value })} required autoComplete="username" autoCapitalize="none" />
      </label>
      <label className="field"><span>가입 이메일</span>
        <input type="email" value={account.email} onChange={(e) => change({ email: e.target.value })} required autoComplete="email" />
      </label>
      <label className="field"><span>휴대폰 번호</span>
        <input type="tel" value={account.phone} onChange={(e) => change({ phone: e.target.value })} required placeholder="010-0000-0000" autoComplete="tel" />
      </label>
    </>
  )

  return (
    <div className="container narrow">
      <h1 className="mt"><Link to="/" style={{ color: 'inherit' }}><Brand /></Link> 계정 찾기</h1>
      <div className="tabs">
        {TABS.map((t) => (
          <button key={t.key} type="button" className={tab === t.key ? 'active' : ''} onClick={() => choose(t.key)}>{t.label}</button>
        ))}
      </div>

      {done ? (
        <div className="card">
          <div className="success-box">{done}</div>
          <Link className="btn primary block" to={me ? '/seller/profile' : '/login'}>{me ? '내 정보로' : '로그인하러 가기'}</Link>
        </div>
      ) : tab === 'id' ? (
        <form className="card" onSubmit={findId}>
          <p className="muted small">{T.findIdHelp}</p>
          <label className="field"><span>가입 이메일</span>
            <input type="email" value={account.email} onChange={(e) => change({ email: e.target.value })} required autoComplete="email" />
          </label>
          <label className="field"><span>휴대폰 번호</span>
            <input type="tel" value={account.phone} onChange={(e) => change({ phone: e.target.value })} required placeholder="010-0000-0000" autoComplete="tel" />
          </label>
          <ErrorBox message={error} />
          <button className="btn primary block" disabled={busy}>아이디 메일로 받기</button>
        </form>
      ) : (
        <form className="card" onSubmit={submitPassword}>
          <p className="muted small">{tab === 'reset' ? T.resetHelp : T.changeHelp}</p>
          {accountFields}
          {!verified && (
            <div className="field">
              <label htmlFor="find-code">인증번호</label>
              {/* 지금 단계의 버튼만 제출 버튼으로 둬서 엔터가 그 버튼으로 동작하게 한다 (submitPassword).
                  폰에서 인증번호 칸과 [확인]이 한 줄에 오게 칸을 먼저, [다시 받기]를 뒤에 둔다 */}
              <div className="row">
                {codeSent ? <>
                  <input id="find-code" type="text" inputMode="numeric" autoComplete="one-time-code" placeholder="인증번호 6자리" maxLength={7}
                    value={code} onChange={(e) => setCode(e.target.value)} style={{ flex: 1, minWidth: 140 }} />
                  <button type="submit" className="btn primary" disabled={busy || !code.trim()}>확인</button>
                  <button type="button" className="btn" disabled={busy} onClick={sendCode}>인증번호 다시 받기</button>
                </> : <button type="submit" className="btn" disabled={busy || !account.loginId || !account.email || !account.phone}>인증번호 받기</button>}
              </div>
              {codeSent && <span className="muted small">{codeSent}</span>}
            </div>
          )}
          {verified && <div className="success-box">{T.emailVerified}</div>}
          {verified && tab === 'change' && <>
            <label className="field"><span>새 비밀번호 (8자 이상)</span>
              <input type="password" minLength={8} value={newPassword} onChange={(e) => setNewPassword(e.target.value)} required autoComplete="new-password" />
            </label>
            <label className="field"><span>새 비밀번호 확인</span>
              <input type="password" minLength={8} value={newPassword2} onChange={(e) => setNewPassword2(e.target.value)} required autoComplete="new-password" />
            </label>
          </>}
          <ErrorBox message={error} />
          {verified && (tab === 'reset'
            ? <button className="btn primary block" disabled={busy}>임시 비밀번호 메일로 받기</button>
            : <button className="btn primary block" disabled={busy}>비밀번호 변경</button>)}
        </form>
      )}
      <p className="muted small">{me ? <Link to="/seller/profile">내 정보로 돌아가기</Link> : <Link to="/login">로그인</Link>}</p>
    </div>
  )
}
