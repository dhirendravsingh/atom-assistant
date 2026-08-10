import Foundation
import SwiftData

enum NotificationHistoryKind: String, Codable, CaseIterable {
  case rang
  case opened
  case snoozed
  case remindedAgain
  case completed
  case ignored
}

@Model
final class NotificationHistoryRecord: Identifiable {
  @Attribute(.unique) var id: UUID
  var reminderID: UUID
  var title: String
  var eventType: String
  var detail: String?
  var resultingScheduledAt: Date?
  var occurredAt: Date
  var isRead: Bool

  init(
    id: UUID,
    reminderID: UUID,
    title: String,
    eventType: NotificationHistoryKind,
    detail: String?,
    resultingScheduledAt: Date?,
    occurredAt: Date,
    isRead: Bool = false
  ) {
    self.id = id
    self.reminderID = reminderID
    self.title = title
    self.eventType = eventType.rawValue
    self.detail = detail
    self.resultingScheduledAt = resultingScheduledAt
    self.occurredAt = occurredAt
    self.isRead = isRead
  }

  var kind: NotificationHistoryKind {
    NotificationHistoryKind(rawValue: eventType) ?? .rang
  }
}
