import SwiftData
import SwiftUI

struct NotificationHistoryView: View {
  @Environment(\.dismiss) private var dismiss
  @Environment(\.modelContext) private var modelContext
  @Query(sort: \NotificationHistoryRecord.occurredAt, order: .reverse)
  private var events: [NotificationHistoryRecord]
  @State private var selectedEvent: NotificationHistoryRecord?

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
      .overlay {
        if let event = selectedEvent {
          notificationDetail(event)
        }
      }
    }
  }

  private func historyCard(_ event: NotificationHistoryRecord) -> some View {
    let presentation = presentation(for: event.kind)
    return HStack(alignment: .top, spacing: 13) {
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
            Text(
              "\(event.kind == .rang ? "Scheduled" : "Next"): \(next.formatted(date: .abbreviated, time: .shortened))"
            )
              .font(.caption.weight(.semibold))
              .foregroundStyle(AtomColors.mint)
          }
        }
      }
      .padding(18)
      .frame(maxWidth: .infinity, alignment: .leading)
      .background(
        event.kind == .ignored ? AtomColors.coral.opacity(0.18) : Color.primary.opacity(0.035),
        in: RoundedRectangle(cornerRadius: 24)
      )
      .overlay {
        RoundedRectangle(cornerRadius: 24)
          .stroke(
            event.kind == .ignored ? AtomColors.coral.opacity(0.42) : Color.primary.opacity(0.07),
            lineWidth: 1
          )
      }
      .contentShape(RoundedRectangle(cornerRadius: 24))
      .onTapGesture { selectedEvent = event }
  }

  private func notificationDetail(_ event: NotificationHistoryRecord) -> some View {
    let presentation = presentation(for: event.kind)
    let ring = events
      .filter {
        $0.reminderID == event.reminderID && $0.kind == .rang && $0.occurredAt <= event.occurredAt
      }
      .max(by: { $0.occurredAt < $1.occurredAt })
    let scheduledFor = ring?.resultingScheduledAt
      ?? (event.kind == .rang ? event.resultingScheduledAt : nil)
      ?? ring?.occurredAt

    return ZStack {
      Color.black.opacity(0.42)
        .ignoresSafeArea()
        .onTapGesture { selectedEvent = nil }

      VStack(alignment: .leading, spacing: 17) {
        HStack(alignment: .top, spacing: 12) {
          Image(systemName: presentation.icon)
            .font(.headline)
            .foregroundStyle(presentation.color)
            .frame(width: 42, height: 42)
            .background(presentation.color.opacity(0.13), in: Circle())
          VStack(alignment: .leading, spacing: 3) {
            Text(presentation.label.uppercased())
              .font(.caption.weight(.bold))
              .foregroundStyle(presentation.color)
            Text(event.title)
              .font(.title3.weight(.semibold))
          }
          Spacer()
          Button { selectedEvent = nil } label: {
            Image(systemName: "xmark")
              .frame(width: 32, height: 32)
              .background(Color.primary.opacity(0.06), in: Circle())
          }
          .buttonStyle(.plain)
          .accessibilityLabel("Close notification details")
        }

        if let detail = event.detail {
          Text(detail)
            .font(.subheadline)
            .foregroundStyle(.secondary)
        }

        VStack(spacing: 0) {
          detailRow("Reminder set for", value: scheduledFor)
          Divider()
          detailRow("Alarm rang", value: ring?.occurredAt)
          Divider()
          detailRow("Action recorded", value: event.occurredAt)
          if event.kind != .rang, let next = event.resultingScheduledAt {
            Divider()
            detailRow("Next reminder", value: next, emphasized: true)
          }
        }
        .background(Color.primary.opacity(0.035), in: RoundedRectangle(cornerRadius: 18))
      }
      .padding(20)
      .background(.regularMaterial, in: RoundedRectangle(cornerRadius: 28))
      .overlay {
        RoundedRectangle(cornerRadius: 28).stroke(Color.primary.opacity(0.08), lineWidth: 1)
      }
      .padding(24)
    }
    .transition(.opacity)
  }

  private func detailRow(
    _ label: String,
    value: Date?,
    emphasized: Bool = false
  ) -> some View {
    HStack {
      Text(label)
        .font(.caption)
        .foregroundStyle(.secondary)
      Spacer()
      Text(value?.formatted(date: .abbreviated, time: .shortened) ?? "Not recorded")
        .font(.caption.weight(.semibold))
        .foregroundStyle(emphasized ? AtomColors.mint : Color.primary)
    }
    .padding(.horizontal, 13)
    .padding(.vertical, 12)
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
