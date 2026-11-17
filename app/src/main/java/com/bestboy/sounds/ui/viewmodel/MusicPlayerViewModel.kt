package com.bestboy.sounds.ui.viewmodel

import android.app.Application
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.content.ComponentName
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.bestboy.sounds.R
import com.bestboy.sounds.service.BestboyMediaService
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class MusicTrack(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val uri: String,
    val durationMs: Long
)

data class MusicUiState(
    val tracks: List<MusicTrack> = emptyList(),
    val currentTrack: MusicTrack? = null,
    val isPlaying: Boolean = false,
    val currentPositionMs: Long = 0,
    val durationMs: Long = 0,
    val activeTab: Int = 0,
    val query: String = "",
    val shuffle: Boolean = false,
    val repeatMode: Int = 0,
    val playlists: List<String> = listOf("Favoris", "Mes morceaux"),
    val favoriteIds: Set<Long> = emptySet()
)

private data class BundledSong(
    val resourceId: Int,
    val title: String,
    val artist: String = "Bestboy Tunechi"
)

class MusicPlayerViewModel(app: Application) : AndroidViewModel(app) {

    private val _uiState = MutableStateFlow(MusicUiState())
    val uiState: StateFlow<MusicUiState> = _uiState.asStateFlow()

    private var player: MediaController? = null
    private var future: com.google.common.util.concurrent.ListenableFuture<MediaController>? = null

    private val bundledSongs = listOf(
        BundledSong(R.raw.song_a_foriyer_by_izi_khalifa_1_104208, "A Foriyer", "Izi Khalifa"),
        BundledSong(R.raw.song_best_boy_ft_demboy_ate_ban_by_junior_make_in_sounds, "Best Boy - Ate Ban", "Junior Make"),
        BundledSong(R.raw.song_best_boy_x_shimigamy_x_knk_oser_lutter_by_izi_khalifa_1, "Best Boy - Oser Lutter", "Izi Khalifa"),
        BundledSong(R.raw.song_best_boy_i_need_you_now_by_izi_khalifa, "I Need You Now", "Izi Khalifa"),
        BundledSong(R.raw.song_best_boy_ich_nin_nin_by_izi_khalifa_rappel_concert_en_dembre_chez_bekaye_ba, "Ich Nin Nin", "Izi Khalifa"),
        BundledSong(R.raw.song_best_boy_nouveau_depart_by_izi_khalifa, "Nouveau Depart", "Izi Khalifa"),
        BundledSong(R.raw.song_best_boy_trap_dans_le_sang_by_izi_khalifa_1, "Trap dans le sang", "Izi Khalifa"),
        BundledSong(R.raw.song_best_boys_revolution_by_izi_khalifa, "Best Boys Revolution", "Izi Khalifa"),
        BundledSong(R.raw.song_og_boys_sissan_sissan, "OG Boys - Sissan Sissan"),
        BundledSong(R.raw.song_rio_de_janerio, "Rio de Janeiro"),
        BundledSong(R.raw.song_tunechi_paou_ka_guirin_by_m_o_zy, "Paou Ka Guirin", "M.O.ZY"),
        BundledSong(R.raw.song_tunechi_vampire_by_m_o_zy_mp3, "Vampire", "M.O.ZY"),
        BundledSong(R.raw.song_tunechi_feat_black_noir_1, "Tunechi feat. Black Noir"),
        BundledSong(R.raw.song_tunechi_ft_baben_favela_by_m_o_zy_mp3, "Tunechi feat. Baben Favela", "M.O.ZY"),
        BundledSong(R.raw.song_tunechi_a_mo_chi_by_izi_khalifa, "A Mo Chi", "Izi Khalifa"),
        BundledSong(R.raw.song_tunechi_boy_feat_luka_mo_boul_chee_djandjo_999998, "Mo Boul Chee", "Luka / Djandjo"),
        BundledSong(R.raw.song_tunechie, "Tunechie"),
        BundledSong(R.raw.song_tunechi_boy_x_boy_neiroja_gang_128k, "Tunechi Boy x Boy Neiroja Gang"),
        BundledSong(R.raw.song_tunichie_prod_by_toni_p_1_1, "Tunichie", "Toni P"),
        BundledSong(R.raw.song_tweerk_assan_fall_1, "Twerk Assan Fall"),
        BundledSong(R.raw.song_tunechi_a_z_by_m_o_zy, "A Z", "M.O.ZY"),
        BundledSong(R.raw.song_tunechi_a_to_ne_tala_by_petit_loup_on_the_track, "A To Ne Tala", "Petit Loup"),
        BundledSong(R.raw.song_tunechi_rnb_prod_by_toni_p_izi_1, "Tunechi RnB", "Toni P"),
        BundledSong(R.raw.song_tunechi_rintin_tigui_prod_by_doucara, "Rintin Tigui", "Doucara"),
        BundledSong(R.raw.song_tunechi_delingan_kabako_de_d0_by_m_o_zy, "Delingan Kabako", "M.O.ZY"),
        BundledSong(R.raw.song_tunechi_ish_ne_ne_128k, "Ish Ne Ne"),
        BundledSong(R.raw.song_tunechi_boy_chocolat_999998, "Tunechi Boy - Chocolat"),
        BundledSong(R.raw.song_tunechi_boy_dictature_128k, "Tunechi Boy - Dictature"),
        BundledSong(R.raw.song_tunechi_boy_fendo_999998, "Tunechi Boy - Fendo"),
        BundledSong(R.raw.song_tunechi_boy_mon_jour_arrivera_999998, "Mon Jour Arrivera"),
        BundledSong(R.raw.song_tunechi_boy_pok_mon_256k_104234, "Pok Mon"),
        BundledSong(R.raw.song_tunechi_boy_pro_17_999998, "Pro 17"),
        BundledSong(R.raw.song_tunechi_boy_succ_s_999998, "Succes"),
        BundledSong(R.raw.song_tunechi_boy_ou_ma_kai_ne_gnongon_999998, "Ou Ma Kai Ne Gnongon")
    )

