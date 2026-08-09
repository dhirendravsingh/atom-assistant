import AtomCore
import Foundation
import Testing

@Suite("Natural-language parser")
struct AtomCommandParserTests {
  private let timezone = TimeZone(identifier: "Asia/Kolkata")!

  @Test("Relative reminders are complete")
  func relativeReminderIsComplete() throws {
    let parser = AtomCommandParser(timezone: timezone)
    let now = try #require(Self.date("2026-08-09 09:00"))

    let result = parser.parse("Hey Atom, remind me to drink water in 20 minutes", now: now)

    #expect(result.title == "Drink water")
    #expect(result.scheduledAtUTC == now.addingTimeInterval(1_200))
    #expect(!result.needsDate)
    #expect(!result.needsTime)
  }

  @Test("Tomorrow at noon uses twelve-hour time")
  func tomorrowAtNoonUsesTwelveHourTime() throws {
    let parser = AtomCommandParser(timezone: timezone)
    let now = try #require(Self.date("2026-08-09 09:00"))

    let result = parser.parse("Please remind me to call Mum tomorrow at 12 PM", now: now)

    #expect(result.title == "Call mum")
    #expect(result.localDate == "2026-08-10")
    #expect(result.localTime == "12:00 PM")
    #expect(!result.needsDate)
    #expect(!result.needsTime)
  }

  @Test("A date without a time requests the time")
  func dateWithoutTimeRequestsTime() throws {
    let parser = AtomCommandParser(timezone: timezone)
    let now = try #require(Self.date("2026-08-09 09:00"))

    let result = parser.parse("Remind me to submit expenses tomorrow", now: now)

    #expect(result.localDate == "2026-08-10")
    #expect(result.localTime == nil)
    #expect(!result.needsDate)
    #expect(result.needsTime)
  }

  @Test("A time without a date requests the date")
  func timeWithoutDateRequestsDate() throws {
    let parser = AtomCommandParser(timezone: timezone)
    let now = try #require(Self.date("2026-08-09 09:00"))

    let result = parser.parse("Atom remind me to call at 4:30 PM", now: now)

    #expect(result.localDate == nil)
    #expect(result.localTime == "4:30 PM")
    #expect(result.needsDate)
    #expect(!result.needsTime)
  }

  @Test("Weekday recurrence schedules the next weekday")
  func weekdayRecurrenceIsPreserved() throws {
    let parser = AtomCommandParser(timezone: timezone)
    let now = try #require(Self.date("2026-08-09 09:00"))

    let result = parser.parse("Remind me to stand up every weekday at 3 PM", now: now)

    #expect(result.recurrenceRule == "FREQ=WEEKLY;BYDAY=MO,TU,WE,TH,FR")
    #expect(result.title == "Stand up")
    #expect(result.localDate == "2026-08-10")
    #expect(!result.needsDate)
  }

  private static func date(_ value: String) -> Date? {
    let formatter = DateFormatter()
    formatter.locale = Locale(identifier: "en_US_POSIX")
    formatter.timeZone = TimeZone(identifier: "Asia/Kolkata")
    formatter.dateFormat = "yyyy-MM-dd HH:mm"
    return formatter.date(from: value)
  }
}

@Suite("iOS permission plan")
struct IOSPermissionPlanTests {
  @Test("Only missing supported permissions are requested")
  func requestsOnlyMissingSupportedPermissions() {
    let plan = iosPermissionPlan(
      for: IOSPermissionSnapshot(
        notificationsGranted: false,
        microphoneGranted: true,
        speechRecognitionGranted: false,
        prominentAlarmsSupported: false,
        prominentAlarmsGranted: false
      )
    )

    #expect(plan == [.notifications, .speechRecognition])
  }

  @Test("Prominent alarm permission appears only when supported")
  func prominentAlarmPermissionAppearsOnlyWhenSupported() {
    let plan = iosPermissionPlan(
      for: IOSPermissionSnapshot(
        notificationsGranted: true,
        microphoneGranted: true,
        speechRecognitionGranted: true,
        prominentAlarmsSupported: true,
        prominentAlarmsGranted: false
      )
    )

    #expect(plan == [.prominentAlarms])
  }
}
