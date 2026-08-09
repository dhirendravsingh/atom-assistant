import AVFoundation
import AtomCore
import Combine
import Foundation
import Speech
import UIKit
import UserNotifications

@MainActor
final class PermissionCoordinator: ObservableObject {
  @Published private(set) var notificationGranted = false
  @Published private(set) var microphoneGranted = false
  @Published private(set) var speechGranted = false
  @Published private(set) var requesting = false

  var plan: [AtomPermissionStep] {
    iosPermissionPlan(
      for: IOSPermissionSnapshot(
        notificationsGranted: notificationGranted,
        microphoneGranted: microphoneGranted,
        speechRecognitionGranted: speechGranted,
        prominentAlarmsSupported: false,
        prominentAlarmsGranted: false
      )
    )
  }

  func refresh() async {
    let settings = await UNUserNotificationCenter.current().notificationSettings()
    notificationGranted =
      settings.authorizationStatus == .authorized
      || settings.authorizationStatus == .provisional
    microphoneGranted = AVAudioSession.sharedInstance().recordPermission == .granted
    speechGranted = SFSpeechRecognizer.authorizationStatus() == .authorized
  }

  func requestInitialPermissions() async {
    requesting = true
    defer { requesting = false }

    if !notificationGranted {
      notificationGranted =
        (try? await UNUserNotificationCenter.current().requestAuthorization(
          options: [.alert, .sound, .badge, .providesAppNotificationSettings]
        )) ?? false
    }
    if !microphoneGranted {
      microphoneGranted = await withCheckedContinuation { continuation in
        AVAudioSession.sharedInstance().requestRecordPermission { granted in
          continuation.resume(returning: granted)
        }
      }
    }
    if !speechGranted {
      speechGranted = await withCheckedContinuation { continuation in
        SFSpeechRecognizer.requestAuthorization { status in
          continuation.resume(returning: status == .authorized)
        }
      }
    }
    await refresh()
  }

  func openSettings() {
    guard let url = URL(string: UIApplication.openSettingsURLString) else { return }
    UIApplication.shared.open(url)
  }
}
