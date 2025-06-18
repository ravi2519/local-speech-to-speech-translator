package in.ravir.ai.repository;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class TranslationInMemory {

    public enum TranslationStatus {
        IN_PROGRESS,
        STOPPED,
        COMPLETED
    }

    private final Map<String, TranslationStatus> translations = new ConcurrentHashMap<>();

    /**
     * Register a new translation with IN_PROGRESS status
     *
     * @param translationId unique identifier for the translation
     * @return true if registered successfully, false if already exists
     */
    public boolean registerTranslation(String translationId) {
        if (translations.containsKey(translationId)) {
            return false;
        }
        translations.put(translationId, TranslationStatus.IN_PROGRESS);
        return true;
    }

    /**
     * Mark a translation as stopped
     *
     * @param translationId unique identifier for the translation
     * @return true if status was updated, false if translation not found
     */
    public boolean stopTranslation(String translationId) {
        if (!translations.containsKey(translationId)) {
            return false;
        }
        translations.put(translationId, TranslationStatus.STOPPED);
        return true;
    }

    /**
     * Mark a translation as completed
     *
     * @param translationId unique identifier for the translation
     * @return true if status was updated, false if translation not found
     */
    public boolean completeTranslation(String translationId) {
        if (!translations.containsKey(translationId)) {
            return false;
        }
        translations.put(translationId, TranslationStatus.COMPLETED);
        return true;
    }

    /**
     * Get the current status of a translation
     *
     * @param translationId unique identifier for the translation
     * @return the status or null if translation not found
     */
    public TranslationStatus getStatus(String translationId) {
        return translations.get(translationId);
    }

    /**
     * Check if translation is in progress
     *
     * @param translationId unique identifier for the translation
     * @return true if the translation is in progress
     */
    public boolean isInProgress(String translationId) {
        return TranslationStatus.IN_PROGRESS.equals(getStatus(translationId));
    }
}