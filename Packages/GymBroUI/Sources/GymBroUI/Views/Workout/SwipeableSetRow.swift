import SwiftUI
import GymBroCore

/// Wraps ExerciseSetRow with swipe gestures:
/// - Swipe LEFT to complete a set
/// - Swipe RIGHT to undo completion
/// - Long-press for options menu (drop set, warmup, failure, delete)
///
/// Keeps the existing button tap as fallback — gestures are enhancement only.
public struct SwipeableSetRow: View {
    let set: ExerciseSet
    let setNumber: Int
    let unitSystem: UnitSystem
    let onComplete: () -> Void
    let onUndo: () -> Void
    let onSetType: (SetType) -> Void
    let onDelete: () -> Void

    @GestureState private var dragOffset: CGFloat = 0
    @State private var showOptions = false
    @State private var swipeState: SwipeState = .idle
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    private let swipeThreshold: CGFloat = 80

    public init(
        set: ExerciseSet,
        setNumber: Int,
        unitSystem: UnitSystem = .metric,
        onComplete: @escaping () -> Void,
        onUndo: @escaping () -> Void,
        onSetType: @escaping (SetType) -> Void,
        onDelete: @escaping () -> Void
    ) {
        self.set = set
        self.setNumber = setNumber
        self.unitSystem = unitSystem
        self.onComplete = onComplete
        self.onUndo = onUndo
        self.onSetType = onSetType
        self.onDelete = onDelete
    }

    public var body: some View {
        ZStack {
            // Background reveal on swipe
            HStack {
                // Right side (undo) — visible on swipe right
                if dragOffset > 0 || swipeState == .undoing {
                    HStack {
                        Image(systemName: "arrow.uturn.backward.circle.fill")
                            .font(.title2)
                            .foregroundStyle(.white)
                        Text("Undo")
                            .font(.subheadline.bold())
                            .foregroundStyle(.white)
                        Spacer()
                    }
                    .padding(.leading, 16)
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                    .background(Color.orange)
                    .clipShape(RoundedRectangle(cornerRadius: 12))
                }

                Spacer()

                // Left side (complete) — visible on swipe left
                if dragOffset < 0 || swipeState == .completing {
                    HStack {
                        Spacer()
                        Text("Complete")
                            .font(.subheadline.bold())
                            .foregroundStyle(.white)
                        Image(systemName: "checkmark.circle.fill")
                            .font(.title2)
                            .foregroundStyle(.white)
                    }
                    .padding(.trailing, 16)
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                    .background(Color.green)
                    .clipShape(RoundedRectangle(cornerRadius: 12))
                }
            }

            // Foreground row
            ExerciseSetRow(
                set: set,
                setNumber: setNumber,
                unitSystem: unitSystem,
                onTap: {
                    if set.completedAt == nil {
                        onComplete()
                    }
                }
            )
            .offset(x: dragOffset)
            .gesture(swipeGesture)
            .simultaneousGesture(longPressGesture)
        }
        .confirmationDialog("Set Options", isPresented: $showOptions) {
            Button("Drop Set") { onSetType(.drop) }
            Button("Warmup Set") { onSetType(.warmup) }
            Button("AMRAP Set") { onSetType(.amrap) }
            Button("Back-off Set") { onSetType(.backoff) }
            Button("Delete Set", role: .destructive) { onDelete() }
        }
        .accessibilityAction(named: "Complete Set") { onComplete() }
        .accessibilityAction(named: "Undo Completion") { onUndo() }
        .accessibilityAction(named: "Set Options") { showOptions = true }
    }

    private var swipeGesture: some Gesture {
        DragGesture(minimumDistance: 20)
            .updating($dragOffset) { value, state, _ in
                let translation = value.translation.width
                // Only allow swipe left if not completed, swipe right if completed
                if translation < 0 && set.completedAt == nil {
                    state = max(translation, -150) // cap swipe distance
                } else if translation > 0 && set.completedAt != nil {
                    state = min(translation, 150)
                }
            }
            .onEnded { value in
                let translation = value.translation.width

                if translation < -swipeThreshold && set.completedAt == nil {
                    swipeState = .completing
                    HapticFeedbackService.shared.setCompleted()
                    onComplete()
                    resetSwipeState()
                } else if translation > swipeThreshold && set.completedAt != nil {
                    swipeState = .undoing
                    HapticFeedbackService.shared.lightImpact()
                    onUndo()
                    resetSwipeState()
                }
            }
    }

    private var longPressGesture: some Gesture {
        LongPressGesture(minimumDuration: 0.5)
            .onEnded { _ in
                HapticFeedbackService.shared.mediumImpact()
                showOptions = true
            }
    }

    private func resetSwipeState() {
        if reduceMotion {
            swipeState = .idle
        } else {
            withAnimation(.spring(response: 0.3, dampingFraction: 0.7)) {
                swipeState = .idle
            }
        }
    }
}

private enum SwipeState {
    case idle, completing, undoing
}
