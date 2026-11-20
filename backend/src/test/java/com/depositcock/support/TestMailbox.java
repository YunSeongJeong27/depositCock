package com.depositcock.support;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.mail.Address;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSenderImpl;

/**
 * 통합 테스트용 메일 서버 대신: 보낸 메일을 실제로 보내지 않고 모아 둔다 (2026-11-20).
 * 가입 인증번호·아이디·임시 비밀번호 메일 내용을 테스트가 읽는다.
 */
public class TestMailbox extends JavaMailSenderImpl {

    private static final Pattern CODE = Pattern.compile("인증번호: (\\d{6})");

    private final List<MimeMessage> sent = new CopyOnWriteArrayList<>();
    private volatile String failTo;

    /** 이 주소로 가는 다음 메일 한 통을 실패시킨다 (메일 서버 오류 흉내) */
    public void failNextTo(String to) {
        this.failTo = to;
    }

    @Override
    public void send(MimeMessage message) {
        try {
            for (Address address : message.getAllRecipients()) {
                if (address.toString().equalsIgnoreCase(failTo)) {
                    failTo = null;
                    throw new MailSendException("테스트: 메일 서버 오류");
                }
            }
        } catch (jakarta.mail.MessagingException e) {
            throw new IllegalStateException(e);
        }
        sent.add(message);
    }

    @Override
    public void send(MimeMessage... messages) {
        sent.addAll(List.of(messages));
    }

    /** 이 주소로 마지막에 보낸 메일 본문 (없으면 null) */
    public String lastBodyTo(String to) {
        try {
            for (int i = sent.size() - 1; i >= 0; i--) {
                MimeMessage m = sent.get(i);
                for (Address address : m.getAllRecipients()) {
                    if (address.toString().equalsIgnoreCase(to)) {
                        return (String) m.getContent();
                    }
                }
            }
            return null;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /** 이 주소로 마지막에 보낸 인증번호 */
    public String lastCodeTo(String to) {
        String body = lastBodyTo(to);
        Matcher m = body == null ? null : CODE.matcher(body);
        if (m == null || !m.find()) {
            throw new IllegalStateException("인증번호 메일이 없습니다: " + to);
        }
        return m.group(1);
    }

    public int countTo(String to) {
        int count = 0;
        try {
            for (MimeMessage m : sent) {
                for (Address address : m.getAllRecipients()) {
                    if (address.toString().equalsIgnoreCase(to)) {
                        count++;
                    }
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        return count;
    }
}
