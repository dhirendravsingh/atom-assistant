import Foundation

public struct AtomCommandParser: Sendable {
  private let calendar: Calendar
  private let timezone: TimeZone

  public init(
    calendar: Calendar = .current,
    timezone: TimeZone = .current
  ) {
    var configuredCalendar = calendar
    configuredCalendar.timeZone = timezone
    self.calendar = configuredCalendar
    self.timezone = timezone
  }

  public func parse(_ input: String, now: Date = Date()) -> ReminderDraft {
    var working = stripPrefix(from: normalized(input))
    var conflicts: [String] = []
    let recurrenceRule = extractRecurrence(from: working)

    if let relative = firstMatch(
      pattern: #"\bin\s+(\d+)\s+(minute|minutes|hour|hours|day|days)\b"#,
      in: working
    ), let amount = Int(relative.groups[0]) {
      let unit = relative.groups[1]
      let seconds: TimeInterval
      if unit.hasPrefix("minute") {
        seconds = TimeInterval(amount * 60)
      } else if unit.hasPrefix("hour") {
        seconds = TimeInterval(amount * 3_600)
      } else {
        seconds = TimeInterval(amount * 86_400)
      }
      let scheduledAt = now.addingTimeInterval(seconds)
      working = removing(relative.fullMatch, from: working)
      return makeDraft(
        title: cleanedTitle(working),
        scheduledAt: scheduledAt,
        recurrenceRule: recurrenceRule,
        needsDate: false,
        needsTime: false,
        conflicts: conflicts
      )
    }

    let timeMatches = matches(
      pattern: #"\b(?:at\s+)?(1[0-2]|0?[1-9])(?::([0-5]\d))?\s*([ap])\.?m\.?\b"#,
      in: working
    )
    if timeMatches.count > 1 { conflicts.append("multiple times") }

    let mentionsToday = containsWord("today", in: working)
    let mentionsTomorrow = containsWord("tomorrow", in: working)
    if mentionsToday, mentionsTomorrow { conflicts.append("multiple dates") }

    var dateBase: Date? = {
      if mentionsTomorrow {
        return calendar.date(byAdding: .day, value: 1, to: calendar.startOfDay(for: now))
      }
      if mentionsToday { return calendar.startOfDay(for: now) }
      return nil
    }()

    let timeParts: DateComponents? = timeMatches.first.flatMap { match in
      guard let rawHour = Int(match.groups[0]) else { return nil }
      let minute = Int(match.groups[1]) ?? 0
      let marker = match.groups[2]
      let hour = marker == "p" ? (rawHour % 12) + 12 : rawHour % 12
      return DateComponents(hour: hour, minute: minute)
    }

    if dateBase == nil, let recurrenceRule, let timeParts {
      dateBase = nextRecurringDate(rule: recurrenceRule, timeParts: timeParts, after: now)
    }

    var scheduledAt: Date?
    if let dateBase, let timeParts {
      scheduledAt = calendar.date(
        bySettingHour: timeParts.hour ?? 0,
        minute: timeParts.minute ?? 0,
        second: 0,
        of: dateBase
      )
    }

    for match in timeMatches { working = removing(match.fullMatch, from: working) }
    working = removingWords(["today", "tomorrow"], from: working)
    working = removingRecurrence(from: working)

    return makeDraft(
      title: cleanedTitle(working),
      scheduledAt: scheduledAt,
      dateBase: dateBase,
      timeParts: timeParts,
      recurrenceRule: recurrenceRule,
      needsDate: dateBase == nil,
      needsTime: timeParts == nil,
      conflicts: conflicts
    )
  }

  private func makeDraft(
    title: String,
    scheduledAt: Date?,
    dateBase: Date? = nil,
    timeParts: DateComponents? = nil,
    recurrenceRule: String?,
    needsDate: Bool,
    needsTime: Bool,
    conflicts: [String]
  ) -> ReminderDraft {
    let displayDate = scheduledAt ?? dateBase
    let localDate = displayDate.map { Self.dateFormatter(timezone: timezone).string(from: $0) }
    let localTime: String? = {
      if let scheduledAt {
        return Self.timeFormatter(timezone: timezone).string(from: scheduledAt)
      }
      guard let timeParts,
        let synthetic = calendar.date(
          from: DateComponents(
            year: 2001,
            month: 1,
            day: 1,
            hour: timeParts.hour,
            minute: timeParts.minute
          ))
      else { return nil }
      return Self.timeFormatter(timezone: timezone).string(from: synthetic)
    }()
    return ReminderDraft(
      title: title,
      scheduledAtUTC: scheduledAt,
      localDate: localDate,
      localTime: localTime,
      timezone: timezone.identifier,
      recurrenceRule: recurrenceRule,
      needsDate: needsDate,
      needsTime: needsTime,
      conflicts: conflicts
    )
  }

  private func extractRecurrence(from input: String) -> String? {
    if input.range(of: #"\bevery\s+weekday\b"#, options: .regularExpression) != nil {
      return "FREQ=WEEKLY;BYDAY=MO,TU,WE,TH,FR"
    }
    if input.range(of: #"\bevery\s+day\b"#, options: .regularExpression) != nil {
      return "FREQ=DAILY"
    }
    if input.range(of: #"\bevery\s+week\b"#, options: .regularExpression) != nil {
      return "FREQ=WEEKLY"
    }
    return nil
  }

