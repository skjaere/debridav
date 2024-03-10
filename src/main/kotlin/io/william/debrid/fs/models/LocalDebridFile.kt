package io.william.debrid.fs.models

class LocalDebridFile(
    override var id: Long?,
    override var name: String?,
    override var path: String?,
    override var size: Long?,
    override var directory: Directory?
) : DebriDavFile {
}