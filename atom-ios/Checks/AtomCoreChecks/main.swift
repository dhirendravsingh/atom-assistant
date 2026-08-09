import AtomCore
import Foundation

private struct CheckSuite {
  private(set) var count = 0

  mutating func expect(_ condition: @autoclosure () -> Bool, _ message: String) {
    count += 1
    guard condition() else {
      fatalError("AtomCore check failed: \(message)")
    }
  }
}

private func date(_ value: String) -> Date {
  let formatter = DateFormatter()
  formatter.locale = Locale(identifier: "en_US_POSIX")
  formatter.timeZone = TimeZone(identifier: "Asia/Kolkata")
  formatter.dateFormat = "yyyy-MM-dd HH:mm"
  guard let result = formatter.date(from: value) else {
    fatalError("Invalid fixture date: \(value)")
  }
  return result
}

private var checks = CheckSuite()
let timezone = TimeZone(identifier: "Asia/Kolkata")!
let parser = AtomCommandParser(timezone: timezone)
let now = date("2026-08-09 09:00")

let relative = parser.parse(
  "Hey Atom, remind me to drink water in 20 minutes",
  now: now
)
checks.expect(relative.title == "Drink water", "voice prefixes should be stripped")
checks.expect(
  relative.scheduledAtUTC == now.addingTimeInterval(1_200), "relative time should schedule")
checks.expect(!relative.needsDate && !relative.needsTime, "relative time should be complete")

let tomorrow = parser.parse(
  "Please remind me to call Mum tomorrow at 12 PM",
  now: now
)
checks.expect(tomorrow.localDate == "2026-08-10", "tomorrow should preserve the local date")
checks.expect(tomorrow.localTime == "12:00 PM", "noon should use twelve-hour time")

let needsTime = parser.parse("Remind me to submit expenses tomorrow", now: now)
checks.expect(
  !needsTime.needsDate && needsTime.needsTime, "a date-only reminder should request time")

let needsDate = parser.parse("Atom remind me to call at 4:30 PM", now: now)
checks.expect(
  needsDate.needsDate && !needsDate.needsTime, "a time-only reminder should request date")

let recurring = parser.parse("Remind me to stand up every weekday at 3 PM", now: now)
checks.expect(
  recurring.recurrenceRule == "FREQ=WEEKLY;BYDAY=MO,TU,WE,TH,FR",
  "weekday recurrence should be retained"
)
checks.expect(recurring.localDate == "2026-08-10", "weekend recurrence should advance to Monday")

let permissionPlan = iosPermissionPlan(
  for: IOSPermissionSnapshot(
    notificationsGranted: false,
    microphoneGranted: true,
    speechRecognitionGranted: false,
    prominentAlarmsSupported: false,
    prominentAlarmsGranted: false
  )
)
checks.expect(
  permissionPlan == [.notifications, .speechRecognition],
  "unsupported permissions should not be requested"
)

print("AtomCore checks passed: \(checks.count)")
