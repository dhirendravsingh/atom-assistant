import SwiftData
import SwiftUI

struct NotificationHistoryView: View {
  @Environment(\.dismiss) private var dismiss
  @Environment(\.modelContext) private var modelContext
  @Query(sort: \NotificationHistoryRecord.occurredAt, order: .reverse)
  private var events: [NotificationHistoryRecord]

  var body: some View {
    NavigationStack {
      Group {
        if events.isEmpty {
          ContentUnavailableView(
            "No alarm activity yet",
            systemImage: "bell.slash",
            description: Text(
              "When a reminder rings or you act on it, the update will appear here."
            )
          )
        } else {
          ScrollView {
            LazyVStack(spacing: 12) {
              ForEach(events) { event in
                historyCard(event)
              }
            }
            .padding(20)
          }
        }
      }
      .background(AtomColors.canvas)
      .navigationTitle("Alarm activity")
      .navigationBarTitleDisplayMode(.inline)
      .toolbar {
        ToolbarItem(placement: .topBarLeading) {
          Button("Close") { dismiss() }
        }
      }
      .task { markAllRead() }
    }
  }

  private func historyCard(_ event: NotificationHistoryRecord) -> some View {
    let presentation = presentation(for: event.kind)
    return AtomCard {
      HStack(alignment: .top, spacing: 13) {
        Image(systemName: presentation.icon)
          .font(.headline)
          .foregroundStyle(presentation.color)
          .frame(width: 42, height: 42)
          .background(presentation.color.opacity(0.12), in: Circle())

        VStack(alignment: .leading, spacing: 5) {
          HStack(alignment: .firstTextBaseline) {
            Text(event.title)
              .font(.headline)
              .frame(maxWidth: .infinity, alignment: .leading)
            Text(presentation.label)
              .font(.caption.weight(.bold))
              .foregroundStyle(presentation.color)
          }
          if let detail = event.detail {
            Text(detail)
              .font(.subheadline)
              .foregroundStyle(.secondary)
          }
          Text(event.occurredAt.formatted(date: .abbreviated, time: .shortened))
            .font(.caption)
            .foregroundStyle(.secondary)
          if let next = event.resultingScheduledAt {
            Text("Next: \(next.formatted(date: .abbreviated, time: .shortened))")
              .font(.caption.weight(.semibold))
              .foregroundStyle(AtomColors.mint)
          }
        }
      }
    }
  }

  private func presentation(for kind: NotificationHistoryKind) -> (
    icon: String, label: String, color: Color
  ) {
    switch kind {
    case .rang: ("bell.badge.fill", "Rang", AtomColors.ink)
    case .opened: ("bell.fill", "Opened", AtomColors.ink)
    case .snoozed: ("zzz", "Snoozed", AtomColors.ink)
    case .remindedAgain: ("repeat", "Remind again", AtomColors.ink)
    case .completed: ("checkmark.circle.fill", "Completed", .green)
    case .ignored: ("xmark.circle.fill", "Ignored", AtomColors.coral)
    }
  }

  private func markAllRead() {
    guard events.contains(where: { !$0.isRead }) else { return }
    for event in events {
      event.isRead = true
    }
    try? modelContext.save()
  }
}
