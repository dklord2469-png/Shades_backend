package com.sunglassstore.service;

import com.sunglassstore.dto.request.PaymentRequest;
import com.sunglassstore.dto.response.PaymentResult;
import com.sunglassstore.entity.Order;
import com.sunglassstore.entity.Payment;
import com.sunglassstore.entity.User;
import com.sunglassstore.entity.enums.OrderStatus;
import com.sunglassstore.entity.enums.PaymentStatus;
import com.sunglassstore.exception.BadRequestException;
import com.sunglassstore.exception.ResourceNotFoundException;
import com.sunglassstore.repository.OrderRepository;
import com.sunglassstore.repository.PaymentRepository;
import com.sunglassstore.service.impl.PaymentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PaymentServiceImplTest {
    private PaymentRepository payments;
    private OrderRepository orders;
    private PaymentProcessor processor;
    private OrderService orderService;
    private PaymentServiceImpl service;
    private ApplicationEventPublisher events;

    @BeforeEach
    void setUp() {
        payments = mock(PaymentRepository.class); orders = mock(OrderRepository.class);
        processor = mock(PaymentProcessor.class); orderService = mock(OrderService.class);
        events = mock(ApplicationEventPublisher.class);
        service = new PaymentServiceImpl(payments, orders, processor, orderService, events);
    }

    @Test
    void successfulMockPaymentLocksOrderNormalizesMethodAndRecordsPaidTime() {
        Order order = order();
        when(orders.findByOrderIdAndUserUserIdForUpdate(4L, 7L)).thenReturn(Optional.of(order));
        when(payments.findFirstByOrderOrderIdAndPaymentStatus(4L, PaymentStatus.PAID)).thenReturn(Optional.empty());
        when(processor.process("MOCK", new BigDecimal("283.20")))
                .thenReturn(new PaymentResult(true, "MOCK-1", "ok"));
        when(payments.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Payment payment = service.processPayment(7L, 4L, request(" mock "));

        assertEquals(PaymentStatus.PAID, payment.getPaymentStatus());
        assertEquals("MOCK", payment.getPaymentMethod());
        assertNotNull(payment.getPaidAt());
        verify(orders).findByOrderIdAndUserUserIdForUpdate(4L, 7L);
        verify(orderService).updateOrderStatus(4L, OrderStatus.CONFIRMED, "Payment received");
        verify(events).publishEvent(any(com.sunglassstore.notification.event.OrderPaymentConfirmed.class));
    }

    @Test
    void successfulPaymentRequestsExactlyOneConfirmationEmailWithTheOrdersSnapshotLines() {
        Order order = order();
        com.sunglassstore.entity.OrderItem line = new com.sunglassstore.entity.OrderItem();
        line.setProductName("Wayfarer"); line.setVariantLabel("Ocean Blue");
        line.setQuantity(2); line.setUnitPrice(new BigDecimal("141.60"));
        order.getItems().add(line);
        when(orders.findByOrderIdAndUserUserIdForUpdate(4L, 7L)).thenReturn(Optional.of(order));
        when(payments.findFirstByOrderOrderIdAndPaymentStatus(4L, PaymentStatus.PAID)).thenReturn(Optional.empty());
        when(processor.process("MOCK", new BigDecimal("283.20")))
                .thenReturn(new PaymentResult(true, "MOCK-1", "ok"));
        when(payments.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.processPayment(7L, 4L, request("MOCK"));

        // Two events publish on success — the in-app notification and this email — so capture
        // everything and pick the email out by type, asserting it appears exactly once.
        var captor = org.mockito.ArgumentCaptor.forClass(Object.class);
        verify(events, times(2)).publishEvent(captor.capture());
        var emails = captor.getAllValues().stream()
                .filter(com.sunglassstore.email.event.OrderConfirmationEmailRequested.class::isInstance)
                .map(com.sunglassstore.email.event.OrderConfirmationEmailRequested.class::cast)
                .toList();
        assertEquals(1, emails.size());
        var email = emails.get(0);
        assertEquals("buyer@example.test", email.email());
        assertEquals(4L, email.orderId());
        assertEquals(new BigDecimal("283.20"), email.totalAmount());
        // Rendered from the order line's own snapshot columns, colourway included.
        assertEquals(java.util.List.of("2 x Wayfarer (Ocean Blue) - INR 141.60 each"), email.items());
    }

    @Test
    void failedPaymentRequestsNoConfirmationEmail() {
        Order order = order();
        when(orders.findByOrderIdAndUserUserIdForUpdate(4L, 7L)).thenReturn(Optional.of(order));
        when(payments.findFirstByOrderOrderIdAndPaymentStatus(4L, PaymentStatus.PAID)).thenReturn(Optional.empty());
        when(processor.process("MOCK", new BigDecimal("283.20")))
                .thenReturn(new PaymentResult(false, "MOCK-1", "declined"));
        when(payments.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        assertEquals(PaymentStatus.FAILED, service.processPayment(7L, 4L, request("MOCK")).getPaymentStatus());
        verify(events, never()).publishEvent(any(com.sunglassstore.email.event.OrderConfirmationEmailRequested.class));
    }

    @Test
    void duplicateSuccessfulPaymentRetryReturnsExistingPaymentWithoutCallingProcessor() {
        Order order = order(); Payment existing = new Payment();
        when(orders.findByOrderIdAndUserUserIdForUpdate(4L, 7L)).thenReturn(Optional.of(order));
        when(payments.findFirstByOrderOrderIdAndPaymentStatus(4L, PaymentStatus.PAID))
                .thenReturn(Optional.of(existing));

        assertSame(existing, service.processPayment(7L, 4L, request("MOCK")));
        verifyNoInteractions(processor);
        verify(payments, never()).save(any());
        // A replayed callback must not re-send the confirmation email either.
        verify(events, never()).publishEvent(any());
    }

    @Test
    void customerCannotReadAnotherCustomersPaymentHistory() {
        when(orders.findByOrderIdAndUserUserId(4L, 7L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.getPayments(7L, 4L, PageRequest.of(0, 10)));

        verify(payments, never()).findByOrderOrderId(any(), any());
    }

    private Order order() {
        User user = new User(); user.setUserId(7L); user.setName("Asha"); user.setEmail("buyer@example.test");
        Order order = new Order(); order.setOrderId(4L); order.setUser(user);
        order.setOrderStatus(OrderStatus.PLACED); order.setTotalAmount(new BigDecimal("283.20")); return order;
    }

    private PaymentRequest request(String method) {
        PaymentRequest request = new PaymentRequest(); request.setPaymentMethod(method); return request;
    }
}
