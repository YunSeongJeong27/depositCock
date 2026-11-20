package com.depositcock.seller;

import java.util.Locale;
import java.util.regex.Pattern;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * 로그인 아이디 규칙 (2026-11-20): 영문 소문자로 시작하는 영문 소문자·숫자·밑줄 4~20자.
 * 대문자로 적어도 소문자로 바꿔 저장·비교한다 ("Demo" 로 가입하면 "demo").
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class LoginIds {

    private static final Pattern RULE = Pattern.compile("^[a-z][a-z0-9_]{3,19}$");

    /** 앞뒤 공백을 빼고 소문자로. 규칙에 안 맞으면 null */
    public static String normalize(String raw) {
        if (raw == null) {
            return null;
        }
        String id = raw.trim().toLowerCase(Locale.ROOT);
        return RULE.matcher(id).matches() ? id : null;
    }
}
