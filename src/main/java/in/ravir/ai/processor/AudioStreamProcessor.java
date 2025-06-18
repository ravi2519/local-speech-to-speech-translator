package in.ravir.ai.processor;

import io.github.givimad.whisperjni.WhisperContext;
import io.github.givimad.whisperjni.WhisperFullParams;
import io.github.givimad.whisperjni.WhisperJNI;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Path;

@Slf4j
@Component
public class AudioStreamProcessor {
    private volatile boolean running = false;
    private final ByteArrayOutputStream audioBuffer = new ByteArrayOutputStream();
    private long lastProcessedPosition = 0;
    private final WhisperContext whisperContext;
    private TranscriptionListener listener;

    public void setTranscriptionListener(TranscriptionListener listener) {
        this.listener = listener;
    }

    private final ByteArrayOutputStream processingBuffer = new ByteArrayOutputStream();
    private final StringBuilder sentenceBuffer = new StringBuilder();

    AudioFormat getAudioFormat() {
        float sampleRate = 16000;
        int sampleSizeInBits = 16;
        int channels = 1;
        boolean signed = true;
        boolean bigEndian = false;
        return new AudioFormat(sampleRate, sampleSizeInBits, channels, signed, bigEndian);
    }

    public AudioStreamProcessor() throws IOException {
        WhisperJNI.loadLibrary();
        WhisperJNI.setLibraryLogger(null);
        var whisper = new WhisperJNI();
        whisperContext = whisper.init(Path.of(System.getProperty("user.home"), "ggml-large-v3.bin"));
    }

    @SneakyThrows
    public void startContinuousRecording() {
        try {
            AudioFormat format = getAudioFormat();
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);

            TargetDataLine line = (TargetDataLine) AudioSystem.getLine(info);
            line.open(format);
            line.start();
            running = true;

            // Start capture in a virtual thread
            Thread.ofVirtual().name("continuous-recorder").start(() -> {
                try {
                    byte[] buffer = new byte[line.getBufferSize()];
                    while (running) {
                        int count = line.read(buffer, 0, buffer.length);
                        if (count > 0) {
                            synchronized (audioBuffer) {
                                audioBuffer.write(buffer, 0, count);
                            }
                        }
                    }
                } catch (Exception e) {
                    log.error("Error in continuous recording", e);
                } finally {
                    line.stop();
                    line.close();
                }
            });

        } catch (LineUnavailableException ex) {
            log.error("Could not start continuous recording", ex);
        }
    }

    public void startContinuousProcessing() {
        if (running) {
            return;
        }

        startContinuousRecording();

        Thread.ofVirtual().name("continuous-processor").start(() -> {
            var whisper = new WhisperJNI();
            var params = new WhisperFullParams();

            try {
                while (running) {
                    float[] newSamples = getNewSamples();

                    if (newSamples.length > 0) {
                        byte[] sampleBytes = floatSamplesToBytes(newSamples);
                        synchronized (processingBuffer) {
                            processingBuffer.write(sampleBytes);

                            if (processingBuffer.size() >= 48000) {
                                float[] samples = getBytesAsFloatSamples(processingBuffer.toByteArray());
                                processingBuffer.reset();

                                if (whisper.full(whisperContext, params, samples, samples.length) == 0) {
                                    int segmentCount = whisper.fullNSegments(whisperContext);

                                    for (int j = 0; j < segmentCount; j++) {
                                        String text = whisper.fullGetSegmentText(whisperContext, j);
                                        if (!text.trim().isEmpty()) {
                                            sentenceBuffer.append(text);

                                            String bufferStr = sentenceBuffer.toString();
                                            int sentenceEnd = findSentenceEnd(bufferStr);

                                            while (sentenceEnd != -1) {
                                                String sentence = bufferStr.substring(0, sentenceEnd + 1).trim();
                                                if (!sentence.isEmpty()) {
                                                    log.trace(sentence);
                                                    listener.onTranscription(sentence);
                                                }
                                                bufferStr = bufferStr.substring(sentenceEnd + 1).trim();
                                                sentenceEnd = findSentenceEnd(bufferStr);
                                            }

                                            // Update the buffer with any remaining partial sentence
                                            sentenceBuffer.setLength(0);
                                            sentenceBuffer.append(bufferStr);
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Thread.sleep(100);
                }
            } catch (Exception e) {
                log.error("Error in continuous processing", e);
                Thread.currentThread().interrupt();
            }
        });
    }

    public float[] getNewSamples() {
        synchronized (audioBuffer) {
            if (audioBuffer.size() <= lastProcessedPosition) {
                return new float[0]; // No new data
            }

            try {
                byte[] allData = audioBuffer.toByteArray();
                int newDataSize = allData.length - (int)lastProcessedPosition;

                ByteBuffer newDataBuffer = ByteBuffer.allocate(newDataSize);
                newDataBuffer.order(ByteOrder.LITTLE_ENDIAN);

                System.arraycopy(
                        allData,
                        (int) lastProcessedPosition,
                        newDataBuffer.array(),
                        0,
                        newDataSize
                );

                audioBuffer.flush();

                lastProcessedPosition = allData.length;

                ByteArrayInputStream bais = new ByteArrayInputStream(newDataBuffer.array());

                return readJFKFileSamples(new AudioInputStream(
                        bais, getAudioFormat(), newDataBuffer.array().length / getAudioFormat().getFrameSize()));
            } catch (Exception e) {
                log.error("Error processing audio samples", e);
                return new float[0];
            }
        }
    }

    public void stop() {
        running = false;
    }

    private float[] readJFKFileSamples(AudioInputStream audioInputStream) throws IOException {
        ByteBuffer captureBuffer = ByteBuffer.allocate(audioInputStream.available());
        captureBuffer.order(ByteOrder.LITTLE_ENDIAN);
        int read = audioInputStream.read(captureBuffer.array());
        if (read == -1) {
            throw new IOException("Empty file");
        }
        var shortBuffer = captureBuffer.asShortBuffer();
        float[] samples = new float[captureBuffer.capacity() / 2];
        var i = 0;
        while (shortBuffer.hasRemaining()) {
            samples[i++] = Float.max(-1f, Float.min(((float) shortBuffer.get()) / (float) Short.MAX_VALUE, 1f));
        }
        return samples;
    }

    private byte[] floatSamplesToBytes(float[] samples) {
        ByteBuffer buffer = ByteBuffer.allocate(samples.length * 2);
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        for (float sample : samples) {
            short value = (short) (sample * Short.MAX_VALUE);
            buffer.putShort(value);
        }

        return buffer.array();
    }

    private float[] getBytesAsFloatSamples(byte[] bytes) {
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        float[] samples = new float[bytes.length / 2];
        int i = 0;

        while (buffer.remaining() >= 2) {
            short value = buffer.getShort();
            samples[i++] = Float.max(-1f, Float.min(((float) value) / (float) Short.MAX_VALUE, 1f));
        }

        return samples;
    }

    private int findSentenceEnd(String text) {
        // Looks for '.', '!' or '?' followed by a space or end of string
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if ((c == '.' || c == '!' || c == '?') &&
                (i == text.length() - 1 || Character.isWhitespace(text.charAt(i + 1)))) {
                return i;
            }
        }
        return -1;
    }
}