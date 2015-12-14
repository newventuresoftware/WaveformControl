/*
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.newventuresoftware.waveformdemo;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.nio.ShortBuffer;

public class PlaybackThread {
    static final int SAMPLE_RATE = 44100;
    static final int CHANNELS = 1;
    private static final String LOG_TAG = PlaybackThread.class.getSimpleName();

    public PlaybackThread(short[] samples, PlaybackListener listener) {
        mSamples = ShortBuffer.wrap(samples);
        mNumSamples = samples.length;
        mListener = listener;
        initAudioTrack();
    }

    private Thread mThread;
    private boolean mShouldContinue;
    private ShortBuffer mSamples;
    private AudioTrack mAudioTrack;
    private int mNumSamples, mBufferSize;
    private PlaybackListener mListener;

    private void initAudioTrack() {
        int audioFormat = (CHANNELS == 2 ? AudioFormat.CHANNEL_OUT_STEREO :
                AudioFormat.CHANNEL_OUT_MONO);

        mBufferSize = AudioTrack.getMinBufferSize(SAMPLE_RATE, audioFormat,
                AudioFormat.ENCODING_PCM_16BIT);
        if (mBufferSize == AudioTrack.ERROR || mBufferSize == AudioTrack.ERROR_BAD_VALUE) {
            mBufferSize = SAMPLE_RATE * CHANNELS * 2;
        }

        mAudioTrack = new AudioTrack(
                AudioManager.STREAM_MUSIC,
                SAMPLE_RATE,
                audioFormat,
                AudioFormat.ENCODING_PCM_16BIT,
                mBufferSize,
                AudioTrack.MODE_STREAM);

        mAudioTrack.setPlaybackPositionUpdateListener(
            new AudioTrack.OnPlaybackPositionUpdateListener() {
                @Override
                public void onPeriodicNotification(AudioTrack track) {
                    if (mListener != null && playing()) {
                        mListener.onProgress((track.getPlaybackHeadPosition() * 1000) / SAMPLE_RATE);
                    }
                }

                @Override
                public void onMarkerReached(AudioTrack track) {
                    Log.v(LOG_TAG, "Audio file end reached");
                    stopPlayback();
                    if (mListener != null) {
                        mListener.onCompletion();
                    }
                }
            }, new Handler(Looper.getMainLooper()));
    }

    public boolean playing() {
        return mThread != null;
    }

    public void startPlayback() {
        if (mThread != null || mAudioTrack.getState() != AudioTrack.STATE_INITIALIZED)
            return;

        mAudioTrack.flush();

        // Start streaming in a thread
        mShouldContinue = true;
        mThread = new Thread(new Runnable() {
            @Override
            public void run() {
                play();
            }
        });
        mThread.start();

        mAudioTrack.setPositionNotificationPeriod(SAMPLE_RATE / 30); // 30 times per second
        mAudioTrack.setNotificationMarkerPosition(mNumSamples);
        mAudioTrack.play();
    }

    public void stopPlayback() {
        if (mThread == null)
            return;

        mShouldContinue = false;
        mThread = null;

        mAudioTrack.pause();  // pause() stops the playback immediately.
        mAudioTrack.stop();   // Unblock mAudioTrack.write() to avoid deadlocks.
        mAudioTrack.flush();  // just in case...
    }

    public void release() {
        mAudioTrack.release();
    }

    private void play() {
        Log.v(LOG_TAG, "Audio streaming started");

        short[] buffer = new short[mBufferSize];
        mSamples.rewind();
        int limit = mNumSamples;
        int totalWritten = 0;
        while (mSamples.position() < limit && mShouldContinue) {
            int numSamplesLeft = limit - mSamples.position();
            int samplesToWrite;
            if (numSamplesLeft >= buffer.length) {
                mSamples.get(buffer);
                samplesToWrite = buffer.length;
            } else {
                for(int i = numSamplesLeft; i < buffer.length; i++) {
                    buffer[i] = 0;
                }
                mSamples.get(buffer, 0, numSamplesLeft);
                samplesToWrite = numSamplesLeft;
            }
            totalWritten += samplesToWrite;
            mAudioTrack.write(buffer, 0, samplesToWrite);
        }

        Log.v(LOG_TAG, "Audio streaming finished. Samples written: " + totalWritten);
    }
}
