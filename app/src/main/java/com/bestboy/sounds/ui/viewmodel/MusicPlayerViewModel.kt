package com.bestboy.sounds.ui.viewmodel

import android.app.Application
import android.content.ContentUris
import android.content.ComponentName
import android.provider.MediaStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.bestboy.sounds.service.BestboyMediaService
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MusicTrack(val id: Long, val title: String, val artist: String, val album: String, val uri: String, val durationMs: Long)
data class MusicUiState(val tracks: List<MusicTrack> = emptyList(), val currentTrack: MusicTrack? = null, val isPlaying: Boolean = false, val currentPositionMs: Long = 0, val durationMs: Long = 0, val activeTab: Int = 0, val query: String = "", val shuffle: Boolean = false, val repeatMode: Int = 0, val playlists: List<String> = listOf("Favoris", "Mes morceaux"), val favoriteIds: Set<Long> = emptySet())

class MusicPlayerViewModel(app: Application) : AndroidViewModel(app) {
    private val _uiState = MutableStateFlow(MusicUiState())
    val uiState: StateFlow<MusicUiState> = _uiState.asStateFlow()
    private var player: MediaController? = null
    private var future: com.google.common.util.concurrent.ListenableFuture<MediaController>? = null
    init {
        val token = SessionToken(app, ComponentName(app, BestboyMediaService::class.java))
        future = MediaController.Builder(app, token).buildAsync()
        future?.addListener({ try { player = future?.get(); sync() } catch (_: Exception) { } }, MoreExecutors.directExecutor())
        loadTracks()
        viewModelScope.launch { while (true) { sync(); delay(500) } }
    }
    fun loadTracks() {
        val found = mutableListOf<MusicTrack>(); val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val cols = arrayOf(MediaStore.Audio.Media._ID, MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.ARTIST, MediaStore.Audio.Media.ALBUM, MediaStore.Audio.Media.DURATION)
        try { getApplication<Application>().contentResolver.query(uri, cols, "${MediaStore.Audio.Media.IS_MUSIC} != 0", null, "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC")?.use { c ->
            val id=c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID); val title=c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE); val artist=c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST); val album=c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM); val dur=c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            while(c.moveToNext()){ val key=c.getLong(id); found += MusicTrack(key,c.getString(title)?:"Titre inconnu",c.getString(artist)?.takeUnless{it=="<unknown>"}?:"Artiste inconnu",c.getString(album)?:"",ContentUris.withAppendedId(uri,key).toString(),c.getLong(dur)) }
        } } catch (_: SecurityException) { }
        _uiState.update { it.copy(tracks=found) }
    }
    fun playTrack(track: MusicTrack) { val p=player?:return; val tracks=_uiState.value.tracks; val items=tracks.map { t -> MediaItem.Builder().setMediaId(t.id.toString()).setUri(t.uri).setMediaMetadata(MediaMetadata.Builder().setTitle(t.title).setArtist(t.artist).setAlbumTitle(t.album).build()).build() }; val index=tracks.indexOfFirst{it.id==track.id}.coerceAtLeast(0); p.setMediaItems(items,index,0); p.prepare(); p.play(); sync() }
    fun togglePlayPause(){ player?.let{if(it.isPlaying)it.pause() else it.play()}; sync() }
    fun skipNext(context: android.content.Context? = null){player?.seekToNextMediaItem();sync()}
    fun skipPrevious(){player?.seekToPreviousMediaItem();sync()}
    fun seekTo(ms:Long){player?.seekTo(ms);sync()}
    fun setActiveTab(tab:Int){_uiState.update{it.copy(activeTab=tab)}}
    fun setQuery(q:String){_uiState.update{it.copy(query=q)}}
    fun toggleShuffle(){player?.let{it.shuffleModeEnabled=!it.shuffleModeEnabled};sync()}
    fun cycleRepeat(){player?.let{it.repeatMode=when(it.repeatMode){Player.REPEAT_MODE_OFF->Player.REPEAT_MODE_ALL;Player.REPEAT_MODE_ALL->Player.REPEAT_MODE_ONE;else->Player.REPEAT_MODE_OFF}};sync()}
    fun toggleFavorite(){val id=_uiState.value.currentTrack?.id?:return;_uiState.update{it.copy(favoriteIds=if(id in it.favoriteIds)it.favoriteIds-id else it.favoriteIds+id)}}
    fun addPlaylist(name:String){if(name.isNotBlank())_uiState.update{it.copy(playlists=it.playlists+name.trim())}}
    private fun sync(){val p=player?:return;val m=p.currentMediaItem;val id=m?.mediaId?.toLongOrNull();val track=_uiState.value.tracks.firstOrNull{it.id==id}?:m?.let{MusicTrack(id?:-1,it.mediaMetadata.title?.toString()?:"Lecture en cours",it.mediaMetadata.artist?.toString()?:"Bestboy Sounds","",it.localConfiguration?.uri?.toString()?:"",p.duration.coerceAtLeast(0))};_uiState.update{it.copy(currentTrack=track,isPlaying=p.isPlaying,currentPositionMs=p.currentPosition.coerceAtLeast(0),durationMs=p.duration.coerceAtLeast(0),shuffle=p.shuffleModeEnabled,repeatMode=p.repeatMode)}}
    override fun onCleared(){future?.let{MediaController.releaseFuture(it)};player=null;super.onCleared()}
}
