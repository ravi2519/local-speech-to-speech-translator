package in.ravir.ai.service;

import dev.langchain4j.model.chat.ChatLanguageModel;
import in.ravir.ai.processor.AudioStreamProcessor;
import in.ravir.ai.repository.TranslationInMemory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
@RequiredArgsConstructor
public class TranscriptionService {

    private final TranslationInMemory translationInMemory;
    private final AudioStreamProcessor processor;
    private final TranslationService translationService;

    public String startTranslation(String sourceLanguage, String targetLanguage) {
        log.info("Starting translation from {} to {}", sourceLanguage, targetLanguage);

        String translationId = UUID.randomUUID().toString();

        translationInMemory.registerTranslation(translationId);
        processor.setTranscriptionListener(translationService);
        CompletableFuture.runAsync(() -> {
            try {
                while (translationInMemory.isInProgress(translationId)) {
                    processor.startContinuousProcessing();
                    Thread.sleep(1000); // Simulating ongoing work
                }

                if (translationInMemory.isInProgress(translationId)) {
                    translationInMemory.completeTranslation(translationId);
                    log.info("Translation {} completed", translationId);
                } else {
                    log.info("Translation {} was stopped", translationId);
                }
            } catch (Exception e) {
                log.error("Error during translation {}: {}", translationId, e.getMessage());
            }
        });

        return translationId;
    }

    public boolean stopTranslation(String translationId) {
        log.info("Stopping translation {}", translationId);
        processor.stop();
        return translationInMemory.stopTranslation(translationId);
    }
}