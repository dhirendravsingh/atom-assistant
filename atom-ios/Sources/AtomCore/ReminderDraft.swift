import Foundation

public enum ReminderSource: String, Codable, Sendable {
  case text
  case voice
}

public struct ReminderDraft: Codable, Equatable, Identifiable, Sendable {
  public let title: String
  public let scheduledAtUTC: Date?
  public let localDate: String?
  public let localTime: String?
  public let timezone: String
  public let recurrenceRule: String?
  public let needsDate: Bool
  public let needsTime: Bool
  public let conflicts: [String]

  public var id: String {
    [title, localDate ?? "", localTime ?? "", timezone].joined(separator: "|")
  }

  public init(
    title: String,
    scheduledAtUTC: Date?,
    localDate: String?,
    localTime: String?,
    timezone: String,
    recurrenceRule: String?,
    needsDate: Bool,
    needsTime: Bool,
    conflicts: [String] = []
  ) {
    self.title = title
    self.scheduledAtUTC = scheduledAtUTC
    self.localDate = localDate
    self.localTime = localTime
    self.timezone = timezone
    self.recurrenceRule = recurrenceRule
    self.needsDate = needsDate
    self.needsTime = needsTime
    self.conflicts = conflicts
  }
}
