#if canImport(Speech)
import Foundation
import Speech
import AVFoundation
import os

/// Service for voice-to-text input using SFSpeechRecognizer.
/// Designed for hands-free gym use: tap to start, tap to stop.
@MainActor
@Observable
public final class VoiceInputService {

    private static let logger = Logger(subsystem: "com.gymbro", category: "VoiceInput")

    // MARK: - Published State

    public var isRecording: Bool = false
    public var transcribedText: String = ""
    public var isAuthorized: Bool = false
    public var errorMessage: String?

    // MARK: - Private

    private let speechRecognizer = SFSpeechRecognizer(locale: Locale.current)
    private var recognitionRequest: SFSpeechAudioBufferRecognitionRequest?
    private var recognitionTask: SFSpeechRecognitionTask?
    private let audioEngine = AVAudioEngine()

    public init() {}

    // MARK: - Authorization

    public func requestAuthorization() {
        SFSpeechRecognizer.requestAuthorization { [weak self] status in
            Task { @MainActor in
                switch status {
                case .authorized:
                    self?.isAuthorized = true
                case .denied, .restricted, .notDetermined:
                    self?.isAuthorized = false
                    self?.errorMessage = "Speech recognition not available. Enable in Settings."
                @unknown default:
                    self?.isAuthorized = false
                }
            }
        }
    }

    // MARK: - Recording

    public func startRecording() throws {
        guard let recognizer = speechRecognizer, recognizer.isAvailable else {
            errorMessage = "Speech recognition unavailable."
            return
        }

        recognitionTask?.cancel()
        recognitionTask = nil

        let audioSession = AVAudioSession.sharedInstance()
        try audioSession.setCategory(.record, mode: .measurement, options: .duckOthers)
        try audioSession.setActive(true, options: .notifyOthersOnDeactivation)

        recognitionRequest = SFSpeechAudioBufferRecognitionRequest()
        guard let request = recognitionRequest else {
            errorMessage = "Failed to create recognition request."
            return
        }
        request.shouldReportPartialResults = true

        let inputNode = audioEngine.inputNode
        recognitionTask = recognizer.recognitionTask(with: request) { [weak self] result, error in
            Task { @MainActor in
                if let result {
                    self?.transcribedText = result.bestTranscription.formattedString
                }
                if error != nil || (result?.isFinal ?? false) {
                    self?.stopRecordingInternal()
                }
            }
        }

        let recordingFormat = inputNode.outputFormat(forBus: 0)
        inputNode.installTap(onBus: 0, bufferSize: 1024, format: recordingFormat) { buffer, _ in
            request.append(buffer)
        }

        audioEngine.prepare()
        try audioEngine.start()
        isRecording = true
        transcribedText = ""
        errorMessage = nil

        Self.logger.info("Voice recording started")
    }

    public func stopRecording() -> String {
        let result = transcribedText
        stopRecordingInternal()
        Self.logger.info("Voice recording stopped, transcribed: \(result.prefix(50))")
        return result
    }

    private func stopRecordingInternal() {
        audioEngine.stop()
        audioEngine.inputNode.removeTap(onBus: 0)
        recognitionRequest?.endAudio()
        recognitionRequest = nil
        recognitionTask?.cancel()
        recognitionTask = nil
        isRecording = false
    }
}
#endif
