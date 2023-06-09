package com.adeliosys.keybout.api

import com.adeliosys.keybout.model.*
import com.adeliosys.keybout.model.Constants.ACTION_CLAIM_WORD
import com.adeliosys.keybout.model.Constants.ACTION_CONNECT
import com.adeliosys.keybout.model.Constants.ACTION_CREATE_GAME
import com.adeliosys.keybout.model.Constants.ACTION_DELETE_GAME
import com.adeliosys.keybout.model.Constants.ACTION_JOIN_GAME
import com.adeliosys.keybout.model.Constants.ACTION_LEAVE_GAME
import com.adeliosys.keybout.model.Constants.ACTION_QUIT_GAME
import com.adeliosys.keybout.model.Constants.ACTION_START_GAME
import com.adeliosys.keybout.model.Constants.ACTION_START_ROUND
import com.adeliosys.keybout.service.PlayService
import com.adeliosys.keybout.service.UserNameService
import com.adeliosys.keybout.util.*
import jakarta.annotation.PostConstruct
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.handler.TextWebSocketHandler

/**
 * Receive WebSocket messages, connects and disconnects,
 * then delegate the business processing to [PlayService].
 */
@Service
class PlayController(private val userNameService: UserNameService, private val playService: PlayService) : TextWebSocketHandler() {

    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    @Value("\${keybout.latency:0}")
    private var latency = 0L

    val usersCounter = Counter()

    @PostConstruct
    private fun postConstruct() {
        logger.info("Using latency of {} ms", latency)
    }

    override fun afterConnectionEstablished(session: WebSocketSession) {
        session.userName = ""
        session.setState(ClientState.OPENED, 0)
        usersCounter.increment()
    }

    override fun handleTextMessage(session: WebSocketSession, message: TextMessage) {
        try {
            if (latency > 0) {
                Thread.sleep(latency)
            }

            logger.trace("Received message '{}' for {}", message.payload, session.description)
            var processed = false
            val action = Action(message.payload)

            when (session.state) {
                ClientState.OPENED -> {
                    when (action.command) {
                        ACTION_CONNECT -> {
                            processed = true
                            val name = action.rawArguments
                            val notification = userNameService.registerUserName(name)
                            if (notification == null) {
                                session.userName = name
                                playService.goToLobby(session)
                            } else {
                                session.sendObjectMessage(notification)
                            }
                        }
                    }
                }
                ClientState.LOBBY -> {
                    when (action.command) {
                        ACTION_CREATE_GAME -> {
                            if (action.checkArgumentsCount(4)) {
                                processed = true
                                val gameDescriptor = GameDescriptor(
                                        session.userName,
                                        action.arguments[0],
                                        action.arguments[1],
                                        action.arguments[2],
                                        action.arguments[3])

                                playService.createGame(session, gameDescriptor)
                            }
                        }
                        ACTION_JOIN_GAME -> {
                            if (action.checkArgumentsCount(1)) {
                                processed = true
                                try {
                                    playService.joinGame(session, action.arguments[0].toLong())
                                } catch (e: NumberFormatException) {
                                    logger.warn("Invalid game ID in message '{}' for {}", message, session.description)
                                }
                            }
                        }
                    }
                }
                ClientState.CREATED -> {
                    when (action.command) {
                        ACTION_DELETE_GAME -> {
                            processed = true
                            playService.deleteGame(session)
                        }
                        ACTION_START_GAME -> {
                            processed = true
                            playService.startGame(session)
                        }
                    }
                }
                ClientState.JOINED -> {
                    when (action.command) {
                        ACTION_LEAVE_GAME -> {
                            processed = true
                            playService.leaveGame(session)
                        }
                    }
                }
                ClientState.PLAYING -> {
                    when (action.command) {
                        ACTION_CLAIM_WORD -> {
                            if (action.checkArgumentsCount(1)) {
                                processed = true
                                playService.claimWord(session, action.arguments[0])
                            }
                        }
                        ACTION_START_ROUND -> {
                            processed = true
                            playService.startRound(session)
                        }
                        ACTION_QUIT_GAME -> {
                            processed = true
                            playService.goToLobby(session)
                        }
                    }
                }
            }

            if (!processed) {
                logInvalidMessage(session, message)
            }
        } catch (e: Exception) {
            logger.error("Caught an exception during message processing", e)
        }
    }

    private fun logInvalidMessage(session: WebSocketSession, message: TextMessage) {
        logger.warn("Invalid message '{}' for state {} of {}", message.payload, session.state, session.description)
    }

    override fun afterConnectionClosed(session: WebSocketSession, status: CloseStatus) {
        usersCounter.decrement()

        playService.disconnect(session)

        userNameService.releaseUserName(session)

        logger.debug("Closed {} with code {} and reason '{}'",
                session.description,
                status.code,
                status.reason.orEmpty())
    }
}
