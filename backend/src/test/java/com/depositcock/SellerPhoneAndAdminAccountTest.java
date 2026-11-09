package com.depositcock;

import java.util.HashMap;
import java.util.Map;

import com.depositcock.support.IntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 판매자 휴대폰 번호(가입 때 받음, 인증 없음), 관리자가 판매자 계정을 직접 만들기·정보 고치기 (2026-11-09 사용자 요청).
 */
class SellerPhoneAndAdminAccountTest extends IntegrationTest {

    private static Map<String, Object> account(String email, String phone) {
        Map<String, Object> body = new HashMap<>();
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
        mvc.perform(withJson(post("/api/auth/signup"), account(uniqueEmail(), null)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("PHONE_REQUIRED"));
        mvc.perform(withJson(post("/api/auth/signup"), account(uniqueEmail(), "02-123-4567")))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("PHONE_INVALID"));

        String email = uniqueEmail();
        mvc.perform(withJson(post("/api/auth/signup"), account(email, "010 9876 5432")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.phone").value("010-9876-5432"));
        Cookie seller = login(email, "password123");
        Cookie admin = login("admin@test.local", "admin1234");
        assertThat(adminRow(admin, email).get("phone").asText()).isEqualTo("010-9876-5432");

        // 내 정보: 휴대폰 번호를 안 보내면 그대로, 보내면 바뀜, 빈 값이면 지움
        mvc.perform(withJson(put("/api/seller/profile").cookie(seller), Map.of("displayName", "공구장")))
                .andExpect(jsonPath("$.phone").value("010-9876-5432"));
        mvc.perform(withJson(put("/api/seller/profile").cookie(seller), Map.of("displayName", "공구장", "phone", "01011112222")))
                .andExpect(jsonPath("$.phone").value("010-1111-2222"));
        mvc.perform(withJson(put("/api/seller/profile").cookie(seller), Map.of("displayName", "공구장", "phone", "123")))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("PHONE_INVALID"));
        mvc.perform(withJson(put("/api/seller/profile").cookie(seller), Map.of("displayName", "공구장", "phone", "")))
                .andExpect(jsonPath("$.phone").isEmpty());
    }

    @Test
    void adminCreatesSellerEvenWhenSignupIsClosed() throws Exception {
        Cookie admin = login("admin@test.local", "admin1234");
        mvc.perform(withJson(put("/api/admin/settings").cookie(admin), Map.of("signupOpen", false))).andExpect(status().isOk());

        String email = uniqueEmail();
        mvc.perform(withJson(post("/api/admin/sellers").cookie(admin), account(email, "010-5555-0001"))).andExpect(status().isOk());
        // 만든 계정으로 바로 로그인, 무료 체험으로 시작
        Cookie seller = login(email, "password123");
        mvc.perform(get("/api/me").cookie(seller))
                .andExpect(jsonPath("$.role").value("SELLER"))
                .andExpect(jsonPath("$.plan").value("TRIAL"))
                .andExpect(jsonPath("$.phone").value("010-5555-0001"));

        // 같은 이메일, 잘못된 번호, 판매자가 부르면 막힘
        mvc.perform(withJson(post("/api/admin/sellers").cookie(admin), account(email.toUpperCase(), "010-5555-0002")))
                .andExpect(status().isConflict());
        mvc.perform(withJson(post("/api/admin/sellers").cookie(admin), account(uniqueEmail(), "1234")))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("PHONE_INVALID"));
        mvc.perform(withJson(post("/api/admin/sellers").cookie(seller), account(uniqueEmail(), "010-5555-0003")))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminFixesSellerInfo() throws Exception {
        String email = uniqueEmail();
        Cookie seller = signup(email);
        Cookie admin = login("admin@test.local", "admin1234");
        long id = adminRow(admin, email).get("id").asLong();
        String path = "/api/admin/sellers/" + id + "/info";

        String newEmail = uniqueEmail();
        Map<String, Object> info = new HashMap<>();
        info.put("email", newEmail);
        info.put("displayName", "바뀐 이름");
        info.put("phone", "010-7777-8888");
        info.put("newPassword", "newpass1234");
        mvc.perform(withJson(put(path).cookie(admin), info)).andExpect(status().isOk());

        // 전에 로그인해 둔 쿠키는 그대로 쓰이고(토큰에 이메일이 안 들어감) 바뀐 정보가 보인다
        mvc.perform(get("/api/me").cookie(seller))
                .andExpect(jsonPath("$.email").value(newEmail))
                .andExpect(jsonPath("$.displayName").value("바뀐 이름"))
                .andExpect(jsonPath("$.phone").value("010-7777-8888"));
        // 새 이메일·새 비밀번호로 로그인, 옛 이메일·옛 비밀번호는 안 됨
        login(newEmail, "newpass1234");
        mvc.perform(withJson(post("/api/auth/login"), Map.of("email", email, "password", "password123")))
                .andExpect(status().isUnauthorized());
        mvc.perform(withJson(post("/api/auth/login"), Map.of("email", newEmail, "password", "password123")))
                .andExpect(status().isUnauthorized());

        // 새 비밀번호를 비우면 비밀번호는 그대로
        info.put("newPassword", null);
        info.put("displayName", "다시 바꿈");
        mvc.perform(withJson(put(path).cookie(admin), info)).andExpect(status().isOk());
        login(newEmail, "newpass1234");

        // 다른 판매자가 쓰는 이메일로는 못 바꿈, 짧은 새 비밀번호는 막힘
        String other = uniqueEmail();
        signup(other);
        info.put("email", other);
        mvc.perform(withJson(put(path).cookie(admin), info)).andExpect(status().isConflict());
        info.put("email", newEmail);
        info.put("newPassword", "short");
        mvc.perform(withJson(put(path).cookie(admin), info))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("PASSWORD_LENGTH"));

        // 판매자는 못 부름
        info.put("newPassword", null);
        mvc.perform(withJson(put(path).cookie(seller), info)).andExpect(status().isForbidden());
    }

    @Test
    void adminAccountEmailCannotChange() throws Exception {
        Cookie admin = login("admin@test.local", "admin1234");
        JsonNode me = read(mvc.perform(get("/api/me").cookie(admin)).andReturn());
        Map<String, Object> info = new HashMap<>();
        info.put("email", uniqueEmail());
        info.put("displayName", "관리자");
        mvc.perform(withJson(put("/api/admin/sellers/" + me.get("id").asLong() + "/info").cookie(admin), info))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("ADMIN_EMAIL_FIXED"));
        // 이메일을 그대로 두면 이름은 바꿀 수 있다
        info.put("email", "admin@test.local");
        mvc.perform(withJson(put("/api/admin/sellers/" + me.get("id").asLong() + "/info").cookie(admin), info))
                .andExpect(status().isOk());
    }
}
