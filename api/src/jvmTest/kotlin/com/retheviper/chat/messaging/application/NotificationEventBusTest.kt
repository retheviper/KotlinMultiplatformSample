package com.retheviper.chat.messaging.application

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.yield
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class NotificationEventBusTest : FunSpec({
    test("publish delivers an event to the member stream") {
        val bus = NotificationEventBus()
        val memberId = Uuid.generateV7()

        runBlocking {
            val awaitingSignal = async {
                withTimeout(1_000) {
                    bus.stream(memberId).first()
                }
            }

            yield()
            bus.publish(memberId)

            awaitingSignal.await() shouldBe Unit
        }
    }

    test("publish iterable delivers events to each existing member stream") {
        val bus = NotificationEventBus()
        val firstMember = Uuid.generateV7()
        val secondMember = Uuid.generateV7()

        runBlocking {
            val firstSignal = async {
                withTimeout(1_000) {
                    bus.stream(firstMember).first()
                }
            }
            val secondSignal = async {
                withTimeout(1_000) {
                    bus.stream(secondMember).first()
                }
            }

            yield()
            bus.publish(listOf(firstMember, secondMember))

            firstSignal.await() shouldBe Unit
            secondSignal.await() shouldBe Unit
        }
    }
})
