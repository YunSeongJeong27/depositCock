package com.depositcock.order;

/**
 * 주문의 배송 정보(주소). 배송이 아닌 주문은 NONE (모든 칸 null).
 * 고객 주문서와 판매자 주문 변경이 같은 규칙(OrderRules.delivery)으로 만든다.
 * 수령인 이름·전화번호는 받지 않는다: 판매자가 입금자명·휴대폰 번호로 보낸다 (2026-11-10 사용자 결정)
 */
record Delivery(String zipcode, String address, String addressDetail) {

    static final Delivery NONE = new Delivery(null, null, null);

    static Delivery of(CustomerOrder order) {
        return new Delivery(order.getZipcode(), order.getAddress(), order.getAddressDetail());
    }
}
