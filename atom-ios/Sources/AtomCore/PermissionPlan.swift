public enum AtomPermissionStep: String, CaseIterable, Equatable, Sendable {
  case notifications
  case microphone
  case speechRecognition
  case prominentAlarms
}

public struct IOSPermissionSnapshot: Equatable, Sendable {
  public let notificationsGranted: Bool
  public let microphoneGranted: Bool
  public let speechRecognitionGranted: Bool
  public let prominentAlarmsSupported: Bool
  public let prominentAlarmsGranted: Bool

  public init(
    notificationsGranted: Bool,
    microphoneGranted: Bool,
    speechRecognitionGranted: Bool,
    prominentAlarmsSupported: Bool,
    prominentAlarmsGranted: Bool
  ) {
    self.notificationsGranted = notificationsGranted
    self.microphoneGranted = microphoneGranted
    self.speechRecognitionGranted = speechRecognitionGranted
    self.prominentAlarmsSupported = prominentAlarmsSupported
    self.prominentAlarmsGranted = prominentAlarmsGranted
  }
}

public func iosPermissionPlan(for snapshot: IOSPermissionSnapshot) -> [AtomPermissionStep] {
  var plan: [AtomPermissionStep] = []
  if !snapshot.notificationsGranted { plan.append(.notifications) }
  if !snapshot.microphoneGranted { plan.append(.microphone) }
  if !snapshot.speechRecognitionGranted { plan.append(.speechRecognition) }
  if snapshot.prominentAlarmsSupported, !snapshot.prominentAlarmsGranted {
    plan.append(.prominentAlarms)
  }
  return plan
}
