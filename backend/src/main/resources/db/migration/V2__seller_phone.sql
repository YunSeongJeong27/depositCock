-- 판매자 휴대폰 번호 (2026-11-09 사용자 요청: 관리자가 연락할 수단). 숫자만 저장 (PhoneNumbers.normalize).
-- 인증은 하지 않고 적은 그대로 받는다. 이 칸이 생기기 전에 가입한 판매자는 비어 있다.
ALTER TABLE seller ADD COLUMN phone VARCHAR(20);
