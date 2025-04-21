package gurumirum.sad.app

import gurumirum.sad.Hash
import gurumirum.sad.script.OperationType

data class OpHash(
    val type: OperationType,
    val hash: Hash
)