    init {
        val token = SessionToken(app, ComponentName(app, BestboyMediaService::class.java))
        future = MediaController.Builder(app, token).buildAsync()

        future?.addListener({
            try {
                player = future?.get()
                sync()
            } catch (_: Exception) {
            }
        }, MoreExecutors.directExecutor())

        loadTracks()

        viewModelScope.launch {
            while (true) {
                sync()
                delay(500)
            }
        }
    }

    private fun createTrack(app: Application, song: BundledSong): MusicTrack {
        val uri = Uri.Builder()
            .scheme("android.resource")
            .authority(app.packageName)
            .appendPath(song.resourceId.toString())
            .build()

        var title = song.title
        var artist = song.artist
        var duration = 0L

        val retriever = MediaMetadataRetriever()

        try {
            retriever.setDataSource(app, uri)

            title = retriever.extractMetadata(
                MediaMetadataRetriever.METADATA_KEY_TITLE
            )?.takeIf { it.isNotBlank() && !it.equals("<unknown>", true) } ?: title

            artist = retriever.extractMetadata(
                MediaMetadataRetriever.METADATA_KEY_ARTIST
            )?.takeIf { it.isNotBlank() && !it.equals("<unknown>", true) } ?: artist

            duration = retriever.extractMetadata(
                MediaMetadataRetriever.METADATA_KEY_DURATION
            )?.toLongOrNull() ?: 0L

        } catch (_: Exception) {
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {
            }
        }

        return MusicTrack(
            id = song.resourceId.toLong(),
            title = title,
            artist = artist,
            album = "Bestboy Sounds",
            uri = uri.toString(),
            durationMs = duration
        )
    }

    fun loadTracks() {
        viewModelScope.launch {
            val app = getApplication<Application>()

            val found = withContext(Dispatchers.IO) {
                bundledSongs.map { createTrack(app, it) }
            }

            _uiState.update {
                it.copy(tracks = found)
            }
        }
    }

    fun playTrack(track: MusicTrack) {
        val p = player ?: return
        val tracks = _uiState.value.tracks
        if (tracks.isEmpty()) return

        val items = tracks.map { t ->
            MediaItem.Builder()
                .setMediaId(t.id.toString())
                .setUri(t.uri)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(t.title)
                        .setArtist(t.artist)
                        .setAlbumTitle(t.album)
                        .build()
                )
                .build()
        }

        val index = tracks.indexOfFirst { it.id == track.id }
        if (index < 0) return

        p.setMediaItems(items, index, 0L)
        p.prepare()
        p.play()
        sync()
    }

    fun togglePlayPause() {
        player?.let {
            if (it.isPlaying) it.pause() else it.play()
        }
        sync()
    }

    fun skipNext(context: android.content.Context? = null) {
        player?.seekToNextMediaItem()
        sync()
    }

    fun skipPrevious() {
        player?.seekToPreviousMediaItem()
        sync()
    }

    fun seekTo(ms: Long) {
        player?.seekTo(ms)
        sync()
    }

    fun setActiveTab(tab: Int) {
        _uiState.update { it.copy(activeTab = tab) }
    }

    fun setQuery(q: String) {
        _uiState.update { it.copy(query = q) }
    }

    fun toggleShuffle() {
        player?.let { it.shuffleModeEnabled = !it.shuffleModeEnabled }
        sync()
    }

    fun cycleRepeat() {
        player?.let {
            it.repeatMode = when (it.repeatMode) {
                Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                else -> Player.REPEAT_MODE_OFF
            }
        }
        sync()
    }

    fun toggleFavorite() {
        val id = _uiState.value.currentTrack?.id ?: return
        _uiState.update {
            it.copy(
                favoriteIds = if (id in it.favoriteIds) {
                    it.favoriteIds - id
                } else {
                    it.favoriteIds + id
                }
            )
        }
    }

    fun addPlaylist(name: String) {
        if (name.isNotBlank()) {
            _uiState.update { it.copy(playlists = it.playlists + name.trim()) }
        }
    }

    private fun sync() {
        val p = player ?: return
        val mediaItem = p.currentMediaItem
        val id = mediaItem?.mediaId?.toLongOrNull()

        val track = _uiState.value.tracks.firstOrNull { it.id == id }
            ?: mediaItem?.let {
                MusicTrack(
                    id ?: -1,
                    it.mediaMetadata.title?.toString() ?: "Lecture en cours",
                    it.mediaMetadata.artist?.toString() ?: "Bestboy Sounds",
                    "",
                    it.localConfiguration?.uri?.toString() ?: "",
                    p.duration.coerceAtLeast(0)
                )
            }

        _uiState.update {
            it.copy(
                currentTrack = track,
                isPlaying = p.isPlaying,
                currentPositionMs = p.currentPosition.coerceAtLeast(0),
                durationMs = p.duration.coerceAtLeast(0),
                shuffle = p.shuffleModeEnabled,
                repeatMode = p.repeatMode
            )
        }
    }

    override fun onCleared() {
        future?.let { MediaController.releaseFuture(it) }
        player = null
        super.onCleared()
    }
}
