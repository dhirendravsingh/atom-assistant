import SwiftUI

enum AtomColors {
  static let ink = Color(red: 0.063, green: 0.090, blue: 0.075)
  static let canvas = Color(red: 0.966, green: 0.956, blue: 0.922)
  static let paper = Color(red: 0.995, green: 0.989, blue: 0.965)
  static let mint = Color(red: 0.455, green: 0.851, blue: 0.682)
  static let mintPale = Color(red: 0.835, green: 0.941, blue: 0.856)
  static let coral = Color(red: 0.937, green: 0.490, blue: 0.408)
}

struct AtomMark: View {
  var compact = false
  var includeWordmark = true

  var body: some View {
    HStack(spacing: compact ? 7 : 9) {
      ZStack {
        RoundedRectangle(cornerRadius: compact ? 8 : 11)
          .fill(AtomColors.ink)
        ForEach([0.0, 60.0, -60.0], id: \.self) { rotation in
          Ellipse()
            .stroke(AtomColors.mint, lineWidth: compact ? 1.3 : 1.7)
            .frame(width: compact ? 9 : 12, height: compact ? 23 : 31)
            .rotationEffect(.degrees(rotation))
        }
        Circle()
          .fill(AtomColors.coral)
          .frame(width: compact ? 5 : 7, height: compact ? 5 : 7)
      }
      .frame(width: compact ? 30 : 40, height: compact ? 30 : 40)
      .rotationEffect(.degrees(-5))

      if includeWordmark {
        Text("atom")
          .font(.system(size: compact ? 22 : 29, weight: .bold, design: .rounded))
          .tracking(-1)
      }
    }
    .accessibilityElement(children: .ignore)
    .accessibilityLabel("Atom")
  }
}

struct AtomCard<Content: View>: View {
  let content: Content

  init(@ViewBuilder content: () -> Content) {
    self.content = content()
  }

  var body: some View {
    content
      .padding(18)
      .frame(maxWidth: .infinity, alignment: .leading)
      .background(.background.opacity(0.92), in: RoundedRectangle(cornerRadius: 26))
      .overlay {
        RoundedRectangle(cornerRadius: 26)
          .stroke(.primary.opacity(0.06), lineWidth: 1)
      }
  }
}
