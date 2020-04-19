package com.mjrcompany.eventplannerservice.tasks

import arrow.core.*
import com.mjrcompany.eventplannerservice.FriendNotInMeetingException
import com.mjrcompany.eventplannerservice.NotFoundException
import com.mjrcompany.eventplannerservice.com.mjrcompany.eventplannerservice.database.withDatabaseErrorTreatment
import com.mjrcompany.eventplannerservice.core.CrudSubResource
import com.mjrcompany.eventplannerservice.core.ServiceResult
import com.mjrcompany.eventplannerservice.domain.Task
import com.mjrcompany.eventplannerservice.domain.TaskOwnerWritable
import com.mjrcompany.eventplannerservice.domain.TaskWritable
import com.mjrcompany.eventplannerservice.meetings.MeetingService
import org.slf4j.LoggerFactory
import java.util.*


object TaskService {
    private var log = LoggerFactory.getLogger(TaskService::class.java)


    val createTask = fun(meetingId: UUID, task: TaskWritable): ServiceResult<Int> {
        log.info("will create the task: $task")
        val result = withDatabaseErrorTreatment {
            TaskRepository.createTask(
                meetingId,
                task
            )
        }

        if (result.isLeft()) {
            log.error("error creating task: $task")
        }

        return result
    }

    val updateTask = fun(id: Int, meetingId: UUID, task: TaskWritable): ServiceResult<Unit> {
        log.info("will update the task: $task")
        val result = withDatabaseErrorTreatment {
            TaskRepository.updateTask(
                id,
                meetingId,
                task
            )
        }

        if (result.isLeft()) {
            log.error("error creating task: $task")
        }

        return result
    }

    val getTask = fun(id: Int, meetingId: UUID): ServiceResult<Option<Task>> {

        log.debug("Querying the task: $id")
        val result = withDatabaseErrorTreatment {
            TaskRepository.getTaskById(id, meetingId)
        }
        result.map { if (it.isEmpty()) log.info("task not found") }
        return result
    }

    val getTasksInMeeting = fun(meetingId: UUID): ServiceResult<List<Task>> {
        return withDatabaseErrorTreatment {
            TaskRepository.getAllTasksInMeeting(
                meetingId
            )
        }
    }

    val acceptTask = fun(taskId: Int, meetingId: UUID, taskOwner: TaskOwnerWritable): ServiceResult<Unit> {
        log.info("will accept the task. taskIdk $taskId , task owner: $taskOwner")
        val meeting = MeetingService.getMeeting(meetingId)

        if (meeting.isLeft()) {
            Either.left(FriendNotInMeetingException("The friends has to be added to the meeting before accept this task"))
        }

        return meeting.flatMap {
            when (it) {
                is None -> Either.left(NotFoundException("Meeting not found!"))
                is Some -> {
                    if (it.t.friends.map { friend -> friend.id }.contains(taskOwner.friendId)) {
                        Either.right(Unit)
                    } else {
                        Either.left(FriendNotInMeetingException("The friends has to be added to the meeting before accept this task"))
                    }

                }
            }.flatMap {
                withDatabaseErrorTreatment {
                    TaskRepository.updateTaskOwner(
                        taskId,
                        meetingId,
                        taskOwner
                    )
                }
            }
        }
    }

    val crudResources = CrudSubResource(
        createTask,
        updateTask,
        getTask,
        getTasksInMeeting
    )

}


