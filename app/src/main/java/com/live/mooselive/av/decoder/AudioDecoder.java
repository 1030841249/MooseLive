package com.live.mooselive.av.decoder;

import android.media.AudioFormat;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;

import java.nio.ByteBuffer;

import static android.media.AudioManager.STREAM_MUSIC;

public class AudioDecoder extends BaseDecoder {

    AudioTrack audioTrack;

    public AudioDecoder(MediaExtractor mExtractor) {
        super(mExtractor, null);
        MediaFormat audioFormat = getFormat();
        int sampleRate = audioFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE);
//        int sampleRate = 44100;
        int channelCount = audioFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
        int pcmEncoding = 0;
        int channel = -1;
        if (channelCount == 1) {
            channel = AudioFormat.CHANNEL_OUT_MONO;
        } else {
            channel = AudioFormat.CHANNEL_OUT_STEREO;
        }
        if (audioFormat.containsKey(MediaFormat.KEY_PCM_ENCODING)) {
            pcmEncoding = audioFormat.getInteger(MediaFormat.KEY_PCM_ENCODING);
        } else {
            pcmEncoding = AudioFormat.ENCODING_PCM_16BIT;
        }
        int bufferSize = AudioTrack.getMinBufferSize(sampleRate, channel, pcmEncoding);

        audioTrack = new AudioTrack(STREAM_MUSIC, sampleRate, channel, pcmEncoding, bufferSize, AudioTrack.MODE_STREAM);
        audioTrack.play();
    }

    @Override
    protected String getFormatType() {
        return "audio/";
    }

    @Override
    protected void renderData(MediaCodec.BufferInfo bufferInfo, ByteBuffer byteBuffer) {
        byte[] buffer = new byte[bufferInfo.size];
        byteBuffer.get(buffer, bufferInfo.offset, bufferInfo.flags);
        audioTrack.write(buffer, 0, bufferInfo.size - bufferInfo.offset);
    }
}
