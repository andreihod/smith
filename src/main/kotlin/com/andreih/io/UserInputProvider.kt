package com.andreih.io

import com.andreih.domain.UserAssessment

/**
 * Interface for collecting user assessment data.
 * Implemented by CLI, and can be implemented by API in the future.
 */
interface UserInputProvider {
    fun collectUserAssessment(): UserAssessment
}
