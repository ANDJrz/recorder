package com.yioks.recorder.MediaRecord.RenderHelper;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaFormat;
import android.media.MediaRecorder;

import com.yioks.recorder.MediaRecord.Bean.MediaFrameData;
import com.yioks.recorder.MediaRecord.Encode.AudioEncoder;

/**
 * Created by ${UserWrapper} on 2017/8/21 0021.
 * 录制音频控制
 */

public class RecordAudioManager {
    private AudioRecord audioRecord;
    private Context context;
    private CallBackEvent event;
    private int track;

    // 音频源：音频输入-麦克风
    private final static int AUDIO_INPUT = MediaRecorder.AudioSource.MIC;

    // 音频通道 单声道
    private final static int AUDIO_CHANNEL = AudioFormat.CHANNEL_IN_MONO;
    // 音频格式：PCM编码
    private final static int AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    // 缓冲区大小：缓冲区字节大小
    private int bufferSizeInBytes = 0;


    private volatile boolean isRecord = false;
    private AudioEncoder audioEncoder;




    public RecordAudioManager(Context context) {
        this.context = context;
    }

    public void startRecord(int AUDIO_SAMPLE_RATE, int AUDIO_RATE, int time) {
        if (isRecord)
            return;
        isRecord = true;
        try {
            bufferSizeInBytes = AudioRecord.getMinBufferSize(AUDIO_SAMPLE_RATE,
                    AUDIO_CHANNEL, AUDIO_ENCODING);

//             bufferSizeInBytes = SAMPLES_PER_FRAME * FRAMES_PER_BUFFER;
//            if (bufferSizeInBytes < minBufferSizeInBytes)
//                bufferSizeInBytes = ((minBufferSizeInBytes / SAMPLES_PER_FRAME) + 1) * SAMPLES_PER_FRAME * 2;

            audioEncoder = new AudioEncoder(AUDIO_SAMPLE_RATE, AUDIO_RATE, bufferSizeInBytes, new AudioEncoder.AudioRecordCallBack() {
                @Override
                public void finish() {
                    if (audioEncoder != null)
                        event.recordAudioFinish();
                }

                @Override
                public void failure(String msg) {
                    cancelRecord();
                    event.recordAudioError(msg);
                }

                @Override
                public int formatConfirm(MediaFormat mediaFormat) {
                    track = event.formatConfirm(mediaFormat);
                    return track;
                }

                @Override
                public void frameAvailable(MediaFrameData frameData) {
                    event.frameAvailable(frameData);
                }

            });
            audioRecord = new AudioRecord(AUDIO_INPUT, AUDIO_SAMPLE_RATE, AUDIO_CHANNEL, AUDIO_ENCODING, bufferSizeInBytes);
            audioEncoder.setAudioRecord(audioRecord);

            audioRecord.startRecording();
            audioEncoder.startRecordAudioLoop();
        } catch (Exception e) {
            e.printStackTrace();
            if (event != null)
                event.recordAudioError("启动录制音频失败");
            return;
        }

        if (event != null)
            event.startRecordAudio();
    }

//    private void getRecordAudioData(AudioRecord recorder, AudioEncoder audioEncoder) {
//
//    }


    public void stopRecord() {
        if (isRecord && audioRecord != null) {
            try {
                isRecord = false;
                audioRecord.stop();
                audioRecord.release();
                if (audioEncoder != null) {
                    audioEncoder.shutdown();
                }
            } catch (IllegalStateException e) {
                e.printStackTrace();
            }
            audioRecord = null;
        }
    }


    public void cancelRecord() {
        if (isRecord && audioRecord != null) {
            try {
                audioRecord.stop();
                audioRecord.release();
                if (audioEncoder != null) {
                    audioEncoder.shutdown();
                    audioEncoder.release(true);
                    audioEncoder = null;
                }
            } catch (IllegalStateException e) {
                e.printStackTrace();
            }
            audioRecord = null;
            isRecord = false;
        }
    }

    public void releaseRecord(boolean releaseData) {
        if (audioEncoder != null) {
            audioEncoder.release(releaseData);
            if (releaseData)
                audioEncoder = null;
        }
    }


    public AudioEncoder getAudioEncoder() {
        return audioEncoder;
    }


    public interface CallBackEvent {
        void startRecordAudio();

        void recordAudioError(String errorMsg);

        void recordAudioFinish();

        int formatConfirm(MediaFormat mediaFormat);

        void frameAvailable(MediaFrameData frameData);
    }

    public CallBackEvent getEvent() {
        return event;
    }

    public void setEvent(CallBackEvent event) {
        this.event = event;
    }

    public int getTrack() {
        return track;
    }
}