  private func removingRecurrence(from input: String) -> String {
    removingPatterns(
      [#"\bevery\s+weekday\b"#, #"\bevery\s+day\b"#, #"\bevery\s+week\b"#],
      from: input
    )
  }

  private func stripPrefix(from input: String) -> String {
    let prefixes = [
      "hey atom", "hi atom", "hello atom", "okay atom", "ok atom", "atom",
      "can you remind me to", "could you remind me to", "would you remind me to",
      "please remind me to", "remind me to", "set a reminder to", "i need to remember to",
      "don't let me forget to", "do not let me forget to",
    ]
    var result = input
    var removedPrefix = true
    while removedPrefix {
      removedPrefix = false
      for prefix in prefixes.sorted(by: { $0.count > $1.count }) {
        let pattern =
          "^" + NSRegularExpression.escapedPattern(for: prefix)
          + #"(?:\b|$)[\s,.:;!\-]*"#
        guard
          let range = result.range(of: pattern, options: [.regularExpression, .caseInsensitive])
        else { continue }
        result.removeSubrange(range)
        result = result.trimmingCharacters(in: .whitespaces)
        removedPrefix = true
        break
      }
    }
    return result
  }

  private func nextRecurringDate(
    rule: String,
    timeParts: DateComponents,
    after now: Date
  ) -> Date? {
    for offset in 0...7 {
      guard
        let day = calendar.date(
          byAdding: .day,
          value: offset,
          to: calendar.startOfDay(for: now)
        ),
        let candidate = calendar.date(
          bySettingHour: timeParts.hour ?? 0,
          minute: timeParts.minute ?? 0,
          second: 0,
          of: day
        ), candidate > now
      else { continue }
      if rule.contains("BYDAY=MO,TU,WE,TH,FR") {
        let weekday = calendar.component(.weekday, from: candidate)
        if weekday == 1 || weekday == 7 { continue }
      }
      return candidate
    }
    return nil
  }

  private func normalized(_ input: String) -> String {
    input
      .lowercased()
      .replacingOccurrences(of: "’", with: "'")
      .trimmingCharacters(in: .whitespacesAndNewlines)
  }

  private func cleanedTitle(_ input: String) -> String {
    let value =
      input
      .replacingOccurrences(of: #"\s+"#, with: " ", options: .regularExpression)
      .trimmingCharacters(in: CharacterSet.whitespacesAndNewlines.union(.punctuationCharacters))
    guard !value.isEmpty else { return "Reminder" }
    return value.prefix(1).uppercased() + String(value.dropFirst())
  }

  private func removing(_ match: String, from input: String) -> String {
    input.replacingOccurrences(of: match, with: " ", options: [.caseInsensitive])
  }

  private func removingWords(_ words: [String], from input: String) -> String {
    removingPatterns(
      words.map { #"\b"# + NSRegularExpression.escapedPattern(for: $0) + #"\b"# }, from: input)
  }

  private func removingPatterns(_ patterns: [String], from input: String) -> String {
    patterns.reduce(input) { partial, pattern in
      partial.replacingOccurrences(
        of: pattern, with: " ", options: [.regularExpression, .caseInsensitive])
    }
  }

  private func containsWord(_ word: String, in input: String) -> Bool {
    input.range(
      of: #"\b"# + NSRegularExpression.escapedPattern(for: word) + #"\b"#,
      options: [.regularExpression, .caseInsensitive]
    ) != nil
  }

  private struct RegexMatch {
    let fullMatch: String
    let groups: [String]
  }

  private func firstMatch(pattern: String, in input: String) -> RegexMatch? {
    matches(pattern: pattern, in: input).first
  }

  private func matches(pattern: String, in input: String) -> [RegexMatch] {
    guard let regex = try? NSRegularExpression(pattern: pattern, options: [.caseInsensitive]) else {
      return []
    }
    let range = NSRange(input.startIndex..., in: input)
    return regex.matches(in: input, range: range).compactMap { result in
      guard let fullRange = Range(result.range(at: 0), in: input) else { return nil }
      let groups = (1..<result.numberOfRanges).map { index -> String in
        guard result.range(at: index).location != NSNotFound,
          let range = Range(result.range(at: index), in: input)
        else { return "" }
        return String(input[range]).lowercased()
      }
      return RegexMatch(fullMatch: String(input[fullRange]), groups: groups)
    }
  }

  private static func dateFormatter(timezone: TimeZone) -> DateFormatter {
    let formatter = DateFormatter()
    formatter.locale = Locale(identifier: "en_US_POSIX")
    formatter.timeZone = timezone
    formatter.dateFormat = "yyyy-MM-dd"
    return formatter
  }

  private static func timeFormatter(timezone: TimeZone) -> DateFormatter {
    let formatter = DateFormatter()
    formatter.locale = Locale(identifier: "en_US_POSIX")
    formatter.timeZone = timezone
    formatter.dateFormat = "h:mm a"
    return formatter
  }
}
