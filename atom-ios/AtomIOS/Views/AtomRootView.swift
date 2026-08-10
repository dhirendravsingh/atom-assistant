import AtomCore
import SwiftData
import SwiftUI

struct AtomRootView: View {
  @Environment(\.modelContext) private var modelContext
  @Environment(\.scenePhase) private var scenePhase
  @EnvironmentObject private var permissions: PermissionCoordinator
  @AppStorage("atom.permissionSetupVersion") private var permissionSetupVersion = 0
  @AppStorage("atom.appearance") private var appearance = "system"
  @State private var selection = 0
  @State private var showPermissionSetup = false
  @State private var showNotificationHistory = false

  var body: some View {
    TabView(selection: $selection) {
      NavigationStack {
        CaptureView(
          onOpenReminders: { selection = 1 },
          onOpenNotificationHistory: { showNotificationHistory = true }
        )
      }
      .tabItem { Label("Capture", systemImage: "sparkles") }
      .tag(0)

      NavigationStack {
        ReminderListView()
      }
      .tabItem { Label("Reminders", systemImage: "checklist") }
      .tag(1)

      NavigationStack {
        SettingsView(appearance: $appearance)
      }
      .tabItem { Label("Settings", systemImage: "slider.horizontal.3") }
      .tag(2)
    }
    .tint(AtomColors.ink)
    .preferredColorScheme(colorScheme)
    .task {
      await permissions.refresh()
      await reconcileNotificationState()
      showPermissionSetup = permissionSetupVersion < 1 && !permissions.plan.isEmpty
    }
    .onChange(of: scenePhase) { _, phase in
      guard phase == .active else { return }
      Task {
        await permissions.refresh()
        await reconcileNotificationState()
      }
    }
    .onReceive(NotificationCenter.default.publisher(for: .atomNotificationHistoryDidChange)) { _ in
      applyNotificationHistory()
    }
    .sheet(isPresented: $showPermissionSetup) {
      PermissionSetupView {
        permissionSetupVersion = 1
        showPermissionSetup = false
      }
      .environmentObject(permissions)
      .interactiveDismissDisabled(permissions.requesting)
    }
    .sheet(isPresented: $showNotificationHistory) {
      NotificationHistoryView()
    }
  }

  private var colorScheme: ColorScheme? {
    switch appearance {
    case "light": .light
    case "dark": .dark
    default: nil
    }
  }

  private func applyNotificationActions() {
    let actions = NotificationScheduler.shared.drainRecordedActions()
    guard !actions.isEmpty,
      let reminders = try? modelContext.fetch(FetchDescriptor<ReminderRecord>())
    else { return }
    for action in actions {
      guard let reminder = reminders.first(where: { $0.id == action.reminderID }) else { continue }
      reminder.updatedAt = Date()
      switch action.kind {
      case .completed:
        reminder.state = "completed"
      case .ignored:
        reminder.state = "missed"
      case .rescheduled:
        reminder.state = "scheduled"
        reminder.scheduledAtUTC = action.scheduledAt
        if let date = action.scheduledAt {
          let dateFormatter = DateFormatter()
          dateFormatter.dateFormat = "yyyy-MM-dd"
          let timeFormatter = DateFormatter()
          timeFormatter.dateFormat = "h:mm a"
          reminder.localDate = dateFormatter.string(from: date)
          reminder.localTime = timeFormatter.string(from: date)
        }
      }
    }
    try? modelContext.save()
  }

  private func reconcileNotificationState() async {
    await NotificationScheduler.shared.synchronizeDeliveredHistory()
    applyNotificationActions()
    applyNotificationHistory()
  }

  private func applyNotificationHistory() {
    let pendingEvents = NotificationScheduler.shared.drainHistoryEvents()
    guard !pendingEvents.isEmpty else { return }
    let existing = (try? modelContext.fetch(FetchDescriptor<NotificationHistoryRecord>())) ?? []
    var existingIDs = Set(existing.map(\.id))
    for event in pendingEvents where !existingIDs.contains(event.id) {
      modelContext.insert(
        NotificationHistoryRecord(
          id: event.id,
          reminderID: event.reminderID,
          title: event.title,
          eventType: event.kind,
          detail: event.detail,
          resultingScheduledAt: event.resultingScheduledAt,
          occurredAt: event.occurredAt
        )
      )
      existingIDs.insert(event.id)
    }
    try? modelContext.save()
  }
}

private struct PermissionSetupView: View {
  @EnvironmentObject private var permissions: PermissionCoordinator
  let onFinished: () -> Void

  var body: some View {
    NavigationStack {
      VStack(alignment: .leading, spacing: 22) {
        AtomMark()
        Text("Set up atom-ios")
          .font(.system(.largeTitle, design: .rounded, weight: .bold))
        Text(
          "Allow these once so Atom can listen after a tap and deliver reminders from your iPhone."
        )
        .foregroundStyle(.secondary)

        permissionRow(
          icon: "bell.badge.fill",
          title: "Notifications",
          detail: "Alerts, sound, Lock Screen delivery, and actions"
        )
        permissionRow(
          icon: "mic.fill",
          title: "Microphone",
          detail: "Used only after you tap the microphone"
        )
        permissionRow(
          icon: "waveform",
          title: "Speech recognition",
          detail: "Turns your reminder into editable text"
        )

        Text(
          "No photo, media, file, or storage permission is required. SwiftData stays inside Atom’s private app container."
        )
        .font(.footnote)
        .foregroundStyle(.secondary)

        Spacer()

        Button {
          Task {
            await permissions.requestInitialPermissions()
            onFinished()
          }
        } label: {
          HStack {
            if permissions.requesting { ProgressView().tint(.white) }
            Text(permissions.requesting ? "Requesting…" : "Continue")
              .frame(maxWidth: .infinity)
          }
          .padding(.vertical, 6)
        }
        .buttonStyle(.borderedProminent)
        .tint(AtomColors.ink)
        .disabled(permissions.requesting)

        Button("Not now", action: onFinished)
          .frame(maxWidth: .infinity)
          .foregroundStyle(.secondary)
      }
      .padding(24)
      .background(AtomColors.canvas.ignoresSafeArea())
    }
    .presentationDetents([.large])
  }

  private func permissionRow(icon: String, title: String, detail: String) -> some View {
    HStack(spacing: 15) {
      Image(systemName: icon)
        .font(.title3)
        .foregroundStyle(AtomColors.ink)
        .frame(width: 42, height: 42)
        .background(AtomColors.mintPale, in: Circle())
      VStack(alignment: .leading, spacing: 3) {
        Text(title).font(.headline)
        Text(detail).font(.subheadline).foregroundStyle(.secondary)
      }
    }
  }
}
