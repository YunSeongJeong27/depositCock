import { josa } from './common'

/** 로그인·가입, 아이디·비밀번호 찾기, 내 정보(알림·탈퇴), 관리자 화면의 긴 문장. 규칙은 common.ts 맨 위 */
export const accountText = {
  // 로그인·가입
  loginExpired: '로그인이 만료되었습니다. 다시 로그인해 주세요.',
  agreeRequired: '이용약관과 개인정보처리방침에 동의해 주세요.',
  displayNameHint: '고객 화면에 보이는 이름입니다.',
  loginIdHint: '영문 소문자로 시작해 영문 소문자·숫자·밑줄 4~20자',
  phoneHint: '아이디·비밀번호를 찾을 때 씁니다.',
  codeSent: (minutes: number) => `인증번호를 메일로 보냈습니다. ${minutes}분 안에 입력해 주세요. 메일이 안 보이면 스팸함도 확인해 주세요.`,
  emailVerified: '이메일 인증을 마쳤습니다.',
  verifyEmailFirst: '이메일 인증을 먼저 해 주세요.',

  // 아이디·비밀번호 찾기 (2026-11-20)
  findIdHelp: '가입할 때 적은 이메일과 휴대폰 번호를 입력하세요. 맞으면 그 이메일로 아이디를 보내 드립니다.',
  findIdSent: '가입한 이메일로 아이디를 보냈습니다. 메일이 안 보이면 스팸함도 확인해 주세요.',
  resetHelp: '아이디·가입 이메일·휴대폰 번호가 맞으면 이메일로 인증번호를 보내 드립니다. 인증번호를 확인하면 임시 비밀번호를 메일로 보내 드립니다.',
  resetSent: '가입한 이메일로 임시 비밀번호를 보냈습니다. 로그인한 뒤 비밀번호를 바꿔 주세요.',
  changeHelp: '아이디·가입 이메일·휴대폰 번호가 맞으면 이메일로 인증번호를 보내 드립니다. 인증번호를 확인한 뒤 새 비밀번호를 정하세요.',
  changeDone: '비밀번호를 바꿨습니다. 새 비밀번호로 로그인해 주세요.',
  passwordMismatch: '새 비밀번호와 비밀번호 확인이 다릅니다.',
  phoneChangeHelp: (email: string) => `인증번호는 가입 이메일 ${email} 주소로 보내 드립니다. 새 번호를 적고 받은 인증번호를 넣으면 바뀝니다.`,
  phoneChanged: '휴대폰 번호를 바꿨습니다.',

  // 내 정보
  withdrawPrompt: '탈퇴하면 모든 공구·주문·입금 정보가 삭제되고 되돌릴 수 없습니다. 필요한 주문 정리표는 먼저 내려받으세요.\n계속하려면 비밀번호를 입력하세요.',
  defaultAccountHint: '새 공구를 만들 때 이 계좌가 자동으로 채워집니다.',
  notifyHelp: '처음에는 꼭 필요한 알림만 켜져 있습니다. 체크하면 바로 저장됩니다.',
  screenNotifyNote: '화면 안내를 꺼도 주문 목록의 표시와 숫자는 그대로 보입니다.',
  withdrawNote: '탈퇴하면 판매자 정보와 받은 주문자·입금 정보가 모두 파기됩니다.',

  // 관리자
  confirmCommentOpen: '댓글 주문 기능을 모든 판매자에게 엽니다. 계속할까요?',
  confirmCommentClose: '댓글 주문 기능 전체 공개를 끕니다. 아래에서 따로 허용한 판매자만 쓸 수 있습니다. 계속할까요?',
  confirmRemoveSeller: (email: string) =>
    `${email} 판매자를 탈퇴 처리합니다. 이 판매자의 공구·주문·입금 정보가 모두 삭제됩니다. 계속할까요?`,
  captureLimitHelp: '캡처 하루 제한은 판매자별로 바꿀 수 있습니다. 비우면 기본값을 씁니다. 날짜는 한국 시간 기준입니다.',
  planHelp: '판매자가 이용료를 입금하면 플랜을 "유료"로 바꾸고 유료 만료일을 넣으세요. 기간이 끝나도 자동으로 막지 않습니다. 막아야 하면 "사용 제한"을 켜세요. 사용 제한 중에는 조회만 되고 고객 주문은 계속 받습니다.',
  commentFeatureNote: '댓글 캡처·집계·주문 반영',
  commentOpenAll: '모든 판매자에게 열려 있습니다.',
  commentClosed: '닫혀 있습니다. 아래 표에서 "댓글 기능"을 체크한 판매자만 씁니다. 주문 링크·입금 대조는 상관없이 씁니다.',
  commentFeatureTitle: '전체 공개가 꺼져 있을 때 이 판매자만 댓글 주문 기능을 쓰게 함',
  adminPasswordNote: '비밀번호는 되돌릴 수 없게 암호화해 저장해서 지금 비밀번호는 누구도 볼 수 없습니다. 새 비밀번호를 정하면 이메일 인증 없이 바로 바뀝니다.',
  confirmAdminLoginId: (from: string, to: string) => `아이디를 ${from}에서 ${to}${josa(to, '으로/로')} 바꿉니다. 판매자에게 새 아이디를 알려 주세요. 계속할까요?`,
  confirmAdminPassword: (loginId: string) => `${loginId} 계정의 비밀번호를 바꿉니다. 판매자에게 새 비밀번호를 알려 주세요. 계속할까요?`,
  adminInfoNote: '이메일·휴대폰은 인증 없이 바로 바뀝니다. 이메일을 바꾸면 알림·계정 메일이 새 이메일로 갑니다. 휴대폰을 비우면 그 판매자는 아이디·비밀번호 찾기를 못 합니다.',
  adminInfoSaved: '정보를 저장했습니다.',
  adminLoginIdChanged: '아이디를 바꿨습니다.',
  adminPasswordChanged: '비밀번호를 바꿨습니다.',
}
