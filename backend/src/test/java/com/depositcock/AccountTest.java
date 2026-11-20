package com.depositcock;

import java.util.Map;

import com.depositcock.support.IntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 가입 이메일 인증, 아이디 로그인, 아이디 찾기, 비밀번호 찾기·변경, 관리자의 아이디·비밀번호 변경 (2026-11-20).
 * 메일은 TestMailbox 가 받고 테스트가 인증번호·아이디·임시 비밀번호를 읽는다.
 */
class AccountTest extends IntegrationTest {

    private static final String PHONE = "010-1234-5678";

    @Autowired
    JdbcTemplate jdbc;

    private Map<String, Object> signupBody(String loginId, String email) {
        return Map.of("loginId", loginId, "password", "password123", "displayName", "공구장", "email", email,
                "phone", PHONE);
    }

    private void sendSignupCode(String email) throws Exception {
        mvc.perform(withJson(post("/api/auth/signup/email-code"), Map.of("email", email)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.validMinutes").value(10));
    }

    private void expire(String column, String email) {
        jdbc.update("update email_verification set " + column + " = " + column + " - INTERVAL '2' HOUR where email = ?",
                email);
    }

    @Test
    void signupNeedsVerifiedEmailCode() throws Exception {
        String email = uniqueEmail();
        String loginId = uniqueLoginId();
        // 인증번호 없이 가입 → 막힘
        mvc.perform(withJson(post("/api/auth/signup"), signupBody(loginId, email)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("EMAIL_NOT_VERIFIED"));
        // 받기 전 확인 → 먼저 받으라고
        mvc.perform(withJson(post("/api/auth/signup/email-verify"), Map.of("email", email, "code", "123456")))
                .andExpect(jsonPath("$.code").value("EMAIL_CODE_NOT_SENT"));

        sendSignupCode(email);
        String code = mailbox.lastCodeTo(email);
        assertThat(mailbox.lastBodyTo(email)).contains("가입 인증번호").contains("10분 안에 입력해 주세요.");
        // 1분 안에 다시 받기 → 막힘
        mvc.perform(withJson(post("/api/auth/signup/email-code"), Map.of("email", email)))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("EMAIL_CODE_TOO_SOON"));
        // 받기만 하고 확인 안 함 → 가입 막힘
        mvc.perform(withJson(post("/api/auth/signup"), signupBody(loginId, email)))
                .andExpect(jsonPath("$.code").value("EMAIL_NOT_VERIFIED"));
        // 틀린 인증번호
        String wrong = code.equals("000000") ? "111111" : "000000";
        mvc.perform(withJson(post("/api/auth/signup/email-verify"), Map.of("email", email, "code", wrong)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("EMAIL_CODE_WRONG"));
        // 맞는 인증번호 (가운데 띄어쓰기도 됨)
        mvc.perform(withJson(post("/api/auth/signup/email-verify"),
                Map.of("email", email.toUpperCase(), "code", code.substring(0, 3) + " " + code.substring(3))))
                .andExpect(status().isOk());

        // 아이디 형식이 틀린 가입은 인증을 쓰지 않는다 → 고쳐서 다시 가입하면 됨
        mvc.perform(withJson(post("/api/auth/signup"), signupBody("1abc", email)))
                .andExpect(jsonPath("$.code").value("LOGIN_ID_INVALID"));
        mvc.perform(withJson(post("/api/auth/signup"), Map.of("loginId", loginId, "password", "password123",
                        "displayName", "공구장", "email", email, "phone", "02-123-4567")))
                .andExpect(jsonPath("$.code").value("PHONE_INVALID"));
        mvc.perform(withJson(post("/api/auth/signup"), signupBody(loginId.toUpperCase(), email)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.loginId").value(loginId))
                .andExpect(jsonPath("$.phone").value(PHONE));
        // 아이디는 대문자로 적어도 로그인됨. 이메일로는 로그인 안 됨
        login(loginId.toUpperCase(), "password123");
        mvc.perform(withJson(post("/api/auth/login"), Map.of("loginId", email, "password", "password123")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("LOGIN_FAILED"));
    }

    @Test
    void wrongCodeFiveTimesLocksAndExpiredCodeIsRejected() throws Exception {
        String email = uniqueEmail();
        sendSignupCode(email);
        String code = mailbox.lastCodeTo(email);
        String wrong = code.equals("000000") ? "111111" : "000000";
        for (int i = 0; i < 4; i++) {
            mvc.perform(withJson(post("/api/auth/signup/email-verify"), Map.of("email", email, "code", wrong)))
                    .andExpect(jsonPath("$.code").value("EMAIL_CODE_WRONG"));
        }
        mvc.perform(withJson(post("/api/auth/signup/email-verify"), Map.of("email", email, "code", wrong)))
                .andExpect(jsonPath("$.code").value("EMAIL_CODE_LOCKED"));
        // 5번 틀린 뒤에는 맞는 번호도 안 됨 (틀린 횟수는 오류가 나도 저장됨)
        mvc.perform(withJson(post("/api/auth/signup/email-verify"), Map.of("email", email, "code", code)))
                .andExpect(jsonPath("$.code").value("EMAIL_CODE_LOCKED"));

        // 다시 받으면 새 번호로 됨. 단 유효 시간(10분)이 지나면 안 됨
        expire("sent_at", email);
        sendSignupCode(email);
        String second = mailbox.lastCodeTo(email);
        expire("expires_at", email);
        mvc.perform(withJson(post("/api/auth/signup/email-verify"), Map.of("email", email, "code", second)))
                .andExpect(jsonPath("$.code").value("EMAIL_CODE_EXPIRED"));

        // 확인하고 30분이 지나면 가입에 못 씀
        expire("sent_at", email);
        sendSignupCode(email);
        mvc.perform(withJson(post("/api/auth/signup/email-verify"),
                Map.of("email", email, "code", mailbox.lastCodeTo(email)))).andExpect(status().isOk());
        expire("verified_at", email);
        mvc.perform(withJson(post("/api/auth/signup"), signupBody(uniqueLoginId(), email)))
                .andExpect(jsonPath("$.code").value("EMAIL_NOT_VERIFIED"));
    }

    @Test
    void findLoginIdSendsIdToSignupEmail() throws Exception {
        String email = uniqueEmail();
        signup(email);
        mvc.perform(withJson(post("/api/auth/find-login-id"), Map.of("email", email, "phone", "010-9999-9999")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ACCOUNT_NOT_FOUND"));
        mvc.perform(withJson(post("/api/auth/find-login-id"), Map.of("email", uniqueEmail(), "phone", PHONE)))
                .andExpect(jsonPath("$.code").value("ACCOUNT_NOT_FOUND"));
        mvc.perform(withJson(post("/api/auth/find-login-id"), Map.of("email", email, "phone", "01012345678")))
                .andExpect(status().isOk());
        assertThat(mailbox.lastBodyTo(email)).contains("아이디: " + loginIdOf(email));
    }

    @Test
    void passwordResetNeedsCodeThenMailsTempPassword() throws Exception {
        String email = uniqueEmail();
        signup(email);
        String loginId = loginIdOf(email);
        Map<String, Object> account = Map.of("loginId", loginId, "email", email, "phone", PHONE);

        // 아이디가 다르면 찾지 못함
        mvc.perform(withJson(post("/api/auth/password/email-code"),
                        Map.of("loginId", uniqueLoginId(), "email", email, "phone", PHONE)))
                .andExpect(jsonPath("$.code").value("ACCOUNT_NOT_FOUND"));
        // 인증번호 확인 없이 임시 비밀번호 → 막힘 (남이 정보만 알고 비밀번호를 바꿔 버리지 못하게)
        mvc.perform(withJson(post("/api/auth/password/reset"), account))
                .andExpect(jsonPath("$.code").value("EMAIL_NOT_VERIFIED"));
        // 가입 인증번호는 비밀번호에 못 씀 (용도가 다름)
        mvc.perform(withJson(post("/api/auth/password/email-code"), account)).andExpect(status().isOk());
        String code = mailbox.lastCodeTo(email);
        assertThat(mailbox.lastBodyTo(email)).contains("비밀번호 찾기·변경 인증번호");
        mvc.perform(withJson(post("/api/auth/signup/email-verify"), Map.of("email", email, "code", code)))
                .andExpect(jsonPath("$.code").value("EMAIL_CODE_NOT_SENT"));
        mvc.perform(withJson(post("/api/auth/password/email-verify"),
                Map.of("loginId", loginId, "email", email, "phone", PHONE, "code", code))).andExpect(status().isOk());

        // 메일을 못 보내면 비밀번호는 그대로 (인증도 남아서 다시 하면 됨)
        mailbox.failNextTo(email);
        mvc.perform(withJson(post("/api/auth/password/reset"), account))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code").value("MAIL_SEND_FAILED"));
        login(loginId, "password123");

        mvc.perform(withJson(post("/api/auth/password/reset"), account)).andExpect(status().isOk());
        String body = mailbox.lastBodyTo(email);
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("임시 비밀번호: ([a-z0-9]{10})").matcher(body);
        assertThat(m.find()).isTrue();
        String temp = m.group(1);
        login(loginId, temp);
        mvc.perform(withJson(post("/api/auth/login"), Map.of("loginId", loginId, "password", "password123")))
                .andExpect(status().isUnauthorized());
        // 인증은 한 번만 쓴다
        mvc.perform(withJson(post("/api/auth/password/reset"), account))
                .andExpect(jsonPath("$.code").value("EMAIL_NOT_VERIFIED"));
    }

    @Test
    void passwordChangeAfterCode() throws Exception {
        String email = uniqueEmail();
        signup(email);
        String loginId = loginIdOf(email);
        Map<String, Object> change = Map.of("loginId", loginId, "email", email, "phone", PHONE,
                "newPassword", "newpass456");
        mvc.perform(withJson(post("/api/auth/password/change"), change))
                .andExpect(jsonPath("$.code").value("EMAIL_NOT_VERIFIED"));
        mvc.perform(withJson(post("/api/auth/password/email-code"),
                Map.of("loginId", loginId, "email", email, "phone", PHONE))).andExpect(status().isOk());
        mvc.perform(withJson(post("/api/auth/password/email-verify"), Map.of("loginId", loginId, "email", email,
                "phone", PHONE, "code", mailbox.lastCodeTo(email)))).andExpect(status().isOk());
        mvc.perform(withJson(post("/api/auth/password/change"), Map.of("loginId", loginId, "email", email,
                        "phone", PHONE, "newPassword", "short")))
                .andExpect(jsonPath("$.code").value("PASSWORD_LENGTH"));
        mvc.perform(withJson(post("/api/auth/password/change"), change)).andExpect(status().isOk());
        login(loginId, "newpass456");
    }

    @Test
    void profilePhoneCanChange() throws Exception {
        String email = uniqueEmail();
        Cookie seller = signup(email);
        mvc.perform(withJson(put("/api/seller/profile").cookie(seller), Map.of("displayName", "공구장",
                        "phone", "1234")))
                .andExpect(jsonPath("$.code").value("PHONE_INVALID"));
        mvc.perform(withJson(put("/api/seller/profile").cookie(seller), Map.of("displayName", "공구장",
                        "phone", "010 5555 6666")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phone").value("010-5555-6666"));
        // 바뀐 번호로 아이디 찾기
        mvc.perform(withJson(post("/api/auth/find-login-id"), Map.of("email", email, "phone", "010-5555-6666")))
                .andExpect(status().isOk());
    }

    @Test
    void adminSeesIdPhoneAndChangesIdAndPassword() throws Exception {
        String email = uniqueEmail();
        Cookie seller = signup(email);
        Cookie admin = login("admin", "admin1234");
        long sellerId = 0;
        for (JsonNode s : read(mvc.perform(get("/api/admin/sellers").cookie(admin)).andReturn())) {
            if (s.get("email").asText().equals(email)) {
                sellerId = s.get("id").asLong();
                assertThat(s.get("loginId").asText()).isEqualTo(loginIdOf(email));
                assertThat(s.get("phone").asText()).isEqualTo(PHONE);
                assertThat(s.has("passwordHash")).isFalse();
            }
        }
        assertThat(sellerId).isPositive();

        // 판매자는 관리자 기능을 못 씀
        mvc.perform(withJson(put("/api/admin/sellers/" + sellerId + "/password").cookie(seller),
                Map.of("password", "hacked123"))).andExpect(status().isForbidden());

        // 아이디: 형식·중복 검사
        mvc.perform(withJson(put("/api/admin/sellers/" + sellerId + "/login-id").cookie(admin),
                Map.of("loginId", "x"))).andExpect(jsonPath("$.code").value("LOGIN_ID_INVALID"));
        mvc.perform(withJson(put("/api/admin/sellers/" + sellerId + "/login-id").cookie(admin),
                Map.of("loginId", "admin"))).andExpect(jsonPath("$.code").value("LOGIN_ID_TAKEN"));
        String newId = uniqueLoginId();
        mvc.perform(withJson(put("/api/admin/sellers/" + sellerId + "/login-id").cookie(admin),
                Map.of("loginId", newId))).andExpect(status().isOk());
        // 비밀번호: 이메일 인증 없이 바로
        mvc.perform(withJson(put("/api/admin/sellers/" + sellerId + "/password").cookie(admin),
                Map.of("password", "short"))).andExpect(jsonPath("$.code").value("PASSWORD_LENGTH"));
        mvc.perform(withJson(put("/api/admin/sellers/" + sellerId + "/password").cookie(admin),
                Map.of("password", "byadmin789"))).andExpect(status().isOk());
        login(newId, "byadmin789");
        mvc.perform(withJson(post("/api/auth/login"), Map.of("loginId", loginIdOf(email), "password", "password123")))
                .andExpect(status().isUnauthorized());
    }
}
