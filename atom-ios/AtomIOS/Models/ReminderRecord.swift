import Foundation
import SwiftData

@Model
final class ReminderRecord: Identifiable {
  @Attribute(.unique) var id: UUID
  var title: String
  var scheduledAtUTC: Date?
  var localDate: String?
  var localTime: String?
  var timezone: String
  var recurrenceRule: String?
  var state: String
  var source: String
  var createdAt: Date
  var updatedAt: Date

  init(
    id: UUID = UUID(),
    title: String,
    scheduledAtUTC: Date?,
    localDate: String?,
    localTime: String?,
    timezone: String,
    recurrenceRule: String?,
    state: String,
    source: String
  ) {
    self.id = id
    self.title = title
    self.scheduledAtUTC = scheduledAtUTC
    self.localDate = localDate
    self.localTime = localTime
    self.timezone = timezone
    self.recurrenceRule = recurrenceRule
    self.state = state
    self.source = source
    createdAt = Date()
    updatedAt = Date()
  }
}
