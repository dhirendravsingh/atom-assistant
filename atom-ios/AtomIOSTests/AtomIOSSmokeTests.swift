import XCTest
@testable import atom_ios

final class AtomIOSSmokeTests: XCTestCase {
  func testHarnessLoads() {
    XCTAssertTrue(true)
  }

  func testNotificationHistoryRecordPreservesAlarmUpdate() {
    let eventID = UUID()
    let reminderID = UUID()
    let now = Date(timeIntervalSince1970: 1_786_349_400)
    let next = now.addingTimeInterval(600)
    let record = NotificationHistoryRecord(
      id: eventID,
      reminderID: reminderID,
      title: "Call Mum",
      eventType: .snoozed,
      detail: "Snoozed for 10 minutes",
      resultingScheduledAt: next,
      occurredAt: now
    )

    XCTAssertEqual(eventID, record.id)
    XCTAssertEqual(reminderID, record.reminderID)
    XCTAssertEqual(.snoozed, record.kind)
    XCTAssertEqual(next, record.resultingScheduledAt)
    XCTAssertFalse(record.isRead)
  }
}
