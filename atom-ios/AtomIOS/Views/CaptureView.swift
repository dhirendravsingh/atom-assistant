import AtomCore
import SwiftData
import SwiftUI

struct CaptureView: View {
  @Environment(\.modelContext) private var modelContext
  @EnvironmentObject private var permissions: PermissionCoordinator
  @AppStorage("atom.ownerName") private var ownerName = "Dhiren"
  @AppStorage("atom.gender") private var gender = "Prefer not to say"
  @StateObject private var speech = SpeechCaptureService()
  @State private var input = ""
  @State private var pendingDraft: ReminderDraft?
  @State private var followUpDate = Date().addingTimeInterval(3_600)
  @State private var errorMessage: String?
  @State private var savedMessage: String?
  @State private var source: ReminderSource = .text
  @Query(sort: \NotificationHistoryRecord.occurredAt, order: .reverse)
  private var notificationHistory: [NotificationHistoryRecord]

  let onOpenReminders: () -> Void
  let onOpenNotificationHistory: () -> Void

  var body: some View {
    ScrollView {
      VStack(alignment: .leading, spacing: 24) {
        HStack {
          AtomMark(compact: true)
          Spacer()
          Button(action: onOpenNotificationHistory) {
            ZStack(alignment: .topTrailing) {
              Image(systemName: "bell")
                .font(.headline)
                .frame(width: 38, height: 38)
                .background(.primary.opacity(0.06), in: Circle())
              if notificationHistory.contains(where: { !$0.isRead }) {
                Circle()
                  .fill(AtomColors.coral)
                  .frame(width: 8, height: 8)
                  .overlay(Circle().stroke(AtomColors.canvas, lineWidth: 2))
              }
            }
          }
          .buttonStyle(.plain)
          .accessibilityLabel("Notification history")
          Text(Date.now.formatted(date: .abbreviated, time: .omitted))
            .font(.caption.weight(.semibold))
            .foregroundStyle(.secondary)
        }

        VStack(alignment: .leading, spacing: 7) {
          Text(greeting)
            .font(.system(.largeTitle, design: .rounded, weight: .bold))
          Text("What should I remember for you?")
            .font(.title3)
            .foregroundStyle(.secondary)
        }

        quickCapture

        if let savedMessage {
          Label(savedMessage, systemImage: "checkmark.circle.fill")
            .font(.subheadline.weight(.semibold))
            .foregroundStyle(.green)
            .transition(.opacity.combined(with: .move(edge: .top)))
        }
        if let errorMessage = errorMessage ?? speech.errorMessage {
          Label(errorMessage, systemImage: "exclamationmark.triangle.fill")
            .font(.subheadline)
            .foregroundStyle(AtomColors.coral)
        }

        AtomCard {
          VStack(alignment: .leading, spacing: 10) {
            Text("TRY SAYING")
              .font(.caption.weight(.bold))
              .tracking(1.2)
              .foregroundStyle(.secondary)
            Text("“Hey Atom, remind me to call Mum tomorrow at 12 PM.”")
              .font(.headline)
            Text(
              "Prefixes such as Atom, Hey Atom, Please remind me, and Can you remind me are optional."
            )
            .font(.footnote)
            .foregroundStyle(.secondary)
          }
        }

        Button("View all reminders", action: onOpenReminders)
          .buttonStyle(.bordered)
          .frame(maxWidth: .infinity)
      }
      .padding(20)
    }
    .background(AtomColors.canvas)
    .navigationBarHidden(true)
    .onChange(of: speech.transcript) { _, value in
      guard !value.isEmpty else { return }
      input = value
      source = .voice
    }
    .sheet(item: $pendingDraft) { draft in
      ScheduleFollowUpView(
        draft: draft,
        selectedDate: $followUpDate,
        onSave: { save(draft, scheduledAt: followUpDate) },
        onSaveUnscheduled: { save(draft, scheduledAt: nil) },
        onCancel: { pendingDraft = nil }
      )
    }
  }

