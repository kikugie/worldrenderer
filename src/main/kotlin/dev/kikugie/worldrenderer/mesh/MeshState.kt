package dev.kikugie.worldrenderer.mesh

enum class MeshState(val isBuildStage: Boolean, val canRender: Boolean) {
    NEW(false, false),
    BUILDING(true, false),
    REBUILDING(true, true),
    READY(false, true),
    CORRUPT(false, false)
}