package com.depositcock;

import java.util.HashMap;
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
 * 판매자 휴대폰 번호(가입 때 받음, 인증 없음), 관리자가 판매자 계정을 직접 만들기·정보 고치기 (2026-11-09 사용자 요청).
 * 2026-11-20: 아이디 추가, 관리자 [정보 수정]에서 아이디도, 관리자 자신의 이메일도 바꿈. 내 정보 휴대폰 변경은 이메일 인증 (AccountTest)
 */
class SellerPhoneAndAdminAccountTest extends IntegrationTest {

    @Autowired
    JdbcTemplate jdbc;

    private static Map<String, Object> account(String loginId, String email, String phone) {
        Map<String, Object> body = new HashMap<>();
        body.put("loginId", loginId);
        body.put("email", email);
        body.put("password", "password123");
        body.put("displayName", "공구장");
        body.put("phone", phone);
        return body;
    }

    private JsonNode adminRow(Cookie admin, String email) throws Exception {
        for (JsonNode row : read(mvc.perform(get("/api/admin/sellers").cookie(admin)).andReturn())) {
            if (row.get("email").asText().equals(email)) {
                return row;
            }
        }
        throw new AssertionError("관리자 목록에 없음: " + email);
    }

    @Test
    void signupNeedsPhoneAndShowsItFormatted() throws Exception {
        mvc.perform(withJson(post("/api/auth/signup"), account(uniqueLoginId(), uniqueEmail(), null)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("PHONE_REQUIRED"));
        mvc.perform(withJson(post("/api/auth/signup"), account(uniqueLoginId(), uniqueEmail(), "02-123-4567")))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("PHONE_INVALID"));

        String email = uniqueEmail();
        String loginId = uniqueLoginId();
        verifySignupEmail(email);
        mvc.perform(withJson(post("/api/auth/signup"), account(loginId, email, "010 9876 5432")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.phone").value("010-9876-5432"));
        Cookie seller = login(loginId, "password123");
        Cookie admin = login("admin", "admin1234");
        assertThat(adminRow(admin, email).get("phone").asText()).isEqualTo("010-9876-5432");
        assertThat(adminRow(admin, email).get("loginId").asText()).isEqualTo(loginId);

        // 내 정보 저장으로는 휴대폰이 안 바뀜 (2026-11-20: 이메일 인증번호를 넣어야 바뀜, AccountTest.phoneChangeNeedsEmailCode)
        mvc.perform(withJson(put("/api/seller/profile").cookie(seller), Map.of("displayName", "공구장")))
                .andExpect(jsonPath("$.phone").value("010-9876-5432"));
        mvc.perform(withJson(put("/api/seller/profile").cookie(seller), Map.of("displayName", "공구장", "phone", "")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.phone").value("010-9876-5432"));
    }

    @Test
    void adminCreatesSellerEvenWhenSignupIsClosed() throws Exception {
        Cookie admin = login("admin", "admin1234");
        mvc.perform(withJson(put("/api/admin/settings").cookie(admin), Map.of("signupOpen", false))).andExpect(status().isOk());

        // 관리자가 만드는 계정은 이메일 인증번호 없이 바로 만들어짐
        String email = uniqueEmail();
        String loginId = uniqueLoginId();
        mvc.perform(withJson(post("/api/admin/sellers").cookie(admin), account(loginId, email, "010-5555-0001")))
                .andExpect(status().isOk());
        // 만든 계정으로 바로 로그인, 무료 체험으로 시작
        Cookie seller = login(loginId, "password123");
        mvc.perform(get("/api/me").cookie(seller))
                .andExpect(jsonPath("$.role").value("SELLER"))
                .andExpect(jsonPath("$.plan").value("TRIAL"))
                .andExpect(jsonPath("$.loginId").value(loginId))
                .andExpect(jsonPath("$.phone").value("010-5555-0001"));

        // 같은 이메일·같은 아이디·잘못된 아이디·잘못된 번호, 판매자가 부르면 막힘
        mvc.perform(withJson(post("/api/admin/sellers").cookie(admin), account(uniqueLoginId(), email.toUpperCase(), "010-5555-0002")))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("EMAIL_TAKEN"));
        mvc.perform(withJson(post("/api/admin/sellers").cookie(admin), account(loginId.toUpperCase(), uniqueEmail(), "010-5555-0002")))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("LOGIN_ID_TAKEN"));
        mvc.perform(withJson(post("/api/admin/sellers").cookie(admin), account("ab", uniqueEmail(), "010-5555-0002")))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("LOGIN_ID_INVALID"));
        mvc.perform(withJson(post("/api/admin/sellers").cookie(admin), account(uniqueLoginId(), uniqueEmail(), "1234")))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("PHONE_INVALID"));
        mvc.perform(withJson(post("/api/admin/sellers").cookie(seller), account(uniqueLoginId(), uniqueEmail(), "010-5555-0003")))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminFixesSellerInfoIncludingLoginId() throws Exception {
        String email = uniqueEmail();
        Cookie seller = signup(email);
        String loginId = loginIdOf(email);
        Cookie admin = login("admin", "admin1234");
        JsonNode row = adminRow(admin, email);
        long id = row.get("id").asLong();
        assertThat(row.has("passwordHash")).isFalse();
        String path = "/api/admin/sellers/" + id + "/info";

        String newEmail = uniqueEmail();
        String newId = uniqueLoginId();
        Map<String, Object> info = new HashMap<>();
        info.put("loginId", newId.toUpperCase());
        info.put("email", newEmail);
        info.put("displayName", "바뀐 이름");
        info.put("phone", "010-7777-8888");
        info.put("newPassword", "newpass1234");
        mvc.perform(withJson(put(path).cookie(admin), info)).andExpect(status().isOk());

        // 전에 로그인해 둔 쿠키는 그대로 쓰이고(토큰에 이메일·아이디가 안 들어감) 바뀐 정보가 보인다
        mvc.perform(get("/api/me").cookie(seller))
                .andExpect(jsonPath("$.loginId").value(newId))
                .andExpect(jsonPath("$.email").value(newEmail))
                .andExpect(jsonPath("$.displayName").value("바뀐 이름"))
                .andExpect(jsonPath("$.phone").value("010-7777-8888"));
        // 새 아이디·새 비밀번호로 로그인, 옛 아이디·옛 비밀번호는 안 됨
        login(newId, "newpass1234");
        mvc.perform(withJson(post("/api/auth/login"), Map.of("loginId", loginId, "password", "newpass1234")))
                .andExpect(status().isUnauthorized());
        mvc.perform(withJson(post("/api/auth/login"), Map.of("loginId", newId, "password", "password123")))
                .andExpect(status().isUnauthorized());
        // 바뀐 이메일·휴대폰으로 아이디 찾기
        mvc.perform(withJson(post("/api/auth/find-login-id"), Map.of("email", newEmail, "phone", "010-7777-8888")))
                .andExpect(status().isOk());
        assertThat(mailbox.lastBodyTo(newEmail)).contains("아이디: " + newId);

        // 새 비밀번호를 비우면 비밀번호는 그대로, 휴대폰을 비우면 지움
        info.put("loginId", newId);
        info.put("newPassword", null);
        info.put("phone", "");
        info.put("displayName", "다시 바꿈");
        mvc.perform(withJson(put(path).cookie(admin), info)).andExpect(status().isOk());
        login(newId, "newpass1234");
        mvc.perform(get("/api/me").cookie(seller)).andExpect(jsonPath("$.phone").isEmpty());

        // 다른 판매자가 쓰는 이메일·아이디로는 못 바꿈, 아이디 형식·짧은 새 비밀번호는 막힘
        String other = uniqueEmail();
        signup(other);
        info.put("email", other);
        mvc.perform(withJson(put(path).cookie(admin), info))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("EMAIL_TAKEN"));
        info.put("email", newEmail);
        info.put("loginId", loginIdOf(other));
        mvc.perform(withJson(put(path).cookie(admin), info))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("LOGIN_ID_TAKEN"));
        info.put("loginId", "1bad");
        mvc.perform(withJson(put(path).cookie(admin), info))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("LOGIN_ID_INVALID"));
        info.put("loginId", newId);
        info.put("newPassword", "short");
        mvc.perform(withJson(put(path).cookie(admin), info))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("PASSWORD_LENGTH"));

        // 판매자는 못 부름
        info.put("newPassword", null);
        mvc.perform(withJson(put(path).cookie(seller), info)).andExpect(status().isForbidden());
    }

    /**
     * 관리자 자신의 이메일도 바꿀 수 있다 (2026-11-20, 전에는 ADMIN_EMAIL_FIXED 로 막았음).
     * 앱이 켜질 때 관리자가 하나도 없을 때만 만들어서 바꿔도 관리자가 늘지 않는다 (AdminBootstrapRuleTest)
     */
    @Test
    void adminCanChangeOwnEmail() throws Exception {
        Cookie admin = login("admin", "admin1234");
        JsonNode me = read(mvc.perform(get("/api/me").cookie(admin)).andReturn());
        String path = "/api/admin/sellers/" + me.get("id").asLong() + "/info";
        Integer admins = jdbc.queryForObject("select count(*) from seller where role = 'ADMIN'", Integer.class);
        Map<String, Object> info = new HashMap<>();
        info.put("loginId", "admin");
        info.put("email", uniqueEmail());
        info.put("displayName", "관리자");
        try {
            mvc.perform(withJson(put(path).cookie(admin), info)).andExpect(status().isOk());
        } finally {
            // 다른 테스트가 쓰는 관리자 이메일로 되돌림
            info.put("email", "admin@test.local");
            mvc.perform(withJson(put(path).cookie(admin), info)).andExpect(status().isOk());
        }
        assertThat(jdbc.queryForObject("select count(*) from seller where role = 'ADMIN'", Integer.class)).isEqualTo(admins);
    }
}
