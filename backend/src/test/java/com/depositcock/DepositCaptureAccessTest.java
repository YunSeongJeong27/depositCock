package com.depositcock;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;

import com.depositcock.support.IntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.servlet.http.Cookie;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 입금 대조의 "은행앱 화면 캡처로 올리기"는 관리자가 켜고 끈다 (2026-11-09 사용자 요청). 처음에는 씀.
 * 끄면 캡처 읽기·저장 API 가 막히고, 글자 없는 PDF 안내도 캡처를 권하지 않는다. 거래내역 파일 올리기는 그대로.
 */
class DepositCaptureAccessTest extends IntegrationTest {

    private static byte[] blankPdf() throws Exception {
        try (PDDocument doc = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            doc.addPage(new PDPage());
            doc.save(out);
            return out.toByteArray();
        }
    }

    @Test
    void adminTurnsDepositCaptureOffAndOn() throws Exception {
        Cookie seller = signup(uniqueEmail());
        JsonNode created = read(mvc.perform(withJson(post("/api/seller/group-buys").cookie(seller),
                groupBuyBody("캡처", List.of(product("상품", 1000, null))))).andExpect(status().isOk()).andReturn());
        String base = "/api/seller/group-buys/" + created.get("id").asLong() + "/deposits";
        Map<String, Object> save = Map.of("rows", List.of(Map.of("name", "김입금", "amount", 1000)));
        MockMultipartFile pdf = new MockMultipartFile("file", "그림.pdf", "application/pdf", blankPdf());

        mvc.perform(get("/api/me").cookie(seller)).andExpect(jsonPath("$.depositCaptureEnabled").value(true));
        mvc.perform(multipart(base + "/preview").file(pdf).cookie(seller))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("STATEMENT_PDF_NO_TEXT"));

        Cookie admin = login("admin@test.local", "admin1234");
        mvc.perform(withJson(put("/api/admin/settings").cookie(admin), Map.of("depositCaptureOpen", false)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.depositCaptureOpen").value(false))
                // 보내지 않은 설정은 그대로
                .andExpect(jsonPath("$.signupOpen").value(true));

        mvc.perform(get("/api/me").cookie(seller)).andExpect(jsonPath("$.depositCaptureEnabled").value(false));
        mvc.perform(withJson(post(base + "/screenshots/save").cookie(seller), save))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("DEPOSIT_CAPTURE_CLOSED"));
        mvc.perform(multipart(base + "/screenshots/extract")
                        .file(new MockMultipartFile("files", "a.png", "image/png", new byte[]{1, 2, 3})).cookie(seller))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("DEPOSIT_CAPTURE_CLOSED"));
        // 글자 없는 PDF 안내는 캡처를 권하지 않는 문구로
        mvc.perform(multipart(base + "/preview").file(pdf).cookie(seller))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("STATEMENT_PDF_NO_TEXT_FILE_ONLY"));
        // 거래내역 파일 올리기는 그대로
        mvc.perform(multipart(base + "/preview").file(new MockMultipartFile("file", "내역.csv", "text/csv",
                        "적요,거래 금액,거래 일시\n김입금,1000,2026-11-09 10:00:00\n".getBytes(java.nio.charset.StandardCharsets.UTF_8)))
                        .cookie(seller))
                .andExpect(status().isOk());

        mvc.perform(withJson(put("/api/admin/settings").cookie(admin), Map.of("depositCaptureOpen", true)))
                .andExpect(jsonPath("$.depositCaptureOpen").value(true));
        mvc.perform(withJson(post(base + "/screenshots/save").cookie(seller), save)).andExpect(status().isOk());
    }

    @Test
    void onlyAdminChangesDepositCaptureSetting() throws Exception {
        Cookie seller = signup(uniqueEmail());
        mvc.perform(withJson(put("/api/admin/settings").cookie(seller), Map.of("depositCaptureOpen", false)))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/me").cookie(seller)).andExpect(jsonPath("$.depositCaptureEnabled").value(true));
    }
}
