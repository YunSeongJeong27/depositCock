-- 배송 주문의 수령인 이름·전화번호를 받지 않는다 (2026-11-10 사용자 결정: 고객 손이 덜 가게).
-- 판매자는 택배를 입금자명·휴대폰 번호로 보낸다. 안 쓰는 개인정보 칸이라 지운다 (이미 받은 값도 같이 지워짐).
ALTER TABLE customer_order DROP COLUMN recipient_name;
ALTER TABLE customer_order DROP COLUMN recipient_phone;
