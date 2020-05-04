package com.mjrcompany.eventplannerservice

import arrow.core.Option
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

    fun generateEvent(): UUID {
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
        return generateEvent(UUID.randomUUID(), event)
    }

    fun generateEvent(uuid: UUID, event: EventWritable): UUID {

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
                it[additionalInfo] = event.additionalInfo
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

    fun queryMiniEvent(id: UUID): Event {
        return transaction {
            Events
                .join(Users, JoinType.INNER, additionalConstraint = { Events.host eq Users.id })
                .join(Subjects, JoinType.INNER, additionalConstraint = { Events.subject eq Subjects.id })
                .select { Events.id eq id }
                .map { DataMapper.mapToEvent(it, emptyList(), emptyList()) }
                .first()
        }
    }

    fun queryEvent(id: UUID): Option<Event> {
        return transaction {
            Events
                .join(Users, JoinType.INNER, additionalConstraint = { Events.host eq Users.id })
                .join(Subjects, JoinType.INNER, additionalConstraint = { Events.subject eq Subjects.id })
                .select { Events.id eq id }
                .firstOrNone()
                .map {
                    val tasks = Tasks.select { Tasks.event eq id }
                        .map { DataMapper.mapToTask(it) }

                    val guests = UsersInEvents
                        .join(Users, JoinType.INNER, additionalConstraint = { UsersInEvents.user eq Users.id })
                        .select { UsersInEvents.event eq id }
                        .map { DataMapper.mapToGuest(it) }
                    DataMapper.mapToEvent(it, tasks, guests)
                }

        }
    }

    fun queryTaskById(id: Int): Task {
        return transaction {
            Tasks.select { Tasks.id eq id }
                .map { DataMapper.mapToTask(it) }
                .first()
        }
    }


    fun generateTask(meetingId: UUID): Int {
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

    fun generateUser(mail: String): String {
        return transaction {
            Users.insert {
                it[id] = UUID.randomUUID()
                it[name] = "test"
                it[email] = mail
            } get Users.email
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

    fun queryUserByEmail(email: String): User {
        return transaction {
            Users.select { Users.email eq email }.map { DataMapper.mapToUser(it) }.first()
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

    fun generateSubject(subjectName: String, subjectDetails: String, subjectImageUrl: String? = null): UUID {
        return transaction {
            Subjects.insert {
                it[id] = UUID.randomUUID()
                it[name] = subjectName
                it[details] = subjectDetails
                it[imageUrl] = subjectImageUrl
            } get Subjects.id
        }
    }

    fun querySubjectById(id: UUID): Subject {
        lateinit var subject: Subject
        transaction {
            subject = Subjects
                .select { Subjects.id eq id }
                .map {
                    DataMapper.mapToSubject(it)
                }
                .firstOrNone()
                .getOrElse { throw RuntimeException("Error querying subjects") }
        }
        return subject
    }

    fun addGuestIntoEvent(eventId: UUID, guestId: UUID) {
        transaction {
            UsersInEvents.insert {
                it[event] = eventId
                it[user] = guestId
                it[status] = UserInEventStatus.Pending
            }
        }

    }

}
