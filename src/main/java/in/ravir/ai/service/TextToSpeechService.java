package in.ravir.ai.service;

import io.github.whitemagic2014.tts.TTS;
import io.github.whitemagic2014.tts.TTSVoice;
import io.github.whitemagic2014.tts.bean.Voice;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class TextToSpeechService {
    private static boolean javafxInitialized = false;
    private final Voice frenchVoice;

    static {
        try {
            javafx.application.Platform.startup(() -> {});
            javafxInitialized = true;
        } catch (IllegalStateException e) {
            // Toolkit already initialized, ignore
            javafxInitialized = true;
        }
    }

    public TextToSpeechService() {
        // Find a French voice, default to 'fr-FR-DeniseNeural' if available
        List<Voice> voices = TTSVoice.provides();
        Optional<Voice> found = voices.stream()
                .filter(v -> v.getShortName().startsWith("fr-FR"))
                .findFirst();
        if (found.isPresent()) {
            frenchVoice = found.get();
        } else {
            throw new RuntimeException("No French voice found in Edge TTS voices.");
        }
    }

    public void speak(String text) {
        try {
            String fileName = new TTS(frenchVoice, text)
                    .findHeadHook()
                    .fileName("output_audio")
                    .overwrite(true)
                    .formatMp3()
                    .trans();
            playAudioWithJavaFX("./storage/" + fileName);
        } catch (Exception e) {
            log.error("TTS error: {}", e.getMessage());
        }
    }

    private void playAudioWithJavaFX(String filePath) {
        try {
            javafx.application.Platform.runLater(() -> {
                javafx.scene.media.MediaPlayer mediaPlayer = null;
                try {
                    javafx.scene.media.Media media = new javafx.scene.media.Media(
                            new java.io.File(filePath).toURI().toString());
                    mediaPlayer = new javafx.scene.media.MediaPlayer(media);
                    mediaPlayer.setOnEndOfMedia(mediaPlayer::dispose);
                    mediaPlayer.setOnError(mediaPlayer::dispose);
                    mediaPlayer.play();
                } catch (Exception e) {
                    log.error("Error playing audio with JavaFX: {}", e.getMessage());
                    if (mediaPlayer != null) {
                        mediaPlayer.dispose();
                    }
                }
            });
        } catch (Exception e) {
            log.error("Error scheduling audio playback with JavaFX: {}", e.getMessage());
        }
    }
} 