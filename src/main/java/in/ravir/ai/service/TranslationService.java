package in.ravir.ai.service;

import in.ravir.ai.assistant.OllamaTranslationAssistant;
import in.ravir.ai.processor.TranscriptionListener;
import in.ravir.ai.util.TranscriptionUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
@Service
@RequiredArgsConstructor
@Slf4j
public class TranslationService implements TranscriptionListener {

    private final OllamaTranslationAssistant assistant;
    private final TextToSpeechService textToSpeechService;

    @Override
    public void onTranscription(String transcription) {
        if (TranscriptionUtils.shouldIgnoreTranscription(transcription)) {
            return;
        }

        log.info("Received the transcription as ===> {}", transcription);
        textToSpeechService.speak(assistant.translateToFrench(transcription));
    }
}
