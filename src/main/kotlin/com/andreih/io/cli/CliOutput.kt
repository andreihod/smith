package com.andreih.io.cli

import com.andreih.io.OutputProvider

class CliOutput : OutputProvider {

    override fun showMessage(message: String) {
        println(message)
    }

    override fun showResult(result: String) {
        println(result)
    }
}
