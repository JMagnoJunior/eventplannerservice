package com.mjrcompany.eventplannerservice

import arrow.core.firstOrNone
import arrow.core.getOrElse
import com.mjrcompany.eventplannerservice.database.*
import com.mjrcompany.eventplannerservice.domain.*
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDateTime
import java.util.*


object TestDatabaseHelper {

    fun addEvent(): UUID {
        val hostId =
            generateUser(
                UUID.randomUUID()
            )
        val subjectId =
            generateSubject(
                UUID.randomUUID()
            )

        val event = EventWritable(
            title = "test",
            host = hostId,
            address = "somwhere",
            subject = subjectId,
            maxNumberGuest = 10,
            date = LocalDateTime.now(),
            totalCost = BigDecimal.TEN,
            additionalInfo = ""
        )
        return addEvent(UUID.randomUUID(), event)
    }

    fun addEvent(uuid: UUID, event: EventWritable): UUID {

        return transaction {
            Events.insert {
                it[id] = uuid
                it[title] = event.title
                it[host] = event.host
                it[subject] = event.subject
                it[date] = event.date
                it[address] = event.address
                it[maxNumberGuests] = event.maxNumberGuest
                it[createDate] = Instant.now()
                it[totalCost] = event.totalCost
                it[status] = EventStatus.Open
            } get Events.id
        }
    }

    fun addGuestInEvent(friendId: UUID, meetingId: UUID) {
        transaction {
            UsersInEvents.insert {
                it[event] = meetingId
                it[user] = friendId
                it[status] = UserInEventStatus.Pending
            }
        }
    }

    fun queryEventWithoutTasks(id: UUID): Event {
        return transaction {
            Events
                .join(Users, JoinType.INNER, additionalConstraint = { Events.host eq Users.id })
                .join(Subjects, JoinType.INNER, additionalConstraint = { Events.subject eq Subjects.id })
                .select { Events.id eq id }
                .map { DataMapper.mapToEvent(it, emptyList(), emptyList()) }
                .first()
        }
    }

    fun queryTaskById(id: Int): Task {
        return transaction {
            Tasks.select { Tasks.id eq id }
                .map { DataMapper.mapToTask(it) }
                .first()
        }
    }


    fun addTask(meetingId: UUID): Int {
        val taskId = transaction {
            Tasks.insert {
                it[details] = "test task"
                it[event] = meetingId
            } get Tasks.id
        }
        return taskId.value
    }

    fun generateUser(uuid: UUID): UUID {
        transaction {
            Users.insert {
                it[id] = uuid
                it[name] = "test"
                it[email] = "test@email.com"
            }
        }
        return uuid
    }

    fun generateUser(mail: String): UUID {
        return transaction {
            Users.insert {
                it[id] = UUID.randomUUID()
                it[name] = "test"
                it[email] = mail
            } get Users.id
        }
    }

    fun generateUser(uuid: UUID, hostName: String, hostEmail: String): UUID {
        return transaction {
            Users.insert {
                it[id] = uuid
                it[name] = hostName
                it[email] = hostEmail
            } get Users.id
        }
    }

    fun queryUserById(id: UUID): User {
        return transaction {
            Users.select { Users.id eq id }.map { DataMapper.mapToUser(it) }.first()
        }
    }

    fun generateSubject(uuid: UUID): UUID {
        transaction {
            Subjects.insert {
                it[id] = uuid
                it[name] = "test"
            }
        }
        return uuid
    }


    fun generateSubject(uuid: UUID, dishName: String): UUID {
        transaction {
            Subjects.insert {
                it[id] = uuid
                it[name] = dishName
            }
        }
        return uuid
    }

    fun queryDishById(id: UUID): Subject {
        lateinit var subject: Subject
        transaction {
            subject = Subjects
                .select { Subjects.id eq id }
                .map {
                    DataMapper.mapToDish(it)
                }
                .firstOrNone()
                .getOrElse { throw RuntimeException("Error querying com.mjrcompany.eventplannerservice.subjects") }
        }
        return subject
    }

}
