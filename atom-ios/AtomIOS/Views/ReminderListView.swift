import AtomCore
import SwiftData
import SwiftUI

struct ReminderListView: View {
  @Environment(\.modelContext) private var modelContext
  @Query(sort: \ReminderRecord.createdAt, order: .reverse) private var reminders: [ReminderRecord]
  @State private var editingReminder: ReminderRecord?

  var body: some View {
    Group {
      if reminders.isEmpty {
        ContentUnavailableView(
          "Nothing here",
          systemImage: "checkmark.circle",
          description: Text("Your mind is clear.")
        )
      } else {
        List {
          scheduledSection
          unscheduledSection
          missedSection
          completedSection
        }
        .listStyle(.insetGrouped)
        .scrollContentBackground(.hidden)
        .background(AtomColors.canvas)
      }
    }
    .navigationTitle("Reminders")
    .background(AtomColors.canvas)
    .sheet(item: $editingReminder) { reminder in
      ReminderEditView(reminder: reminder)
    }
  }

  @ViewBuilder
  private var scheduledSection: some View {
    let values = reminders.filter { $0.state == "scheduled" }
    if !values.isEmpty {
      Section("UPCOMING") {
        ForEach(values) { reminder in
          ReminderRow(reminder: reminder)
            .contentShape(Rectangle())
            .onTapGesture { editingReminder = reminder }
            .swipeActions(edge: .leading, allowsFullSwipe: true) {
              Button("Done") { complete(reminder) }
                .tint(.green)
            }
            .swipeActions(edge: .trailing, allowsFullSwipe: true) {
              Button("Delete", role: .destructive) { delete(reminder) }
            }
        }
      }
    }
  }

  @ViewBuilder
  private var unscheduledSection: some View {
    let values = reminders.filter { $0.state == "unscheduled" }
    if !values.isEmpty {
      Section("UNSCHEDULED") {
        ForEach(values) { reminder in
          ReminderRow(reminder: reminder)
            .contentShape(Rectangle())
            .onTapGesture { editingReminder = reminder }
            .swipeActions(edge: .leading, allowsFullSwipe: true) {
              Button("Done") { complete(reminder) }.tint(.green)
            }
            .swipeActions(edge: .trailing, allowsFullSwipe: true) {
              Button("Delete", role: .destructive) { delete(reminder) }
            }
        }
      }
    }
  }

  @ViewBuilder
  private var missedSection: some View {
    let values = reminders.filter { $0.state == "missed" }
    if !values.isEmpty {
      Section("MISSED") {
        ForEach(values) { reminder in
          ReminderRow(reminder: reminder)
            .contentShape(Rectangle())
            .onTapGesture { editingReminder = reminder }
            .swipeActions(edge: .leading, allowsFullSwipe: true) {
              Button("Done") { complete(reminder) }.tint(.green)
            }
            .swipeActions(edge: .trailing, allowsFullSwipe: true) {
              Button("Delete", role: .destructive) { delete(reminder) }
            }
        }
      }
    }
  }

  @ViewBuilder
  private var completedSection: some View {
    let values = reminders.filter { $0.state == "completed" }
    if !values.isEmpty {
      Section("COMPLETED") {
        ForEach(values) { reminder in
          ReminderRow(reminder: reminder)
            .opacity(0.58)
            .swipeActions(edge: .trailing, allowsFullSwipe: true) {
              Button("Delete", role: .destructive) { delete(reminder) }
            }
        }
      }
    }
  }

  private func complete(_ reminder: ReminderRecord) {
    reminder.state = "completed"
    reminder.updatedAt = Date()
    NotificationScheduler.shared.cancel(id: reminder.id)
    try? modelContext.save()
  }

  private func delete(_ reminder: ReminderRecord) {
    NotificationScheduler.shared.cancel(id: reminder.id)
    modelContext.delete(reminder)
    try? modelContext.save()
  }
}

