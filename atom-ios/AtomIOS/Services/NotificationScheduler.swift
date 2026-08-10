import Foundation
import UserNotifications

enum AtomNotificationAction {
  static let done = "ATOM_DONE"
  static let snooze = "ATOM_SNOOZE"
  static let remindAgain = "ATOM_REMIND_AGAIN"
  static let ignore = "ATOM_IGNORE"
  static let category = "ATOM_REMINDER"
}

struct NotificationActionMutation: Codable {
  enum Kind: String, Codable {
    case completed
    case ignored
    case rescheduled
  }

  let reminderID: UUID
  let kind: Kind
  let scheduledAt: Date?
}

struct PendingNotificationHistoryEvent: Codable {
  let id: UUID
  let reminderID: UUID
  let title: String
  let kind: NotificationHistoryKind
  let detail: String?
  let resultingScheduledAt: Date?
  let occurredAt: Date
}

extension Notification.Name {
  static let atomNotificationHistoryDidChange = Notification.Name(
    "atom.notificationHistoryDidChange"
  )
}

final class NotificationScheduler: NSObject, UNUserNotificationCenterDelegate {
  static let shared = NotificationScheduler()

  private let center = UNUserNotificationCenter.current()
  private let actionStoreKey = "atom.pendingNotificationActions"
  private let historyStoreKey = "atom.pendingNotificationHistory"
  private let storeLock = NSLock()

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
    let ignore = UNNotificationAction(
      identifier: AtomNotificationAction.ignore,
      title: "Ignore",
      options: [.destructive]
    )
    center.setNotificationCategories([
      UNNotificationCategory(
        identifier: AtomNotificationAction.category,
        actions: [done, snooze, remindAgain, ignore],
        intentIdentifiers: [],
        options: [.customDismissAction]
      )
    ])
    center.delegate = self
  }

  func schedule(id: UUID, title: String, at date: Date) async throws {
    cancel(id: id)
    let content = reminderContent(
      id: id,
      occurrenceID: UUID(),
      title: title,
      scheduledAt: date
    )
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
    recordRang(notification)
    return [.banner, .list, .sound]
  }

  func userNotificationCenter(
    _ center: UNUserNotificationCenter,
    didReceive response: UNNotificationResponse
  ) async {
    let request = response.notification.request
    guard let reminderID = request.content.userInfo["reminderID"] as? String,
      let id = UUID(uuidString: reminderID)
    else { return }

    recordRang(response.notification)

    switch response.actionIdentifier {
    case AtomNotificationAction.done:
      cancel(id: id)
      recordAction(.init(reminderID: id, kind: .completed, scheduledAt: nil))
      recordHistory(
        reminderID: id,
        title: request.content.body,
        kind: .completed,
        detail: "Marked done"
      )
    case AtomNotificationAction.snooze:
      let date = Date().addingTimeInterval(10 * 60)
      try? await schedule(
        id: id,
        title: request.content.body,
        at: date
      )
      recordAction(.init(reminderID: id, kind: .rescheduled, scheduledAt: date))
      recordHistory(
        reminderID: id,
        title: request.content.body,
        kind: .snoozed,
        detail: "Snoozed for 10 minutes",
        resultingScheduledAt: date
      )
    case AtomNotificationAction.remindAgain:
      let date = Date().addingTimeInterval(60 * 60)
      try? await schedule(
        id: id,
        title: request.content.body,
        at: date
      )
      recordAction(.init(reminderID: id, kind: .rescheduled, scheduledAt: date))
      recordHistory(
        reminderID: id,
        title: request.content.body,
        kind: .remindedAgain,
        detail: "Asked Atom to remind again in 1 hour",
        resultingScheduledAt: date
      )
    case AtomNotificationAction.ignore, UNNotificationDismissActionIdentifier:
      cancel(id: id)
      recordAction(.init(reminderID: id, kind: .ignored, scheduledAt: nil))
      recordHistory(
        reminderID: id,
        title: request.content.body,
        kind: .ignored,
        detail: "Ignored"
      )
    case UNNotificationDefaultActionIdentifier:
      recordHistory(
        reminderID: id,
        title: request.content.body,
        kind: .opened,
        detail: "Opened from the notification"
      )
    default:
      break
    }
  }

  func drainRecordedActions() -> [NotificationActionMutation] {
    let defaults = UserDefaults.standard
    guard let data = defaults.data(forKey: actionStoreKey),
      let actions = try? JSONDecoder().decode([NotificationActionMutation].self, from: data)
    else { return [] }
    defaults.removeObject(forKey: actionStoreKey)
    return actions
  }

  func synchronizeDeliveredHistory() async {
    let notifications = await center.deliveredNotifications()
    notifications.forEach(recordRang)
  }

  func drainHistoryEvents() -> [PendingNotificationHistoryEvent] {
    storeLock.lock()
    defer { storeLock.unlock() }
    let defaults = UserDefaults.standard
    guard let data = defaults.data(forKey: historyStoreKey),
      let events = try? JSONDecoder().decode([PendingNotificationHistoryEvent].self, from: data)
    else { return [] }
    defaults.removeObject(forKey: historyStoreKey)
    return events
  }

  private func reminderContent(
    id: UUID,
    occurrenceID: UUID,
    title: String,
    scheduledAt: Date
  ) -> UNMutableNotificationContent {
    let content = UNMutableNotificationContent()
    content.title = "Atom reminder"
    content.body = title
    content.sound = .default
    content.categoryIdentifier = AtomNotificationAction.category
    content.threadIdentifier = "atom-reminders"
    content.userInfo = [
      "reminderID": id.uuidString,
      "occurrenceID": occurrenceID.uuidString,
      "scheduledAt": scheduledAt.timeIntervalSince1970,
    ]
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

  private func recordRang(_ notification: UNNotification) {
    let request = notification.request
    guard let reminderText = request.content.userInfo["reminderID"] as? String,
      let reminderID = UUID(uuidString: reminderText)
    else { return }

    let occurrenceID =
      (request.content.userInfo["occurrenceID"] as? String)
      .flatMap(UUID.init(uuidString:)) ?? reminderID
    recordHistory(
      id: occurrenceID,
      reminderID: reminderID,
      title: request.content.body,
      kind: .rang,
      detail: "Reminder rang",
      occurredAt: notification.date
    )
  }

  private func recordHistory(
    id: UUID = UUID(),
    reminderID: UUID,
    title: String,
    kind: NotificationHistoryKind,
    detail: String?,
    resultingScheduledAt: Date? = nil,
    occurredAt: Date = Date()
  ) {
    let event = PendingNotificationHistoryEvent(
      id: id,
      reminderID: reminderID,
      title: title.isEmpty ? "Reminder" : title,
      kind: kind,
      detail: detail,
      resultingScheduledAt: resultingScheduledAt,
      occurredAt: occurredAt
    )
    storeLock.lock()
    let defaults = UserDefaults.standard
    var events: [PendingNotificationHistoryEvent] = []
    if let data = defaults.data(forKey: historyStoreKey),
      let stored = try? JSONDecoder().decode([PendingNotificationHistoryEvent].self, from: data)
    {
      events = stored
    }
    if !events.contains(where: { $0.id == event.id }) {
      events.append(event)
      if let data = try? JSONEncoder().encode(events) {
        defaults.set(data, forKey: historyStoreKey)
      }
    }
    storeLock.unlock()
    DispatchQueue.main.async {
      NotificationCenter.default.post(name: .atomNotificationHistoryDidChange, object: nil)
    }
  }
}
