package com.mjrcompany.eventplannerservice.com.mjrcompany.eventplannerservice.routes


import com.mjrcompany.eventplannerservice.UserEmailAttributeKey
import com.mjrcompany.eventplannerservice.com.mjrcompany.eventplannerservice.authorization.withHostRequestPermission
import com.mjrcompany.eventplannerservice.com.mjrcompany.eventplannerservice.core.EventOrderBy
import com.mjrcompany.eventplannerservice.com.mjrcompany.eventplannerservice.core.TaskOrderBy
import com.mjrcompany.eventplannerservice.com.mjrcompany.eventplannerservice.domain.EventDTO
import com.mjrcompany.eventplannerservice.core.*
import com.mjrcompany.eventplannerservice.domain.AcceptGuestInEventWritable
import com.mjrcompany.eventplannerservice.domain.EventSubscriberWritable
import com.mjrcompany.eventplannerservice.domain.TaskOwnerWritable
import com.mjrcompany.eventplannerservice.event.EventService
import com.mjrcompany.eventplannerservice.tasks.TaskService
import com.mjrcompany.eventplannerservice.withFriendInEventRequestPermission
import com.mjrcompany.eventplannerservice.withHostRequestPermission
import com.mjrcompany.eventplannerservice.withIdToken
import io.ktor.application.call
import io.ktor.auth.authenticate
import io.ktor.http.HttpStatusCode
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.post
import io.ktor.routing.put
import io.ktor.routing.route
import io.ktor.util.KtorExperimentalAPI


@KtorExperimentalAPI
fun Route.events() {

    route("/events") {

        // this is a experimental function that I've created.
        // The plan is: after add this function all CRUD method will be provided to this resource
        CrudRestApi.createResource(
            this,
            getDefaultIdAsUUID,
            EventService.crudResources,
            EventOrderBy.orderBy
        )

        // similar to the previous function, but for a sub resource:
        CrudRestApi.createSubResource(
            this, "/tasks",
            getDefaultIdAsUUID,
            getIdAsInt,
            TaskService.crudResources,
            TaskOrderBy.orderBy,
            withPermissionToModify = withHostRequestPermission
        )

        // Create event does not follow the patter for a regular CRUD resource,
        // because of that the POST and PUT method are not provided by CRUD Api
        authenticate {
            withIdToken {
                post("/") {
                    val event = call.receive<EventDTO>()
                    val userEmail = call.attributes.get(UserEmailAttributeKey)

                    val (status, body) = withValidRequest(event) {
                        HttpStatusCode.Created to
                                EventService.createEvent(
                                    userEmail,
                                    event
                                )
                    }
                    call.respond(status, body)
                }

                withHostRequestPermission {
                    put("/{id}") {
                        val dto = call.receive<EventDTO>()
                        val id = call.getParamIdAsUUID()
                        val userEmail = call.attributes.get(UserEmailAttributeKey)

                        val (status, body) = withValidRequest(dto) { eventDTO ->
                            HttpStatusCode.Accepted to EventService.updateEvent(userEmail, id, eventDTO)
                        }

                        call.respond(status, body)
                    }
                }
            }

            post("{id}/subscribe") {
                val dto = call.receive<EventSubscriberWritable>()
                val meetingId = call.getParamIdAsUUID()
                val (status, body) = withValidRequest(dto) {
                    HttpStatusCode.Accepted to EventService.subscribeEvent(meetingId, it)
                }
                call.respond(status, body)
            }

            withHostRequestPermission {
                post("{id}/accept-guest") {
                    val dto = call.receive<AcceptGuestInEventWritable>()
                    val eventId = call.getParamIdAsUUID()

                    val (status, body) = withValidRequest(dto) {
                        HttpStatusCode.Accepted to EventService.acceptGuest(eventId, it)
                    }
                    call.respond(status, body)
                }
            }
        }

        // sub resources: /tasks
        withFriendInEventRequestPermission {
            route("{id}/tasks") {
                authenticate {
                    post("/{subId}/accept") {
                        val dto = call.receive<TaskOwnerWritable>()
                        val eventId = call.getParamIdAsUUID()
                        val taskId = call.getParamSubIdAsInt()

                        val (status, body) = withValidRequest(dto) {
                            HttpStatusCode.Accepted to TaskService.acceptTask(taskId, eventId, it)
                        }
                        call.respond(status, body)
                    }
                }
            }
        }
    }
}



