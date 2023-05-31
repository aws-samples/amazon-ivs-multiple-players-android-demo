package com.amazon.ivs.multiple.players.common

import timber.log.Timber

private const val TIMBER_TAG = "MultiplePlayers"
class LineNumberDebugTree : Timber.DebugTree() {
    override fun createStackElementTag(element: StackTraceElement) =
        "$TIMBER_TAG: (${element.fileName}:${element.lineNumber}) #${element.methodName} "
}
