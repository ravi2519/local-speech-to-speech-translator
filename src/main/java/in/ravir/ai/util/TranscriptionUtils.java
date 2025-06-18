package in.ravir.ai.util;

import java.util.List;

public class TranscriptionUtils {
    private TranscriptionUtils() {}

    public static boolean shouldIgnoreTranscription(String transcription) {
        if (transcription == null || transcription.trim().isEmpty()) {
            return true;
        }

        List<String> ignoreSentences = List.of(".", "- Okay.");
        for (String phrase : ignoreSentences) {
            if (transcription.trim().equals(phrase)) {
                return true;
            }
        }

        String lower = transcription.toLowerCase();
        List<String> ignorePhrases = List.of("amara", "thank you");
        for (String phrase : ignorePhrases) {
            if (lower.contains(phrase)) {
                return true;
            }
        }
        return false;
    }
} 