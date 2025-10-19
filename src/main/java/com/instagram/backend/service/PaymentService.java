package com.instagram.backend.service;

import com.instagram.backend.model.entity.BankAccount;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Refund;
import com.stripe.model.Transfer;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.RefundCreateParams;
import com.stripe.param.TransferCreateParams;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

@Service
@Slf4j
public class PaymentService {

    @Value("${stripe.api.key:}")
    private String stripeApiKey;

    @Value("${payment.platform.fee.percentage:5.0}")
    private BigDecimal platformFeePercentage;

    @PostConstruct
    public void init() {
        if (stripeApiKey != null && !stripeApiKey.isEmpty()) {
            Stripe.apiKey = stripeApiKey;
            log.info("Stripe API key configured successfully");
        } else {
            log.warn("Stripe API key not configured. Payment processing will fail.");
        }
    }

    /**
     * Process a donation payment
     */
    public String processPayment(
            BigDecimal amount,
            String currency,
            String paymentMethod,
            Map<String, String> paymentDetails
    ) throws Exception {
        try {
            long amountInCents = amount.multiply(new BigDecimal("100")).longValue();

            PaymentIntentCreateParams.Builder paramsBuilder = PaymentIntentCreateParams.builder()
                    .setAmount(amountInCents)
                    .setCurrency(currency.toLowerCase())
                    .setDescription("Campaign Donation");

            // Check if payment method is provided
            if (paymentDetails != null && paymentDetails.containsKey("paymentMethodId")) {
                String paymentMethodId = paymentDetails.get("paymentMethodId");

                log.info("Processing payment with payment method: {}", paymentMethodId);

                // Only set confirm=true when we have a payment method
                paramsBuilder.setPaymentMethod(paymentMethodId)
                        .setConfirm(true)
                        .setAutomaticPaymentMethods(
                                PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                        .setEnabled(true)
                                        .setAllowRedirects(PaymentIntentCreateParams.AutomaticPaymentMethods.AllowRedirects.NEVER)
                                        .build()
                        );

                // Only add return_url if we're confirming
                String returnUrl = System.getenv("PAYMENT_RETURN_URL");
                if (returnUrl == null) {
                    returnUrl = "http://localhost:8080";
                }
                paramsBuilder.setReturnUrl(returnUrl);

            } else {
                // No payment method provided, don't confirm yet
                log.info("Creating payment intent without confirmation");
                paramsBuilder.setAutomaticPaymentMethods(
                        PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                .setEnabled(true)
                                .build()
                );
            }

            PaymentIntentCreateParams params = paramsBuilder.build();
            PaymentIntent intent = PaymentIntent.create(params);

            log.info("Payment intent created: id={}, status={}", intent.getId(), intent.getStatus());

            if ("succeeded".equals(intent.getStatus())) {
                log.info("Payment successful: transactionId={}", intent.getId());
                return intent.getId();
            } else if ("requires_action".equals(intent.getStatus()) ||
                    "requires_payment_method".equals(intent.getStatus())) {
                // Payment requires additional action (3D Secure, UPI, etc.)
                // Return client secret for frontend to handle
                log.info("Payment requires action, returning client secret");
                return intent.getClientSecret();
            } else {
                throw new Exception("Payment failed with status: " + intent.getStatus());
            }

        } catch (StripeException e) {
            log.error("Stripe payment error: {}", e.getMessage(), e);
            throw new Exception("Payment processing failed: " + e.getMessage());
        }
    }

    /**
     * Process a refund for a donation
     */
    public String processRefund(String transactionId, BigDecimal amount) throws Exception {
        log.info("Processing refund: transactionId={}, amount={}", transactionId, amount);

        try {
            long amountInCents = amount.multiply(new BigDecimal("100")).longValue();

            RefundCreateParams params = RefundCreateParams.builder()
                    .setPaymentIntent(transactionId)
                    .setAmount(amountInCents)
                    .build();

            Refund refund = Refund.create(params);

            if ("succeeded".equals(refund.getStatus())) {
                log.info("Refund successful: refundId={}", refund.getId());
                return refund.getId();
            } else {
                throw new Exception("Refund failed with status: " + refund.getStatus());
            }

        } catch (StripeException e) {
            log.error("Stripe refund error: {}", e.getMessage(), e);
            throw new Exception("Refund processing failed: " + e.getMessage());
        }
    }

    /**
     * Process bank transfer for withdrawal
     */
    public String processBankTransfer(
            BigDecimal amount,
            String currency,
            BankAccount bankAccount
    ) throws Exception {
        log.info("Processing bank transfer: amount={}, currency={}, account={}",
                amount, currency, bankAccount.getMaskedAccountNumber());

        try {
            long amountInCents = amount.multiply(new BigDecimal("100")).longValue();

            TransferCreateParams params = TransferCreateParams.builder()
                    .setAmount(amountInCents)
                    .setCurrency(currency.toLowerCase())
                    .setDestination("acct_" + bankAccount.getId())
                    .setDescription("Campaign Withdrawal")
                    .build();

            Transfer transfer = Transfer.create(params);

            log.info("Bank transfer successful: transferId={}", transfer.getId());
            return transfer.getId();

        } catch (StripeException e) {
            log.error("Stripe transfer error: {}", e.getMessage(), e);
            throw new Exception("Bank transfer failed: " + e.getMessage());
        }
    }

    /**
     * Calculate platform fee
     */
    public BigDecimal calculatePlatformFee(BigDecimal amount) {
        return amount.multiply(platformFeePercentage)
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
    }

    /**
     * Calculate payment gateway fee (example: 2.9% + $0.30 for Stripe)
     */
    public BigDecimal calculateGatewayFee(BigDecimal amount) {
        BigDecimal percentageFee = amount.multiply(new BigDecimal("0.029"));
        BigDecimal fixedFee = new BigDecimal("0.30");
        return percentageFee.add(fixedFee);
    }

    /**
     * Create a payment intent (for frontend to complete)
     */
    public String createPaymentIntent(BigDecimal amount, String currency) throws Exception {
        try {
            long amountInCents = amount.multiply(new BigDecimal("100")).longValue();

            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount(amountInCents)
                    .setCurrency(currency.toLowerCase())
                    .setAutomaticPaymentMethods(
                            PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                    .setEnabled(true)
                                    .build()
                    )
                    .build();

            PaymentIntent intent = PaymentIntent.create(params);
            return intent.getClientSecret();

        } catch (StripeException e) {
            log.error("Error creating payment intent: {}", e.getMessage(), e);
            throw new Exception("Failed to create payment intent: " + e.getMessage());
        }
    }

    /**
     * Verify payment status
     */
    public boolean verifyPayment(String transactionId) {
        try {
            PaymentIntent intent = PaymentIntent.retrieve(transactionId);
            return "succeeded".equals(intent.getStatus());
        } catch (StripeException e) {
            log.error("Error verifying payment: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Mock payment processing for development/testing
     */

    /**
     * Mock refund for development/testing
     */
    public String processMockRefund(String transactionId, BigDecimal amount) {
        log.info("Processing mock refund: txn={}, amount={}", transactionId, amount);
        return "mock_refund_" + System.currentTimeMillis();
    }
}