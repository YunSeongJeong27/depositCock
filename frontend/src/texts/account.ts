import { SUPPORT_EMAIL } from './common'

/** 로그인·가입, 내 정보(알림·탈퇴), 관리자 화면의 긴 문장. 규칙은 common.ts 맨 위 */
export const accountText = {
  // 로그인·가입
  loginExpired: '로그인이 만료되었습니다. 다시 로그인해 주세요.',
  agreeRequired: '이용약관과 개인정보처리방침에 동의해 주세요.',
  displayNameHint: '고객 화면에 보이는 이름입니다.',
  sellerPhoneHint: '입금콕 운영자가 연락할 때만 씁니다.',
  signupClosed: `관리자가 잠시 회원가입을 비활성화 했습니다. ${SUPPORT_EMAIL} 으로 문의해주시면 바로 안내해드리겠습니다.`,

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
  createSellerHelp: '메일 등으로 요청받은 판매자 계정을 만듭니다. 회원가입을 꺼 둬도 만들 수 있습니다. 만든 뒤 이메일과 비밀번호를 판매자에게 알려 주세요.',
  editSellerHelp: '새 비밀번호는 바꿀 때만 적으세요. 이메일이나 비밀번호를 바꾸면 판매자는 다음 로그인부터 새 값으로 로그인합니다.',
  adminEmailFixed: '관리자 계정 이메일은 서버 설정 APP_ADMIN_EMAIL 로 정해서 여기서 바꿀 수 없습니다.',
  sellerCreated: '계정을 만들었습니다. 이메일과 비밀번호를 판매자에게 알려 주세요.',
  sellerInfoSaved: '판매자 정보를 저장했습니다. 비밀번호를 바꿨다면 판매자에게 새 비밀번호를 알려 주세요.',
  signupOpenNow: '새 판매자가 회원가입할 수 있습니다.',
  signupClosedNow: '새 판매자가 회원가입할 수 없습니다. 첫 화면·로그인·회원가입 화면의 가입 버튼이 꺼집니다. 이미 가입한 판매자는 그대로 씁니다.',
  confirmSignupOpen: '새 판매자 회원가입을 다시 받습니다. 계속할까요?',
  confirmSignupClose: '새 판매자 회원가입을 막습니다. 첫 화면·로그인·회원가입 화면의 가입 버튼이 꺼집니다. 이미 가입한 판매자는 그대로 씁니다. 계속할까요?',
}
