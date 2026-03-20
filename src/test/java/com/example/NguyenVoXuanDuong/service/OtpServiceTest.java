package com.example.NguyenVoXuanDuong.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OtpServiceTest {

    private final OtpService otpService = new OtpService();

    @Test
    void shouldVerifySuccessfully_whenOtpIsCorrect() {
        OtpService.OtpRequestResult request = otpService.requestRedeemOtp("0901234567");

        assertTrue(request.success());
        assertTrue(request.demoOtp() != null && request.demoOtp().length() == 6);

        OtpService.OtpVerifyResult verify = otpService.verifyRedeemOtp("0901234567", request.demoOtp());
        assertTrue(verify.success());
    }

    @Test
    void shouldReject_whenOtpIsIncorrect() {
        OtpService.OtpRequestResult request = otpService.requestRedeemOtp("0909999999");
        assertTrue(request.success());

        OtpService.OtpVerifyResult verify = otpService.verifyRedeemOtp("0909999999", "000000");
        assertFalse(verify.success());
    }

    @Test
    void shouldSupportEmailChannel_whenRequestingOtp() {
        OtpService.OtpRequestResult request = otpService.requestRedeemOtp("0901234567", "email", "user_demo@local.test");
        assertTrue(request.success());

        OtpService.OtpVerifyResult verify = otpService.verifyRedeemOtp("0901234567", request.demoOtp());
        assertTrue(verify.success());
    }
}
