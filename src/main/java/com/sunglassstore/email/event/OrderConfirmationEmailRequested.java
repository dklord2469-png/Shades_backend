package com.sunglassstore.email.event;

import java.math.BigDecimal;
import java.util.List;

/**
 * Raised when an order's payment succeeds — the moment the shop is actually committed to
 * fulfilling it, which is why this fires on payment rather than on order creation: a PLACED but
 * unpaid order can still expire, and confirming one by email would promise something the expiry
 * job may take back.
 *
 * @param items one display line per order line ("2 × Wayfarer (Ocean Blue) — INR 950.00 each"),
 *              rendered by the publisher from the order's own snapshot columns so the email keeps
 *              reading correctly even after the catalogue changes.
 */
public record OrderConfirmationEmailRequested(String email, String customerName, Long orderId,
                                              List<String> items, BigDecimal totalAmount) {}
