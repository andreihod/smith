package com.andreih.agent.memory

import ai.koog.agents.memory.model.Concept
import ai.koog.agents.memory.model.Fact
import ai.koog.agents.memory.model.MemoryScope
import ai.koog.agents.memory.model.MemorySubject
import ai.koog.agents.memory.providers.AgentMemoryProvider

class InMemoryProvider : AgentMemoryProvider {
    private val facts = mutableListOf<Triple<Fact, MemorySubject, MemoryScope>>()

    override suspend fun save(fact: Fact, subject: MemorySubject, scope: MemoryScope) {
        facts.add(Triple(fact, subject, scope))
    }

    override suspend fun load(concept: Concept, subject: MemorySubject, scope: MemoryScope): List<Fact> {
        return facts
            .filter { it.first.concept.keyword == concept.keyword && it.second == subject }
            .map { it.first }
    }

    override suspend fun loadAll(subject: MemorySubject, scope: MemoryScope): List<Fact> {
        return facts.filter { it.second == subject }.map { it.first }
    }

    override suspend fun loadByDescription(description: String, subject: MemorySubject, scope: MemoryScope): List<Fact> {
        return facts
            .filter { it.second == subject && it.first.concept.description.contains(description, ignoreCase = true) }
            .map { it.first }
    }
}
