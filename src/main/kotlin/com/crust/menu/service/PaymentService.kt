package com.crust.menu.service

import com.crust.menu.domain.Payment
import com.crust.menu.repository.OrderRepository
import com.crust.menu.repository.PaymentRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

@Service
class PaymentService(
    private val paymentRepository: PaymentRepository,
    private val orderRepository: OrderRepository
) {
    private val log = LoggerFactory.getLogger(PaymentService::class.java)

    // Simulated failure rate (10% of authorizations fail)
    private val SIMULATED_FAILURE_RATE = 0.10

    @Transactional
    fun initiatePayment(orderId: UUID, amount: BigDecimal, tip: BigDecimal, method: String): Payment {
        val order = orderRepository.findById(orderId).orElseThrow {
            IllegalArgumentException("Order $orderId not found")
        }

        val payment = Payment(
            orderId = orderId,
            amount = amount,
            tipAmount = tip,
            totalCharged = amount.add(tip),
            paymentMethod = method
        )
        val saved = paymentRepository.save(payment)
        log.info("Payment ${saved.id} initiated for order $orderId — $${saved.totalCharged} via $method")
        return saved
    }

    @Transactional
    fun authorizePayment(paymentId: UUID): Payment {
        val payment = paymentRepository.findById(paymentId).orElseThrow {
            IllegalArgumentException("Payment $paymentId not found")
        }

        // Simulated gateway authorization
        val gatewaySuccess = Math.random() > SIMULATED_FAILURE_RATE

        if (gatewaySuccess) {
            payment.status = "AUTHORIZED"
            payment.transactionRef = "TXN-${UUID.randomUUID().toString().take(8).uppercase()}"
            log.info("Payment ${payment.id} authorized — ref: ${payment.transactionRef}")
        } else {
            payment.status = "FAILED"
            payment.failureReason = "Gateway declined: insufficient funds (simulated)"
            log.warn("Payment ${payment.id} failed — simulated decline")
        }
        payment.updatedAt = LocalDateTime.now()
        return paymentRepository.save(payment)
    }

    @Transactional
    fun capturePayment(paymentId: UUID): Payment {
        val payment = paymentRepository.findById(paymentId).orElseThrow {
            IllegalArgumentException("Payment $paymentId not found")
        }
        if (payment.status != "AUTHORIZED") {
            throw IllegalStateException("Cannot capture payment in ${payment.status} state — must be AUTHORIZED")
        }
        payment.status = "CAPTURED"
        payment.updatedAt = LocalDateTime.now()
        log.info("Payment ${payment.id} captured — $${payment.totalCharged}")

        // Mark the order as completed
        val order = orderRepository.findById(payment.orderId).orElse(null)
        if (order != null && order.status == "READY") {
            order.status = "COMPLETED"
            order.tip = payment.tipAmount
            order.updatedAt = LocalDateTime.now()
            orderRepository.save(order)
            log.info("Order ${order.id} completed after payment capture")
        }

        return paymentRepository.save(payment)
    }

    @Transactional
    fun refundPayment(paymentId: UUID, reason: String?): Payment {
        val payment = paymentRepository.findById(paymentId).orElseThrow {
            IllegalArgumentException("Payment $paymentId not found")
        }
        if (payment.status != "CAPTURED") {
            throw IllegalStateException("Cannot refund payment in ${payment.status} state — must be CAPTURED")
        }
        payment.status = "REFUNDED"
        payment.failureReason = reason ?: "Customer-requested refund"
        payment.updatedAt = LocalDateTime.now()
        log.info("Payment ${payment.id} refunded — reason: ${payment.failureReason}")
        return paymentRepository.save(payment)
    }

    fun getPaymentsForOrder(orderId: UUID): List<Payment> =
        paymentRepository.findByOrderId(orderId)
}