private struct ReminderEditView: View {
  @Environment(\.dismiss) private var dismiss
  @Environment(\.modelContext) private var modelContext
  @EnvironmentObject private var permissions: PermissionCoordinator
  let reminder: ReminderRecord
  @StateObject private var speech = SpeechCaptureService()
  @State private var title: String
  @State private var scheduled = false
  @State private var selectedDate = Date().addingTimeInterval(3_600)

  init(reminder: ReminderRecord) {
    self.reminder = reminder
    _title = State(initialValue: reminder.title)
    _scheduled = State(initialValue: reminder.scheduledAtUTC != nil)
    _selectedDate = State(initialValue: reminder.scheduledAtUTC ?? Date().addingTimeInterval(3_600))
  }

  var body: some View {
    NavigationStack {
      Form {
        Section("REMINDER") {
          TextField("Reminder", text: $title, axis: .vertical)
          Button {
            Task {
              if !permissions.microphoneGranted || !permissions.speechGranted {
                await permissions.requestInitialPermissions()
              }
              speech.toggle()
            }
          } label: {
            Label(
              speech.isListening ? "Stop listening" : "Change using voice",
              systemImage: speech.isListening ? "stop.fill" : "mic.fill"
            )
          }
        }
        Section("SCHEDULE") {
          Toggle("Scheduled", isOn: $scheduled)
          if scheduled {
            DatePicker(
              "Date and time",
              selection: $selectedDate,
              in: Date()...,
              displayedComponents: [.date, .hourAndMinute]
            )
          }
        }
      }
      .navigationTitle("Edit reminder")
      .navigationBarTitleDisplayMode(.inline)
      .onChange(of: speech.transcript) { _, value in
        let change = AtomCommandParser().parse(value)
        if change.title != "Reminder" { title = change.title }
        if let date = change.scheduledAtUTC {
          selectedDate = date
          scheduled = true
        }
      }
      .toolbar {
        ToolbarItem(placement: .cancellationAction) {
          Button("Cancel") { dismiss() }
        }
        ToolbarItem(placement: .confirmationAction) {
          Button("Save", action: save)
        }
      }
    }
  }

  private func save() {
    NotificationScheduler.shared.cancel(id: reminder.id)
    reminder.title = title.trimmingCharacters(in: .whitespacesAndNewlines)
    reminder.updatedAt = Date()
    reminder.scheduledAtUTC = scheduled ? selectedDate : nil
    reminder.state = scheduled ? "scheduled" : "unscheduled"
    if scheduled {
      let dateFormatter = DateFormatter()
      dateFormatter.dateFormat = "yyyy-MM-dd"
      let timeFormatter = DateFormatter()
      timeFormatter.dateFormat = "h:mm a"
      reminder.localDate = dateFormatter.string(from: selectedDate)
      reminder.localTime = timeFormatter.string(from: selectedDate)
      Task {
        try? await NotificationScheduler.shared.schedule(
          id: reminder.id,
          title: reminder.title,
          at: selectedDate
        )
      }
    } else {
      reminder.localDate = nil
      reminder.localTime = nil
    }
    try? modelContext.save()
    speech.stop()
    dismiss()
  }
}

private struct ReminderRow: View {
  let reminder: ReminderRecord

  var body: some View {
    HStack(spacing: 14) {
      Circle()
        .fill(reminder.state == "scheduled" ? AtomColors.mint : AtomColors.coral.opacity(0.75))
        .frame(width: 10, height: 10)
      VStack(alignment: .leading, spacing: 5) {
        Text(reminder.title)
          .font(.headline)
          .strikethrough(reminder.state == "completed")
        if let date = reminder.scheduledAtUTC {
          Text(date.formatted(date: .abbreviated, time: .shortened))
            .font(.subheadline)
            .foregroundStyle(.secondary)
        } else {
          Text("Needs a date and time")
            .font(.subheadline)
            .foregroundStyle(AtomColors.coral)
        }
        if reminder.recurrenceRule != nil {
          Label("Repeats", systemImage: "repeat")
            .font(.caption)
            .foregroundStyle(.secondary)
        }
      }
      Spacer()
    }
    .padding(.vertical, 5)
  }
}
