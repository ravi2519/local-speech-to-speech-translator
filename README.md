# Local Speech To Speech Translator

Local Speech To Speech Translator is a language translation and transcription service built with Spring Boot, integrating advanced AI models for speech-to-text transcription, text to text translation and text-to-speech capabilities. **All processing is performed entirely on your local machine using local LLM models—no external API calls or cloud services are used. Your data never leaves your device, ensuring privacy and offline operation.**

## Demo

<details>
<summary>Click to view embedded video (if supported by your viewer):</summary>

https://github.com/user-attachments/assets/fd6fbcba-344b-4858-8dc8-0f810f60597a

</details>

## Features
- **Speech-to-Text**: Transcribe audio using [Whisper JNI](https://github.com/openai/whisper), running locally.
- **Text-to-Speech**: Generate speech from text using tts-edge-java and JavaFX, all on-device.
- **Language Translation**: Translate text using Ollama LLM via LangChain4J, with models running locally.
- **REST API**: Expose translation and transcription endpoints for local use.

## Project Structure
```
local-speech-to-speech-translator/
├── src/
│   ├── main/
│   │   ├── java/in/ravir/ai/
│   │   │   ├── controller/      # API endpoints (e.g., TranslationController)
│   │   │   ├── service/         # Business logic (TranslationService, TranscriptionService)
│   │   │   ├── processor/       # Audio/AI processing (AudioStreamProcessor, etc.)
│   │   │   ├── assistant/       # AI integration (OllamaTranslationAssistant)
│   │   │   ├── repository/      # In-memory storage (TranslationInMemory)
│   │   │   └── LocalS2STApplication.java
│   │   └── resources/
│   │       └── application.yml  # Configuration
│   └── test/
│       └── java/in/ravir/ai/service/TranslationServiceTest.java
├── pom.xml                      # Maven build file
├── LICENSE
└── README.md
```

## Requirements
- Java 21+
- Maven 3.8+
- Ollama running locally (for LLM translation)

## Setup
1. **Clone the repository:**
   ```sh
   git clone <repo-url>
   cd local-speech-to-speech-translator
   ```
2. Download whisper model, preferably "ggml-large-v3.bin" from [Huggingface](https://huggingface.co/ggerganov/whisper.cpp/tree/main) and place in `user.home`

3. **Start Ollama:**
   - Download and run Ollama from [https://ollama.com/](https://ollama.com/)
   - Pull the required model:
     ```sh
     ollama pull mrjacktung/mradermacher-llamax3-8b-alpaca-gguf
     ollama serve
     ```
4. **Build and run the application:**
   ```sh
   mvn clean install
   mvn spring-boot:run
   ```

## Configuration
Edit `src/main/resources/application.yml` as needed:
```yaml
spring:
  application:
    name: Local Speech To Speech Translator
  servlet:
    multipart:
      max-file-size: 10MB
      max-request-size: 10MB

langchain4j:
  ollama:
    chat-model:
      base-url: http://localhost:11434
      model-name: mrjacktung/mradermacher-llamax3-8b-alpaca-gguf
```

## External Dependencies
All dependencies are managed via Maven (`pom.xml`). Key dependencies:
- **Spring Boot** (web, test)
- **LangChain4J** (`langchain4j-ollama-spring-boot-starter`, `langchain4j-spring-boot-starter`)
- **Whisper JNI** (`io.github.givimad:whisper-jni`) — Speech-to-text
- **TTS Edge Java** (`io.github.whitemagic2014:tts-edge-java`) — Text-to-speech
- **JavaFX Media** (`org.openjfx:javafx-media`) — Audio playback
- **Lombok** (`org.projectlombok:lombok`) — Code annotations

See `pom.xml` for full details and versions.

## Usage
- Access the REST API endpoints (see `TranslationController.java` for details)
- Example test for TTS and audio playback: `TranslationServiceTest.java`
