import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.runBlocking

private val log = KotlinLogging.logger {}

fun main() {
//    testNonStreamingTts()
    testStreamingTts()
}

fun testNonStreamingTts() {
    val text =
        "Hello, my name is Alex, a virtual agent with The ABC Group. We are conducting a survey with Canadians about important issues and want to ask you a few questions about your views. The ABC Group can be reached directly via email at hello@abcgroup.com and via telephone at 1-${(100..999).random()}-${(100..999).random()}-${(1000..9999).random()}. Would you have a few moments to participate in our study?"
    val azureSpeechKey =
        System.getenv("AZURE_SPEECH_KEY") ?: throw RuntimeException("Missing environment variable AZURE_SPEECH_KEY")

    val threads = (1..30).map { attempt ->
        Thread.ofVirtual().unstarted {
            log.info { "Starting attempt $attempt" }
            val audioBytes = AzureTextToSpeechGateway(
                azureSpeechKey = azureSpeechKey,
                azureSpeechRegion = "canadacentral"
            ).synthesizeSpeech(text)
            log.info { "Synthesized ${audioBytes.size} for attempt $attempt" }
            log.info { "Finished attempt $attempt" }
        }.also {
            it.setUncaughtExceptionHandler { thread, throwable ->
                log.error(throwable) { "ERROR for attempt $attempt" }
            }
        }
    }

    threads.forEach { it.start() }
    threads.forEach { it.join() }
}

fun testStreamingTts() {
    val text =
        "Hello, my name is Alex, a virtual agent with The ABC Group. We are conducting a survey with Canadians about important issues and want to ask you a few questions about your views. The ABC Group can be reached directly via email at hello@abcgroup.com and via telephone at 1-${(100..999).random()}-${(100..999).random()}-${(1000..9999).random()}. Would you have a few moments to participate in our study?"
    val azureSpeechKey =
        System.getenv("AZURE_SPEECH_KEY") ?: throw RuntimeException("Missing environment variable AZURE_SPEECH_KEY")

    val threads = (1..30).map { attempt ->
        Thread.ofVirtual().unstarted {
            log.info { "Starting attempt $attempt" }
            val audioBytesFlow = AzureTextToSpeechGateway(
                azureSpeechKey = azureSpeechKey,
                azureSpeechRegion = "canadacentral"
            ).synthesizeSpeechStreaming(text)
            var audioBytes = 0
            runBlocking {
                audioBytesFlow.collect {
                    audioBytes += it.size
                }
            }
            log.info { "Synthesized $audioBytes for attempt $attempt" }
            log.info { "Finished attempt $attempt" }
        }.also {
            it.setUncaughtExceptionHandler { thread, throwable ->
                log.error(throwable) { "ERROR for attempt $attempt" }
            }
        }
    }

    threads.forEach { it.start() }
    threads.forEach { it.join() }
}
