package com.depositcock.notification;

import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 계정 메일 (2026-11-20): 가입 인증번호, 아이디 찾기, 비밀번호 찾기·변경 인증번호, 임시 비밀번호.
 * 알림 메일과 달리 요청 안에서 바로 보내고, 못 보내면 요청을 실패시킨다 (SellerMail.sendRequired).
 * 메일에는 아이디·인증번호·임시 비밀번호만 넣고 이름·휴대폰 번호는 넣지 않는다.
 */
@Component
@RequiredArgsConstructor
public class AccountMailer {

    private final SellerMail mail;

    public void signupCode(String email, String code, int validMinutes) {
        send(email, MailText.SIGNUP_CODE, List.of(MailText.CODE_LINE.text(code)),
                List.of(MailText.CODE_VALID_MINUTES.text(validMinutes)));
    }

    public void passwordCode(String email, String code, int validMinutes) {
        send(email, MailText.PASSWORD_CODE, List.of(MailText.CODE_LINE.text(code)),
                List.of(MailText.CODE_VALID_MINUTES.text(validMinutes)));
    }

    public void loginId(String email, String loginId) {
        send(email, MailText.LOGIN_ID_FOUND, List.of(MailText.LOGIN_ID_LINE.text(loginId)), List.of());
    }

    public void tempPassword(String email, String tempPassword) {
        send(email, MailText.TEMP_PASSWORD, List.of(MailText.TEMP_PASSWORD_LINE.text(tempPassword)),
                List.of(MailText.TEMP_PASSWORD_CHANGE.text()));
    }

    private void send(String email, MailText headline, List<String> boldLines, List<String> lines) {
        StringBuilder html = new StringBuilder("<p>").append(SellerMail.escape(headline.text())).append("</p>");
        boldLines.forEach(line -> html.append("<p><b>").append(SellerMail.escape(line)).append("</b></p>"));
        lines.forEach(line -> html.append("<p>").append(SellerMail.escape(line)).append("</p>"));
        html.append("<p style=\"color:#6b7280;font-size:12px\">").append(MailText.NOT_REQUESTED.text()).append("</p>");
        mail.sendRequired(email, headline.text(), html.toString());
    }
}
