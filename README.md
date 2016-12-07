# WaveformView
Interface for displaying audio data as waveform for Android.

![alt tag](https://www.newventuresoftware.com/images/default-source/imported/device-2015-12-14-161713-576x1024.png)

The component supports 2 modes:
* Recording: Suitable for use when recording audio.
* Playback: Suitable for use when playing audio. Samples are displayed as a classical waveform with optional playback indicator.

##Examples:
Recording Example:

**activity_main.xml**
``` xml
<com.newventuresoftware.waveform.WaveformView
    android:layout_width="match_parent"
    android:layout_height="0dp"
    android:layout_weight="1"
    app:mode="RECORDING"
    android:background="#000000"
    android:id="@+id/waveformView" />
```
**MainActivity.java**
``` java
mRealtimeWaveformView = (WaveformView) findViewById(R.id.waveformView);
mRecordingThread = new RecordingThread(new AudioDataReceivedListener() {
    @Override
    public void onAudioDataReceived(short[] data) {
        mRealtimeWaveformView.setSamples(data);
    }
});
```

Playback Example:

**activity_main.xml**
``` xml
<com.newventuresoftware.waveform.WaveformView
    android:layout_width="match_parent"
    android:layout_height="0dp"
    android:layout_weight="1"
    app:mode="PLAYBACK"
    app:waveformStrokeThickness="3"
    app:waveformColor="#e5dc33"
    app:waveformFillColor="#e93519"
    android:background="#000000"
    android:id="@+id/playbackWaveformView" />
```
**MainActivity.java**
``` java
final WaveformView mPlaybackView = (WaveformView) findViewById(R.id.playbackWaveformView);
mPlaybackThread = new PlaybackThread(samples, new PlaybackListener() {
    @Override
    public void onProgress(int progress) {
        mPlaybackView.setMarkerPosition(progress);
    }
    @Override
    public void onCompletion() {
        mPlaybackView.setMarkerPosition(mPlaybackView.getAudioLength());
    }
});
mPlaybackView.setChannels(1);
mPlaybackView.setSampleRate(44100);
mPlaybackView.setSamples(samples);
```

For more information on recording and playback of raw audio in Android check out my [blog post] (http://www.newventuresoftware.com/blog/record-play-and-visualize-raw-audio-data-in-android/).

##License
MIT
