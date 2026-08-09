import AVFoundation
import Combine
import Speech

@MainActor
final class SpeechCaptureService: ObservableObject {
  @Published private(set) var isListening = false
  @Published private(set) var transcript = ""
  @Published private(set) var errorMessage: String?

  private let audioEngine = AVAudioEngine()
  private let recognizer = SFSpeechRecognizer(locale: Locale.current)
  private var request: SFSpeechAudioBufferRecognitionRequest?
  private var task: SFSpeechRecognitionTask?

  func toggle() {
    if isListening {
      stop()
    } else {
      start()
    }
  }

  func start() {
    guard SFSpeechRecognizer.authorizationStatus() == .authorized,
      AVAudioSession.sharedInstance().recordPermission == .granted
    else {
      errorMessage = "Allow microphone and speech recognition in Settings first."
      return
    }
    guard let recognizer, recognizer.isAvailable else {
      errorMessage = "Speech recognition is currently unavailable. You can still type."
      return
    }

    stop()
    errorMessage = nil
    transcript = ""

    let request = SFSpeechAudioBufferRecognitionRequest()
    request.shouldReportPartialResults = true
    request.taskHint = .dictation
    request.contextualStrings = [
      "Atom", "remind me", "tomorrow", "snooze", "remind me again", "every weekday",
    ]
    if recognizer.supportsOnDeviceRecognition {
      request.requiresOnDeviceRecognition = true
    }
    self.request = request

    let session = AVAudioSession.sharedInstance()
    do {
      try session.setCategory(.record, mode: .measurement, options: [.duckOthers])
      try session.setActive(true, options: .notifyOthersOnDeactivation)
      let inputNode = audioEngine.inputNode
      let format = inputNode.outputFormat(forBus: 0)
      inputNode.installTap(onBus: 0, bufferSize: 1_024, format: format) { buffer, _ in
        request.append(buffer)
      }
      audioEngine.prepare()
      try audioEngine.start()
      isListening = true
    } catch {
      errorMessage = "Atom could not start the microphone."
      stop()
      return
    }

    task = recognizer.recognitionTask(with: request) { [weak self] result, error in
      Task { @MainActor in
        guard let self else { return }
        if let result {
          transcript = result.bestTranscription.formattedString
          if result.isFinal { stop() }
        }
        if error != nil { stop() }
      }
    }
  }

  func stop() {
    if audioEngine.isRunning {
      audioEngine.stop()
      audioEngine.inputNode.removeTap(onBus: 0)
    }
    request?.endAudio()
    task?.cancel()
    request = nil
    task = nil
    isListening = false
    try? AVAudioSession.sharedInstance().setActive(false, options: .notifyOthersOnDeactivation)
  }
}
