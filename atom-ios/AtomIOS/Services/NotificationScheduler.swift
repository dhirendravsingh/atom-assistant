import Foundation
import UserNotifications

enum AtomNotificationAction {
  static let done = "ATOM_DONE"
  static let snooze = "ATOM_SNOOZE"
  static let remindAgain = "ATOM_REMIND_AGAIN"
  static let category = "ATOM_REMINDER"
}

struct NotificationActionMutation: Codable {
  enum Kind: String, Codable {
    case completed
    case rescheduled
  }

  let reminderID: UUID
  let kind: Kind
  let scheduledAt: Date?
}

final class NotificationScheduler: NSObject, UNUserNotificationCenterDelegate {
  static let shared = NotificationScheduler()

  private let center = UNUserNotificationCenter.current()
  private let actionStoreKey = "atom.pendingNotificationActions"

  private override init() {
    super.init()
  }

  func configure() {
    let done = UNNotificationAction(
      identifier: AtomNotificationAction.done,
      title: "Done",
      options: []
    )
    let snooze = UNNotificationAction(
      identifier: AtomNotificationAction.snooze,
      title: "Snooze 10 min",
      options: []
    )
    let remindAgain = UNNotificationAction(
      identifier: AtomNotificationAction.remindAgain,
      title: "Remind in 1 hour",
      options: []
    )
    center.setNotificationCategories([
      UNNotificationCategory(
        identifier: AtomNotificationAction.category,
        actions: [done, snooze, remindAgain],
        intentIdentifiers: [],
        options: [.customDismissAction]
      )
    ])
    center.delegate = self
  }

  func schedule(id: UUID, title: String, at date: Date) async throws {
    cancel(id: id)
    let content = reminderContent(id: id, title: title)
    let components = Calendar.current.dateComponents(
      [.calendar, .timeZone, .year, .month, .day, .hour, .minute, .second],
      from: date
    )
    let request = UNNotificationRequest(
      identifier: id.uuidString,
      content: content,
      trigger: UNCalendarNotificationTrigger(dateMatching: components, repeats: false)
    )
    try await center.add(request)
  }

  func cancel(id: UUID) {
    let identifier = id.uuidString
    center.removePendingNotificationRequests(withIdentifiers: [identifier])
    center.removeDeliveredNotifications(withIdentifiers: [identifier])
  }

  func userNotificationCenter(
    _ center: UNUserNotificationCenter,
    willPresent notification: UNNotification
  ) async -> UNNotificationPresentationOptions {
    [.banner, .list, .sound]
  }

  func userNotificationCenter(
    _ center: UNUserNotificationCenter,
    didReceive response: UNNotificationResponse
  ) async {
    let request = response.notification.request
    guard let reminderID = request.content.userInfo["reminderID"] as? String,
      let id = UUID(uuidString: reminderID)
    else { return }

    switch response.actionIdentifier {
    case AtomNotificationAction.done:
      cancel(id: id)
      recordAction(.init(reminderID: id, kind: .completed, scheduledAt: nil))
    case AtomNotificationAction.snooze:
      let date = Date().addingTimeInterval(10 * 60)
      try? await schedule(
        id: id,
        title: request.content.body,
        at: date
      )
      recordAction(.init(reminderID: id, kind: .rescheduled, scheduledAt: date))
    case AtomNotificationAction.remindAgain:
      let date = Date().addingTimeInterval(60 * 60)
      try? await schedule(
        id: id,
        title: request.content.body,
        at: date
      )
      recordAction(.init(reminderID: id, kind: .rescheduled, scheduledAt: date))
    default:
      break
    }

    func drainRecordedActions() -> [NotificationActionMutation] {
      let defaults = UserDefaults.standard
      guard let data = defaults.data(forKey: actionStoreKey),
        let actions = try? JSONDecoder().decode([NotificationActionMutation].self, from: data)
      else { return [] }
      defaults.removeObject(forKey: actionStoreKey)
      return actions
    }
  }

  private func reminderContent(id: UUID, title: String) -> UNMutableNotificationContent {
    let content = UNMutableNotificationContent()
    content.title = "Atom reminder"
    content.body = title
    content.sound = .default
    content.categoryIdentifier = AtomNotificationAction.category
    content.threadIdentifier = "atom-reminders"
    content.userInfo = ["reminderID": id.uuidString]
    if #available(iOS 15.0, *) {
      content.interruptionLevel = .timeSensitive
    }
    return content
  }

  private func recordAction(_ action: NotificationActionMutation) {
    let defaults = UserDefaults.standard
    var actions: [NotificationActionMutation] = []
    if let data = defaults.data(forKey: actionStoreKey),
      let stored = try? JSONDecoder().decode([NotificationActionMutation].self, from: data)
    {
      actions = stored
    }
    actions.removeAll { $0.reminderID == action.reminderID }
    actions.append(action)
    if let data = try? JSONEncoder().encode(actions) {
      defaults.set(data, forKey: actionStoreKey)
    }
  }
}
