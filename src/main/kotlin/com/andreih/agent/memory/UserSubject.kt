package com.andreih.agent.memory

import ai.koog.agents.memory.model.MemorySubject

object UserSubject : MemorySubject() {
    override val name: String = "user"
    override val promptDescription: String = "User information (personal details, health goals, dietary restrictions, etc.)"
    override val priorityLevel: Int = 1
}
