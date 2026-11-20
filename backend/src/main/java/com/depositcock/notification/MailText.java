package com.depositcock.notification;

import lombok.RequiredArgsConstructor;

/**
 * 판매자 알림·계정 메일 문구 (2026-10-20). 제목은 첫 줄과 같고 앞에 SUBJECT_PREFIX 가 붙는다.
 * 값이 들어가는 문장은 %s·%d 자리에 순서대로 넣는다.
 */
@RequiredArgsConstructor
public enum MailText {

    SUBJECT_PREFIX("[입금콕] "),
    GROUP_BUY("공구: %s"),
    OPEN_GROUP_BUY("공구 화면 열기"),
    SETTINGS_NOTE("알림은 [내 정보 → 알림 설정]에서 끄고 켤 수 있습니다."),
    STOCK_THRESHOLD_NOTE("알림 기준은 공구 수정 화면에서 상품마다 바꿀 수 있습니다."),

    NEW_ORDER("%d번 새 주문이 들어왔습니다."),
    CUSTOMER_CHANGED("%d번 주문을 고객이 수정했습니다."),
    CUSTOMER_CANCELLED("%d번 주문을 고객이 취소했습니다."),
    REFUND_REQUESTED("%d번 주문에 환불 신청이 들어왔습니다."),
    REFUND_ON_CONFIRMED("주문 확정된 주문입니다."),
    REFUND_ON_UNCONFIRMED("주문 확정 전 주문입니다. 입금 내역을 먼저 확인해 주세요."),
    REFUND_HOW_TO("주문 목록에서 [주문 취소 후 환불 진행] 또는 [환불 신청 취소]를 눌러 주세요. 환불 계좌는 판매자 화면에서 볼 수 있습니다."),
    AUTO_CLOSED("공구가 마감 시각이 돼 마감됐습니다."),
    AUTO_CLOSED_AT("마감 시각: %s"),

    // ---- 계정 메일 (2026-11-20): 가입 인증번호, 아이디 찾기, 비밀번호 찾기·변경 ----
    SIGNUP_CODE("가입 인증번호를 알려 드립니다."),
    PASSWORD_CODE("비밀번호 찾기·변경 인증번호를 알려 드립니다."),
    CODE_LINE("인증번호: %s"),
    CODE_VALID_MINUTES("%d분 안에 입력해 주세요."),
    LOGIN_ID_FOUND("아이디를 알려 드립니다."),
    LOGIN_ID_LINE("아이디: %s"),
    TEMP_PASSWORD("임시 비밀번호를 알려 드립니다."),
    TEMP_PASSWORD_LINE("임시 비밀번호: %s"),
    TEMP_PASSWORD_CHANGE("로그인한 뒤 [내 정보 → 비밀번호 변경]에서 새 비밀번호로 바꿔 주세요."),
    NOT_REQUESTED("요청하지 않았다면 이 메일은 무시해 주세요.");

    private final String format;

    public String text(Object... args) {
        return args.length == 0 ? format : String.format(format, args);
    }
}
