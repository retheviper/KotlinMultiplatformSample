package com.retheviper.chat.messaging.application

import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class NotificationEventBus {
    private val streams = ConcurrentHashMap<Uuid, MutableSharedFlow<Unit>>()

    fun stream(memberId: Uuid): SharedFlow<Unit> =
        streams.computeIfAbsent(memberId) {
            MutableSharedFlow(
                replay = 0,
                extraBufferCapacity = 1,
                onBufferOverflow = BufferOverflow.DROP_OLDEST
            )
        }

    fun publish(memberId: Uuid) {
        streams[memberId]?.tryEmit(Unit)
    }

    fun publish(memberIds: Iterable<Uuid>) {
        memberIds.toSet().forEach(::publish)
    }
}
