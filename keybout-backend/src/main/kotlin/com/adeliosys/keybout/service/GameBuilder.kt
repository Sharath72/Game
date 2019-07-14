package com.adeliosys.keybout.service

import com.adeliosys.keybout.model.Game
import com.adeliosys.keybout.model.GameDescriptor
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource
import org.springframework.stereotype.Service
import org.springframework.web.socket.WebSocketSession
import javax.annotation.PostConstruct

@Service
class GameBuilder {

    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    private val MINIMUM_WORD_LENGTH = 5

    @Value("classpath:words-en.txt")
    private lateinit var wordsEn: Resource

    @Value("classpath:words-fr.txt")
    private lateinit var wordsFr: Resource

    private var wordsByLang = mutableMapOf<String, MutableList<String>>()

    @PostConstruct
    private fun init() {
        loadWords(wordsEn, "en")
        loadWords(wordsFr, "fr")
    }

    /**
     * Load the words for one language.
     */
    private fun loadWords(resource: Resource, lang: String) {
        logger.info("Loading words for '{}' language", lang)

        val words = mutableListOf<String>()

        // Uses UTF-8 encoding to read the file
        resource.file.forEachLine {
            if (it.length >= MINIMUM_WORD_LENGTH) {
                words.add(it)
            }
        }
        wordsByLang[lang] = words
    }

    fun build(descriptor: GameDescriptor, players: List<WebSocketSession>): Game {
        val possibleWords = wordsByLang[descriptor.language]!!
        val selectedWords = mutableListOf<String>()

        while (selectedWords.size < descriptor.words) {
            val selectedWord = possibleWords[(0..possibleWords.size).random()]

            // Check if no other word begins with the same letters
            var found = false
            for (word in selectedWords) {
                if (word.startsWith(selectedWord)) {
                    found = true
                    break
                }
            }
            if (!found) {
                selectedWords.add(selectedWord)
            }
        }

        return Game(descriptor.id, descriptor.rounds, selectedWords, descriptor.creator, players)
    }
}
