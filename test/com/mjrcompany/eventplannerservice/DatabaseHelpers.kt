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
import java.time.LocalDate
import java.util.*


object TestDatabaseHelper {

    fun addMeeting(uuid: UUID): UUID {
        lateinit var eventId: UUID
        val hostId =
            addUser(
                UUID.randomUUID()
            )
        val dishId =
            addDish(
                UUID.randomUUID()
            )
        transaction {
            eventId = Events.insert {
                it[id] = uuid
                it[title] = "test"
                it[host] = hostId
                it[subject] = dishId
                it[date] = LocalDate.now()
                it[address] = "somwhere"
                it[maxNumberGuests] = 10
                it[createDate] = Instant.now()
                it[totalCost] = BigDecimal(10)
                it[status] = EventStatus.Open
            } get Events.id
        }
        return eventId
    }

    fun addFriendsInMeeting(friendId: UUID, meetingId: UUID) {
        transaction {
            UsersInEvents.insert {
                it[event] = meetingId
                it[user] = friendId
                it[status] = UserInEventStatus.Pending
            }
        }
    }

    fun queryMeetingWithoutTasks(id: UUID): Event {
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


    // FIXME move to another object
    fun getDefaultCreateMeetingDTO(hostId: UUID, dishId: UUID): EventWritable {
        return EventWritable(
            "test",
            hostId,
            dishId,
            LocalDate.now(),
            "here",
            10,
            BigDecimal(10),
            "something"
        )
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

    fun addUser(uuid: UUID): UUID {
        transaction {
            Users.insert {
                it[id] = uuid
                it[name] = "test"
                it[email] = "test@email.com"
            }
        }
        return uuid
    }

    fun addUser(uuid: UUID, hostName: String, hostEmail: String): UUID {
        transaction {
            Users.insert {
                it[id] = uuid
                it[name] = hostName
                it[email] = hostEmail
            }
        }
        return uuid
    }


    fun addDish(uuid: UUID): UUID {
        transaction {
            Subjects.insert {
                it[id] = uuid
                it[name] = "test"
            }
        }
        return uuid
    }


    fun addDish(uuid: UUID, dishName: String): UUID {
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
