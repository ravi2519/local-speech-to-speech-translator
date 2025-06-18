package in.ravir.ai.service;

import io.github.whitemagic2014.tts.TTS;
import io.github.whitemagic2014.tts.TTSVoice;
import io.github.whitemagic2014.tts.bean.Voice;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineEvent;
import java.io.File;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
class TranslationServiceTest {

    @Test
    void testTTS() {
        // Select a voice from the available voices (e.g., "en-US-AriaNeural")
        Voice voice = TTSVoice.provides()
                .stream()
                .filter(v -> v.getShortName().equals("en-US-AriaNeural"))
                .collect(Collectors.toList())
                .get(0);

        // Define the text content to convert into speech
        String content = "Hello, this is Microsoft Edge Text-to-Speech in Java!";

        // Set up the TTS instance with options
        String fileName = new TTS(voice, content)
                .findHeadHook()
                .fileName("output_audio") // Specify output file name (optional)
                .overwrite(true)         // Overwrite existing file if it exists
                .formatMp3()             // Output format (default is MP3)
                .trans();                // Generate the audio file

        System.out.println("Audio file generated: " + fileName);

        playAudioWithJavaFX("./storage/" + fileName);
    }

    private void playAudioWithJavaFX(String filePath) {
        try {
            // Initialize JavaFX toolkit
            javafx.application.Platform.startup(() -> {});

            javafx.scene.media.Media media = new javafx.scene.media.Media(
                    new java.io.File(filePath).toURI().toString());
            javafx.scene.media.MediaPlayer mediaPlayer = new javafx.scene.media.MediaPlayer(media);

            // Add listener to handle when playback is done
            mediaPlayer.setOnEndOfMedia(() -> {
                mediaPlayer.dispose();
            });

            mediaPlayer.play();

            // Keep JVM running until playback completes
            Thread.sleep((long)(mediaPlayer.getTotalDuration().toMillis() + 15000));
        } catch (Exception e) {
            log.error("Error playing audio with JavaFX: {}", e.getMessage());
        }
    }

private void playAudio(String filePath) {
    try {
        File audioFile = new File(filePath);
        AudioInputStream audioStream = AudioSystem.getAudioInputStream(audioFile);
        AudioFormat format = audioStream.getFormat();
        DataLine.Info info = new DataLine.Info(Clip.class, format);

        Clip audioClip = (Clip) AudioSystem.getLine(info);
        audioClip.open(audioStream);
        audioClip.start();

        // Add listener to detect when playback is finished
        audioClip.addLineListener(event -> {
            if (event.getType() == LineEvent.Type.STOP) {
                audioClip.close();
            }
        });
    } catch (Exception e) {
        log.error("Error playing audio: {}", e.getMessage());
    }
}
  
}