  private var quickCapture: some View {
    VStack(alignment: .leading, spacing: 16) {
      TextField("Remind me to…", text: $input, axis: .vertical)
        .font(.title3.weight(.medium))
        .lineLimit(2...5)
        .foregroundStyle(.white)
        .textInputAutocapitalization(.sentences)

      HStack {
        Text("Type naturally or tap the mic")
          .font(.caption)
          .foregroundStyle(.white.opacity(0.55))
        Spacer()
        Button {
          Task {
            if !permissions.microphoneGranted || !permissions.speechGranted {
              await permissions.requestInitialPermissions()
            }
            speech.toggle()
          }
        } label: {
          Image(systemName: speech.isListening ? "stop.fill" : "mic.fill")
            .font(.title3)
            .frame(width: 48, height: 48)
            .background(speech.isListening ? AtomColors.coral : AtomColors.mint, in: Circle())
            .foregroundStyle(AtomColors.ink)
        }
        .accessibilityLabel(speech.isListening ? "Stop listening" : "Start voice reminder")

        Button(action: parseAndSave) {
          Image(systemName: "arrow.up")
            .font(.title3.weight(.bold))
            .frame(width: 48, height: 48)
            .background(.white, in: Circle())
            .foregroundStyle(AtomColors.ink)
        }
        .disabled(input.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
        .opacity(input.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ? 0.45 : 1)
        .accessibilityLabel("Save reminder")
      }
    }
    .padding(20)
    .background(AtomColors.ink, in: RoundedRectangle(cornerRadius: 28))
    .overlay(alignment: .trailing) {
      VStack(spacing: 8) {
        Capsule().fill(AtomColors.coral).frame(width: 3, height: 36)
        Capsule().fill(AtomColors.mint).frame(width: 3, height: 54)
      }
      .padding(.trailing, 5)
    }
  }

  private var greeting: String {
    let hour = Calendar.current.component(.hour, from: Date())
    let period = hour < 12 ? "Good morning" : hour < 17 ? "Good afternoon" : "Good evening"
    let greetingName =
      switch gender {
      case "Man": "\(ownerName) Sir"
      case "Woman": "\(ownerName) Ma’am"
      default: ownerName
      }
    return "\(period), \(greetingName)"
  }

  private func parseAndSave() {
    errorMessage = nil
    let draft = AtomCommandParser().parse(input)
    if !draft.conflicts.isEmpty {
      errorMessage =
        "I found conflicting \(draft.conflicts.joined(separator: " and ")). Please edit the reminder."
      return
    }
    if draft.needsDate || draft.needsTime {
      pendingDraft = draft
      followUpDate = Date().addingTimeInterval(3_600)
    } else {
      save(draft, scheduledAt: draft.scheduledAtUTC)
    }
  }

  private func save(_ draft: ReminderDraft, scheduledAt: Date?) {
    let dateFormatter = DateFormatter()
    dateFormatter.dateFormat = "yyyy-MM-dd"
    let timeFormatter = DateFormatter()
    timeFormatter.dateFormat = "h:mm a"
    let record = ReminderRecord(
      title: draft.title,
      scheduledAtUTC: scheduledAt,
      localDate: scheduledAt.map(dateFormatter.string),
      localTime: scheduledAt.map(timeFormatter.string),
      timezone: TimeZone.current.identifier,
      recurrenceRule: draft.recurrenceRule,
      state: scheduledAt == nil ? "unscheduled" : "scheduled",
      source: source.rawValue
    )
    modelContext.insert(record)
    try? modelContext.save()
    if let scheduledAt {
      Task {
        try? await NotificationScheduler.shared.schedule(
          id: record.id,
          title: record.title,
          at: scheduledAt
        )
      }
    }
    speech.stop()
    input = ""
    source = .text
    pendingDraft = nil
    savedMessage = scheduledAt == nil ? "Saved to Unscheduled" : "Reminder scheduled"
  }
}

private struct ScheduleFollowUpView: View {
  @EnvironmentObject private var permissions: PermissionCoordinator
  @StateObject private var speech = SpeechCaptureService()
  let draft: ReminderDraft
  @Binding var selectedDate: Date
  let onSave: () -> Void
  let onSaveUnscheduled: () -> Void
  let onCancel: () -> Void

  var body: some View {
    NavigationStack {
      VStack(alignment: .leading, spacing: 22) {
        AtomMark(compact: true)
        Text(
          draft.needsDate && draft.needsTime
            ? "When should I remind you?" : draft.needsDate ? "Which date?" : "What time?"
        )
        .font(.system(.title, design: .rounded, weight: .bold))
        Text(draft.title).font(.headline).foregroundStyle(.secondary)
        DatePicker(
          "Reminder date and time",
          selection: $selectedDate,
          in: Date()...,
          displayedComponents: [.date, .hourAndMinute]
        )
        .datePickerStyle(.graphical)

        Button {
          Task {
            if !permissions.microphoneGranted || !permissions.speechGranted {
              await permissions.requestInitialPermissions()
            }
            speech.toggle()
          }
        } label: {
          Label(
            speech.isListening ? "Stop listening" : "Say the missing date and time",
            systemImage: speech.isListening ? "stop.fill" : "mic.fill"
          )
          .frame(maxWidth: .infinity)
        }
        .buttonStyle(.bordered)

        Button("Schedule reminder", action: onSave)
          .buttonStyle(.borderedProminent)
          .tint(AtomColors.ink)
          .frame(maxWidth: .infinity)
        Button("Save without a schedule", action: onSaveUnscheduled)
          .frame(maxWidth: .infinity)
          .foregroundStyle(.secondary)
        Spacer()
      }
      .padding(22)
      .background(AtomColors.canvas.ignoresSafeArea())
      .onChange(of: speech.transcript) { _, value in
        let spokenSchedule = AtomCommandParser().parse(value)
        if let scheduledAt = spokenSchedule.scheduledAtUTC {
          selectedDate = scheduledAt
        }
      }
      .toolbar {
        ToolbarItem(placement: .cancellationAction) {
          Button("Cancel", action: onCancel)
        }
      }
    }
  }
}
