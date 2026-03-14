package com.retheviper.chat.messaging.application

import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class NotificationEventBus {
    private val streams = ConcurrentHashMap<Uuid, MutableSharedFlow<Unit>>()

    fun stream(memberId: Uuid): SharedFlow<Unit> =
        streams.computeIfAbsent(memberId) {
            MutableSharedFlow(extraBufferCapacity = 32)
        }

    fun publish(memberId: Uuid) {
        streams[memberId]?.tryEmit(Unit)
    }

    fun publish(memberIds: Iterable<Uuid>) {
        memberIds.forEach(::publish)
    }
}
