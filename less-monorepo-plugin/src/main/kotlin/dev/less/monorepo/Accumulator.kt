package dev.less.monorepo

/** Mutable collector the detector fills while it inspects one directory's marker files. */
internal class Accumulator {
    val kinds = mutableSetOf<ModuleKind>()
    val tags = mutableListOf<String>()
    var workspace = false
}
