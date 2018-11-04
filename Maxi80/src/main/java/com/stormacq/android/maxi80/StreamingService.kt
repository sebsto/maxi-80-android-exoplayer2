package com.stormacq.android.maxi80

import android.app.Service
import android.content.Intent
import android.net.Uri
import android.os.*
import android.util.Log
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util

import okhttp3.OkHttpClient
import saschpe.exoplayer2.ext.icy.IcyHttpDataSourceFactory

class StreamingService() : Service() {

    private var exoPlayer: SimpleExoPlayer? = null
    private val exoPlayerEventListener = ExoPlayerEventListener()
    private var streamURI: String? = null

    override fun onBind(intent: Intent?): IBinder? {
        Log.d(TAG, "onBind")

        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand")
        preparePlayer()
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy")
        releaseResources()
    }

    private fun preparePlayer() {
        val app = application as Maxi80Application

        if (exoPlayer == null) {
            exoPlayer = ExoPlayerFactory.newSimpleInstance(applicationContext,
                    DefaultRenderersFactory(applicationContext),
                    DefaultTrackSelector(),
                    DefaultLoadControl()
            )
            exoPlayer?.addListener(exoPlayerEventListener)
        }

        val audioAttributes = AudioAttributes.Builder()
                .setContentType(C.CONTENT_TYPE_MUSIC)
                .setUsage(C.USAGE_MEDIA)
                .build()
        exoPlayer?.audioAttributes = audioAttributes

        // Custom HTTP data source factory which requests Icy metadata and parses it
        val client = OkHttpClient.Builder().build()
        val icyHttpDataSourceFactory = IcyHttpDataSourceFactory.Builder(client)
                .setUserAgent(Util.getUserAgent(applicationContext, app.station.name()))
                .setIcyHeadersListener { icyHeaders ->
                    Log.d(TAG, "onIcyMetaData: icyHeaders=$icyHeaders")
                }
                .setIcyMetadataChangeListener { icyMetadata ->
                    Log.d(TAG, "onIcyMetaData: icyMetadata=$icyMetadata")
                    app.handleiCyMetaData(icyMetadata.streamTitle)
                }
                .build()

        // Produces DataSource instances through which media data is loaded
        val dataSourceFactory = DefaultDataSourceFactory(
                applicationContext, null, icyHttpDataSourceFactory
        )
        // Produces Extractor instances for parsing the media data
        val extractorsFactory = DefaultExtractorsFactory()

        // The MediaSource represents the media to be played
        val mediaSource = ExtractorMediaSource.Factory(dataSourceFactory)
                .setExtractorsFactory(extractorsFactory)
                .createMediaSource(Uri.parse(streamURI ?: "https://audio1.maxi80.com"))

        // Prepares media to play (happens on background thread) and triggers
        // {@code onPlayerStateChanged} callback when the stream is ready to play
        exoPlayer?.prepare(mediaSource)
        exoPlayer?.playWhenReady = true

    }

    private fun releaseResources() {
        Log.d(TAG, "releaseResources") //: releasePlayer=$releasePlayer")

        // Stops and releases player (if requested and available).
        if (exoPlayer != null) {
            exoPlayer?.release()
            exoPlayer?.removeListener(exoPlayerEventListener)
            exoPlayer = null

            val app = application as Maxi80Application
            app.isPlaying = false
        }
    }

    companion object {
        private const val TAG = "Maxi80_StreamingService"
        private const val MINIMUM_SDK_FEATURES = 20
    }

    /**************************************************************************
     *
     * Exo Player callback
     *
     *************************************************************************/


    private inner class ExoPlayerEventListener : Player.EventListener {
        override fun onTimelineChanged(timeline: Timeline, manifest: Any?, reason: Int) {
        }

        override fun onTracksChanged(trackGroups: TrackGroupArray, trackSelections: TrackSelectionArray) {
        }

        override fun onLoadingChanged(isLoading: Boolean) {
        }

        override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
            Log.i(TAG, "onPlayerStateChanged: playWhenReady=$playWhenReady, playbackState=$playbackState")
            when (playbackState) {
                Player.STATE_IDLE ->
                    Log.d(TAG, "player idle")

                Player.STATE_BUFFERING -> {
                    Log.d(TAG, "player buffering")
                }
                Player.STATE_READY -> {
                    Log.d(TAG, "player ready")
                    val app = application as Maxi80Application
                    app.isPlaying = true
                }

                Player.STATE_ENDED -> {
                    Log.d(TAG, "player ended")
                    releaseResources()
                }
            }
        }

        override fun onPlayerError(error: ExoPlaybackException) {
            Log.e(TAG, "onPlayerError: error=$error")
        }

        override fun onPositionDiscontinuity(reason: Int) {
        }

        override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) {
        }

        override fun onSeekProcessed() {
        }

        override fun onRepeatModeChanged(repeatMode: Int) {
        }

        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
        }
    } // end of private inner class
}

