package com.bestboy.sounds.ui.components
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bestboy.sounds.ui.theme.*
import com.bestboy.sounds.ui.viewmodel.MusicTrack
@Composable fun MiniPlayer(currentTrack:MusicTrack?,isPlaying:Boolean,progress:Float,onPlayPauseClick:()->Unit,onNextClick:()->Unit,onClick:()->Unit){if(currentTrack==null)return;Column(Modifier.fillMaxWidth().background(CardPurple).clickable{onClick()}){LinearProgressIndicator(progress={progress},modifier=Modifier.fillMaxWidth().height(2.dp),color=ElectricViolet,trackColor=BorderPurple);Row(Modifier.fillMaxWidth().padding(horizontal=14.dp,vertical=7.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text(currentTrack.title,color=TextPrimary,maxLines=1);Text(currentTrack.artist,color=TextMuted,maxLines=1,style=MaterialTheme.typography.bodySmall)};IconButton(onClick=onPlayPauseClick){Icon(if(isPlaying)Icons.Rounded.Pause else Icons.Rounded.PlayArrow,null,tint=ElectricViolet)};IconButton(onClick=onNextClick){Icon(Icons.Rounded.SkipNext,null,tint=TextPrimary)}}}}
