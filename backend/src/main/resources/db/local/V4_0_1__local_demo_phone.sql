-- 로컬(맥북) 전용. 데모 판매자에게 휴대폰 번호를 넣어 아이디·비밀번호 찾기를 시험할 수 있게 한다 (2026-11-20).
-- 데모 판매자 로그인: 아이디 demo (V4 가 이메일 @ 앞부분으로 채움) / 비밀번호 demo1234, 휴대폰 010-0000-0001
UPDATE seller SET phone = '01000000001' WHERE email = 'demo@depositcock.local' AND phone IS NULL;
