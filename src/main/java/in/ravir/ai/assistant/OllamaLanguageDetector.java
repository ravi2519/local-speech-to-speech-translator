package in.ravir.ai.assistant;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.spring.AiService;
import dev.langchain4j.service.spring.AiServiceWiringMode;


@AiService(wiringMode = AiServiceWiringMode.EXPLICIT, chatModel = "ollamaChatModel")
public interface OllamaLanguageDetector {
    @SystemMessage("You are a language detector. Return true if the input text is entirely in French, otherwise return false. Output only 'true' or 'false' as plain text, with no explanation, formatting, or additional text. Never apologize or comment.")
    boolean isTextFrench(String message);
}
