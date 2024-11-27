import com.microsoft.cognitiveservices.speech.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.File
import java.util.*

class AzureTextToSpeechGateway(
    private val azureSpeechKey: String,
    private val azureSpeechRegion: String
) {

    fun synthesizeSpeech(text: String): ByteArray {
        val speechConfig = SpeechConfig.fromSubscription(azureSpeechKey, azureSpeechRegion)
        val logFile = File("logs/${UUID.randomUUID()}.txt")
        speechConfig.setProperty(PropertyId.Speech_LogFilename, logFile.absolutePath)
        speechConfig.speechSynthesisVoiceName = "en-CA-ClaraNeural"
        speechConfig.setSpeechSynthesisOutputFormat(SpeechSynthesisOutputFormat.Raw8Khz16BitMonoPcm)

        val speechSynthesizer = SpeechSynthesizer(speechConfig, null)

        val speechSynthesisResult = speechSynthesizer.SpeakText(text)

        if (speechSynthesisResult.reason == ResultReason.Canceled) {
            val cancellationDetails = SpeechSynthesisCancellationDetails.fromResult(speechSynthesisResult)
            throw RuntimeException(
                "Failed to synthesize text '$text' with voice 'en-CA-ClaraNeural': " +
                        "reason='${cancellationDetails.reason}' " +
                        "errorCode='${cancellationDetails.errorCode}' " +
                        "errorDetails='${cancellationDetails.errorDetails}'"
            )
        } else if (speechSynthesisResult.reason != ResultReason.SynthesizingAudioCompleted) {
            throw RuntimeException(
                "Unexpected synthesis result for text '$text' and voice 'en-CA-ClaraNeural': " +
                        "reason='${speechSynthesisResult.reason}'"
            )
        }

        return speechSynthesisResult.audioData
    }

    fun synthesizeSpeechStreaming(text: String): Flow<ByteArray> {
        val speechConfig = SpeechConfig.fromSubscription(azureSpeechKey, azureSpeechRegion)
        val logFile = File("logs/${UUID.randomUUID()}.txt")
        speechConfig.setProperty(PropertyId.Speech_LogFilename, logFile.absolutePath)
        speechConfig.speechSynthesisVoiceName = "en-CA-ClaraNeural"
        speechConfig.setSpeechSynthesisOutputFormat(SpeechSynthesisOutputFormat.Raw8Khz16BitMonoPcm)

        val speechSynthesizer = SpeechSynthesizer(speechConfig, null)

        val speechSynthesisResult = speechSynthesizer.StartSpeakingText(text)

        if (speechSynthesisResult.reason == ResultReason.Canceled) {
            val cancellationDetails = SpeechSynthesisCancellationDetails.fromResult(speechSynthesisResult)
            throw RuntimeException(
                "Failed to synthesize text '$text' with voice 'en-CA-ClaraNeural': " +
                        "reason='${cancellationDetails.reason}' " +
                        "errorCode='${cancellationDetails.errorCode}' " +
                        "errorDetails='${cancellationDetails.errorDetails}'"
            )
        } else if (speechSynthesisResult.reason != ResultReason.SynthesizingAudioStarted) {
            throw RuntimeException(
                "Unexpected synthesis result for text '$text' and voice 'en-CA-ClaraNeural': " +
                        "reason='${speechSynthesisResult.reason}'"
            )
        } else {
            val audioDataStream = AudioDataStream.fromResult(speechSynthesisResult)
            return flow {
                val audioBytes = ByteArray(16000)
                var readBytes = audioDataStream.readData(audioBytes)
                require(readBytes > 0) { "Received 0 byte response. Log file ${logFile.absolutePath}" }
                while (readBytes > 0) {
                    emit(audioBytes.copyOf(readBytes.toInt()))
                    readBytes = audioDataStream.readData(audioBytes)
                }
            }
        }
    }

}
