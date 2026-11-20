package com.depositcock.admin;

import java.time.Clock;

import com.depositcock.groupbuy.GroupBuyRepository;
import com.depositcock.seller.Role;
import com.depositcock.seller.Seller;
import com.depositcock.seller.SellerRepository;
import com.depositcock.vision.CaptureUsageService;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 시작할 때 관리자 계정 만들기 규칙 (2026-11-20). 통합 테스트 DB 에는 관리자가 늘 있어서 "관리자가 없을 때"는 여기서 본다.
 */
class AdminBootstrapRuleTest {

    private final SellerRepository sellers = mock(SellerRepository.class);
    private final AdminService service = new AdminService(sellers, mock(GroupBuyRepository.class),
            mock(PasswordEncoder.class), mock(CaptureUsageService.class), Clock.systemDefaultZone());

    @Test
    void doesNothingWhenAnAdminAlreadyExists() {
        when(sellers.existsByRole(Role.ADMIN)).thenReturn(true);
        assertThat(service.ensureAdmin("admin", "admin@test.local", "admin1234")).isTrue();
        verify(sellers, never()).save(any(Seller.class));
    }

    @Test
    void doesNotPromoteSellerUsingTheEmailOrLoginId() {
        when(sellers.existsByRole(Role.ADMIN)).thenReturn(false);
        when(sellers.existsByEmail("taken@test.local")).thenReturn(true);
        assertThat(service.ensureAdmin("admin", "taken@test.local", "admin1234")).isFalse();
        when(sellers.existsByLoginId("admin")).thenReturn(true);
        assertThat(service.ensureAdmin("admin", "free@test.local", "admin1234")).isFalse();
        assertThat(service.ensureAdmin("1bad", "free@test.local", "admin1234")).isFalse();
        verify(sellers, never()).save(any(Seller.class));
    }

    @Test
    void createsAdminWhenNoneExists() {
        when(sellers.existsByRole(Role.ADMIN)).thenReturn(false);
        assertThat(service.ensureAdmin("Admin", " Admin@Test.local ", "admin1234")).isTrue();
        verify(sellers).save(any(Seller.class));
    }
}
