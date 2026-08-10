import SwiftData
import SwiftUI

@main
struct AtomIOSApp: App {
  @StateObject private var permissions = PermissionCoordinator()

  init() {
    NotificationScheduler.shared.configure()
  }

  var body: some Scene {
    WindowGroup {
      AtomRootView()
        .environmentObject(permissions)
    }
    .modelContainer(for: [ReminderRecord.self, NotificationHistoryRecord.self])
  }
}
