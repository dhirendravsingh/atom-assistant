import SwiftUI

struct SettingsView: View {
  @EnvironmentObject private var permissions: PermissionCoordinator
  @AppStorage("atom.ownerName") private var ownerName = "Dhiren"
  @AppStorage("atom.gender") private var gender = "Prefer not to say"
  @AppStorage("atom.pronouns") private var pronouns = "He/Him"
  @Binding var appearance: String

  var body: some View {
    Form {
      Section("PROFILE") {
        TextField("Name", text: $ownerName)
        Picker("Gender", selection: $gender) {
          Text("Man").tag("Man")
          Text("Woman").tag("Woman")
          Text("Non-binary").tag("Non-binary")
          Text("Prefer not to say").tag("Prefer not to say")
        }
        Picker("Pronouns", selection: $pronouns) {
          Text("He/Him").tag("He/Him")
          Text("She/Her").tag("She/Her")
          Text("They/Them").tag("They/Them")
          Text("Use my name").tag("Use my name")
        }
      }

      Section("APPEARANCE") {
        Picker("Theme", selection: $appearance) {
          Text("System").tag("system")
          Text("Light").tag("light")
          Text("Dark").tag("dark")
        }
        .pickerStyle(.segmented)
      }

      Section("REMINDER ACCESS") {
        permissionRow(
          title: "Notifications",
          ready: permissions.notificationGranted
        )
        permissionRow(
          title: "Microphone",
          ready: permissions.microphoneGranted
        )
        permissionRow(
          title: "Speech recognition",
          ready: permissions.speechGranted
        )
        Button("Open iOS Settings") { permissions.openSettings() }
      } footer: {
        Text(
          "No storage permission is needed. Reminders remain in atom-ios private storage. iOS 17–25 use local notifications; AlarmKit support requires the iOS 26 SDK."
        )
      }

      Section("DEVICE") {
        LabeledContent("Locale", value: Locale.current.identifier)
        LabeledContent("Timezone", value: TimeZone.current.identifier)
        LabeledContent("Database", value: "Private SwiftData")
      }
    }
    .navigationTitle("Settings")
    .scrollContentBackground(.hidden)
    .background(AtomColors.canvas)
    .task { await permissions.refresh() }
  }

  private func permissionRow(title: String, ready: Bool) -> some View {
    HStack {
      Text(title)
      Spacer()
      Text(ready ? "Ready" : "Needs access")
        .font(.caption.weight(.bold))
        .foregroundStyle(ready ? .green : AtomColors.coral)
    }
  }
}
