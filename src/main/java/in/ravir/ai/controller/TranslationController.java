package in.ravir.ai.controller;

import in.ravir.ai.service.TranscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/translations")
@RequiredArgsConstructor
public class TranslationController {

    private final TranscriptionService translationService;

    @PostMapping("/start")
    public ResponseEntity<String> startTranslation(@RequestParam String sourceLanguage,
                                                   @RequestParam String targetLanguage) {
        String translationId = translationService.startTranslation(sourceLanguage, targetLanguage);
        return ResponseEntity.ok("Translation started with ID: " + translationId);
    }

    @PostMapping("/stop/{translationId}")
    public ResponseEntity<String> stopTranslation(@PathVariable String translationId) {
        boolean stopped = translationService.stopTranslation(translationId);
        if (stopped) {
            return ResponseEntity.ok("Translation with ID: " + translationId + " has been stopped");
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}