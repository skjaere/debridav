package io.skjaere.debridav.sabnzbd

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SabnzbdHistoryResponse(
    /*@SerialName("noofslots") val noofSlots: Int,
    @SerialName("ppslots") val ppSlots: Int,
    @SerialName("day_size") val daySize: Int,
    @SerialName("week_size") val weekSize: Int,
    @SerialName("month_size") val monthSize: Int,
    @SerialName("total_size") val totalSize: Int,
    @SerialName("last_history_update") val lastHistoryUpdate: Int,*/
    val history: SabnzbdHistory
)

@Serializable
data class SabnzbdHistory(
    val slots: List<HistorySlot>
)

@Serializable
data class HistorySlot(

    @SerialName("fail_message") val failMessage: String,
    val bytes: Long,
    val category: String,
    @SerialName("nzb_name") val nzbName: String,
    @SerialName("download_time") val downloadTime: String,
    val storage: String,
    val status: String,
    @SerialName("nzo_id") val nzoId: String,
    val name: String,

    /*@SerialName("action_line") val actionLine: Int,
    @SerialName("duplicate_key") val duplicateKey: String,
    val meta: String? = null,
    val loaded: Boolean,
    val size: String,
    val pp: String,
    val retry: Int,
    val script: String,
    @SerialName("has_rating") val hasRating: String,
    @SerialName("script_line") val scriptLine: String,
    val completed: Long,
    val downloaded: Long,
    val report: String,
    val password: String,
    val path: String,
    @SerialName("postproc_time") val postProcTime: String,
    val url: String,
    val md5sum: String,
    val archive: Boolean,
    @SerialName("url_info") val urlInfo: String,
    @SerialName("stage_log") val stageLog: List<String> = emptyList(), */
)