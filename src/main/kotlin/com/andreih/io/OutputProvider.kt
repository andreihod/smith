package com.andreih.io

/**
 * Interface for outputting results to the user.
 * Implemented by CLI, and can be implemented by API in the future.
 */
interface OutputProvider {
    fun showMessage(message: String)
    fun showResult(result: String)
}
