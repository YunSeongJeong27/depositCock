package com.depositcock.notification;

import com.depositcock.common.ApiException;
import com.depositcock.common.ErrorCode;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

/**
 * 판매자에게 메일 보내기 공통. SMTP 는 환경 변수로 설정한다 (SPRING_MAIL_HOST 등, README). 설정이 없으면 보내지 않고 기록만 남긴다.
 * 처음에는 Gmail 계정 + 앱 비밀번호로 보내고, 판매자가 늘면 Amazon SES 등으로 설정만 바꾼다 [2026-10-16 사용자 결정].
 */
@Component
@Slf4j
public class SellerMail {

    private final ObjectProvider<JavaMailSender> mailSender;
    private final String from;
    private final String baseUrl;
    /** 내 맥북 로컬 실행용 (application-local.yml): 메일 설정이 없을 때 꼭 보내야 하는 메일을 보내지 않고 내용을 로그로 남긴다 */
    private final boolean devLog;

    public SellerMail(ObjectProvider<JavaMailSender> mailSender,
                      @Value("${app.mail-from:}") String from,
                      @Value("${spring.mail.username:}") String username,
                      @Value("${app.public-base-url:}") String baseUrl,
                      @Value("${app.mail-dev-log:false}") boolean devLog) {
        this.mailSender = mailSender;
        this.from = from == null || from.isBlank() ? username : from;
        this.baseUrl = baseUrl;
        this.devLog = devLog;
    }

    /** 메일 본문 글자 이스케이프: <>&"' 만 바꾸고 한글·가운뎃점(·)은 그대로 둔다 (두 알림 메일이 같은 방식으로) */
    static String escape(String text) {
        return HtmlUtils.htmlEscape(text, "UTF-8");
    }

    /** 공구 화면 링크 (주소 설정이 없으면 빈 문자열) */
    String groupBuyLink(Long groupBuyId) {
        return baseUrl == null || baseUrl.isBlank() ? ""
                : "<p><a href=\"" + escape(baseUrl.replaceAll("/+$", "") + "/seller/group-buys/" + groupBuyId)
                + "\">" + MailText.OPEN_GROUP_BUY.text() + "</a></p>";
    }

    /** 실패해도 예외를 던지지 않는다 (주문은 이미 저장됨, 화면 알림은 그대로 보인다) */
    void send(String to, String subject, String html) {
        JavaMailSender sender = mailSender.getIfAvailable();
        if (sender == null || from == null || from.isBlank()) {
            log.info("메일 미설정 → 보내지 않음: {}", subject);
            return;
        }
        try {
            deliver(sender, to, subject, html);
        } catch (Exception ex) {
            log.warn("메일 실패: {} / {} / {}", to, subject, ex.getMessage());
        }
    }

    /**
     * 꼭 보내야 하는 메일 (인증번호·아이디·임시 비밀번호, 2026-11-20). 못 보내면 ApiException 으로 요청을 실패시킨다
     * (부르는 쪽 트랜잭션도 되돌아가서 임시 비밀번호가 바뀌지 않는다). 메일 설정이 없으면 MAIL_NOT_CONFIGURED,
     * 단 로컬 실행(app.mail-dev-log)에서는 내용을 로그로 남기고 성공으로 친다.
     */
    void sendRequired(String to, String subject, String html) {
        JavaMailSender sender = mailSender.getIfAvailable();
        if (sender == null || from == null || from.isBlank()) {
            if (devLog) {
                log.info("[로컬] 메일 미설정 → 보내지 않고 내용만 기록: {} / {} / {}", to, subject, html);
                return;
            }
            log.warn("메일 미설정 → 꼭 보내야 하는 메일을 못 보냄: {}", subject);
            throw new ApiException(ErrorCode.MAIL_NOT_CONFIGURED);
        }
        try {
            deliver(sender, to, subject, html);
        } catch (Exception ex) {
            log.warn("메일 실패: {} / {} / {}", to, subject, ex.getMessage());
            throw new ApiException(ErrorCode.MAIL_SEND_FAILED);
        }
    }

    private void deliver(JavaMailSender sender, String to, String subject, String html) throws Exception {
        MimeMessage message = sender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
        helper.setFrom(from);
        helper.setTo(to);
        helper.setSubject(MailText.SUBJECT_PREFIX.text() + subject);
        helper.setText(html, true);
        sender.send(message);
    }
}
