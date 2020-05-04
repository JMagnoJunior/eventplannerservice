package com.mjrcompany.eventplannerservice.event

import arrow.core.getOrElse
import com.mjrcompany.eventplannerservice.RootTestDefinition
import com.mjrcompany.eventplannerservice.TestDatabaseHelper
import com.mjrcompany.eventplannerservice.com.mjrcompany.eventplannerservice.domain.EventDTO
import com.mjrcompany.eventplannerservice.gson
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.fail


class EventServiceTest : RootTestDefinition() {

    @Test
    fun `it should should add an event when creating event with valid user`() {

        val hostEmail = TestDatabaseHelper.generateUser("test@mail.com")
        val subjectId = TestDatabaseHelper.generateSubject(UUID.randomUUID())
        val createEvent = buildCreateEventDTO(subjectId)

        val id = EventService.createEvent(hostEmail, createEvent)
            .toOption()
            .getOrElse { throw RuntimeException("Error creating event") }

        val event = TestDatabaseHelper.queryMiniEvent(id)

        assertEquals(subjectId, event.subject.id)
        assertEquals(subjectId, event.subject.id)
        assertEquals(gson.toJson(createEvent.date), gson.toJson(event.date))
        assertEquals(createEvent.title, event.title)
        assertEquals(createEvent.maxNumberGuest, event.maxNumberGuest)
    }

    @Test
    fun `it should fail to add an event when creating event when user does not exist`() {


        val invalidHostEmail = "invalid@mail.com"
        val subjectId = TestDatabaseHelper.generateSubject(UUID.randomUUID())
        val createEvent = buildCreateEventDTO(subjectId)


        EventService.createEvent(invalidHostEmail, createEvent).fold(
            {
                assertNotNull(it.errorResponse)
            },
            { fail("Error! Event should not bre created for an invalid user") }
        )

    }

    private fun buildCreateEventDTO(dishId: UUID): EventDTO {

        return EventDTO(
            "test",
            dishId,
            LocalDateTime.now(),
            "here",
            10,
            BigDecimal(10),
            "something"
        )
    }

}

