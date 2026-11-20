package com.depositcock.account;

/** 이메일 인증번호를 어디에 쓰는지 (2026-11-20) */
public enum VerificationPurpose {
    /** 가입: 이메일이 본인 것인지 */
    SIGNUP,
    /** 비밀번호 찾기(임시 비밀번호)·변경: 그 판매자의 가입 이메일을 받을 수 있는지 */
    PASSWORD,
    /** 로그인한 판매자의 휴대폰 번호 변경: 가입 이메일로 받은 인증번호를 넣어야 바뀜 */
    PHONE
}
